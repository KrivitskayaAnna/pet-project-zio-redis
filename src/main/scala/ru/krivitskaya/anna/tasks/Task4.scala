package ru.krivitskaya.anna.tasks

import ru.krivitskaya.anna.Environ.persistenceLayers
import zio._
import zio.redis._
import zio.schema._

import java.util.concurrent.TimeUnit

case class SessionService(redis: Redis) {
  private def sessionKey(token: String): String = s"session:$token"

  def createSession(userId: String) =
    for {
      token <- Random.nextUUID
      time  <- Clock.currentTime(TimeUnit.SECONDS)
      tokenKey     = sessionKey(token.toString)
      tokenSession = Session(userId, time)
      _ <- redis.set(tokenKey, tokenSession, Some(30.minutes))
    } yield token.toString

  def getSession(token: String): ZIO[Redis, RedisError, Option[Session]] =
    redis.get(sessionKey(token)).returning[Session]

  def deleteSession(token: String): ZIO[Redis, RedisError, Boolean] =
    redis.getDel(sessionKey(token)).returning[Session].map(_.nonEmpty)
}

case class Session(userId: String, createdAt: Long)

object Session {
  implicit val schema: Schema[Session] = DeriveSchema.gen[Session]
}

object Task4 extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      sessionService <- ZIO.service[SessionService]
      token          <- sessionService.createSession("aboba").debug
      _              <- sessionService.getSession(token).debug
      _              <- sessionService.deleteSession(token).debug
      _              <- sessionService.getSession(token).debug
    } yield ()).provide(
      persistenceLayers,
      ZLayer.fromFunction(SessionService.apply _)
    )
}
