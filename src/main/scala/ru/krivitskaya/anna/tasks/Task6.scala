package ru.krivitskaya.anna.tasks

import ru.krivitskaya.anna.Environ.persistenceLayers
import zio._
import zio.redis.{Redis, RedisError}

case class Service(private val redis: Redis, private val db: Ref[Map[Long, User]]) {
  private def redisKey(id: Long) = s"user:$id"

  def getUser(id: Long): IO[RedisError, Option[User]] = {
    val key = redisKey(id)
    for {
      optUser <- redis.get(key).returning[User]
      result <- optUser match {
        case Some(value) => ZIO.logInfo("From cache") *> ZIO.some(value)
        case None =>
          ZIO.logInfo("From db") *>
            db.get
              .map(_.get(id))
              .tap(optUser => redis.set(key, optUser.get, Some(5.seconds)))
      }
    } yield result
  }

  def updateUser(id: Long, user: User): IO[RedisError, Unit] =
    db.update(amap => amap + (id -> user)) *>
      redis.del(redisKey(id)).unit
}

object Service {
  val live = ZLayer.fromFunction(Service.apply _)

  val liveMockDb: ULayer[Ref[Map[Long, User]]] =
    ZLayer.fromZIO(Ref.make(Map(1L -> User("Ann", 12))))
}

object Task6 extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      service <- ZIO.service[Service]
      _       <- service.getUser(1).debug
      _       <- service.getUser(1).debug
      _       <- service.updateUser(1, User("Maria", 12)).debug
      _       <- service.getUser(1).debug
      _       <- service.getUser(1).debug
    } yield ()).provide(
      persistenceLayers,
      Service.live,
      Service.liveMockDb
    )
}
