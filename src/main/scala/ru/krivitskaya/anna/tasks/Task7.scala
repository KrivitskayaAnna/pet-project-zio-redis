package ru.krivitskaya.anna.tasks

import ru.krivitskaya.anna.Environ.persistenceLayers
import zio._
import zio.redis._

case class DistributedLock(redis: Redis) {
  private implicit val stringInput: Input[String] = Input.StringInput
  private implicit val longOutput: Output[Long]   = Output.LongOutput

  def acquireLock(key: String, ttl: Duration): IO[RedisError, Option[String]] =
    Random.nextUUID
      .flatMap { token =>
        redis
          .set(key, token, Some(ttl), Some(Update.SetNew))
          .zip(ZIO.succeed(token))
      }
      .map { case (isSet, token) => if (isSet) Some(token.toString) else None }

  private val luaScript: String =
    """
      |if redis.call('GET', KEYS[1]) == ARGV[1] then
      |  return redis.call('DEL', KEYS[1])
      |else
      |  return 0
      |end
      |""".stripMargin

  def releaseLock(key: String, token: String): IO[RedisError, Long] =
    redis.eval(luaScript, Chunk(key), Chunk(token)).returning[Long]

  private def acquireLockLoop(key: String, ttl: Duration): UIO[String] =
    ZIO.logInfo("Trying to acquire lock") *>
      acquireLock(key, ttl).orDie.flatMap {
        case Some(token) => ZIO.succeed(token)
        case None        => acquireLockLoop(key, ttl).delay(2.seconds)
      }

  def withLock[R, E, A](key: String, ttl: Duration)(effect: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.acquireReleaseWith(acquire = acquireLockLoop(key, ttl))(release =
      token => releaseLock(key, token).orDie
    )(use = _ => effect)
}

object DistributedLock {
  val live: URLayer[Redis, DistributedLock] = ZLayer.fromFunction(DistributedLock.apply _)
}

object Task7 extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      service <- ZIO.service[DistributedLock]
      _ <- ZIO.foreachParDiscard((1 to 10).toList) { idx =>
        service
          .withLock(s"aboba", 10.seconds)(
            ZIO.logInfo(s"Aboba with lock $idx is finished") *> ZIO.sleep(5.seconds)
          )
          .debug
      }
    } yield ()).provide(
      persistenceLayers,
      DistributedLock.live
    )
}
