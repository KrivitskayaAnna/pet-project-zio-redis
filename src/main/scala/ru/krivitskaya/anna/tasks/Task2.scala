package ru.krivitskaya.anna.tasks

import ru.krivitskaya.anna.Environ.persistenceLayers
import zio._
import zio.redis._

import java.time.format.DateTimeFormatter

sealed trait RateLimitError
case object TooManyRequests extends RateLimitError

object Task2 extends ZIOAppDefault {
  val datetime: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

  val getCurrentTime: UIO[String] = Clock.currentDateTime.map(_.toLocalDateTime.format(datetime))

  def formKey(userId: String, currentTime: String) = s"requests:$userId:$currentTime"

  def recordRequest(userId: String): ZIO[Redis, RedisError, Long] =
    for {
      currentTime <- getCurrentTime
      key = formKey(userId, currentTime)
      redis <- ZIO.service[Redis]
      cur   <- redis.incr(key)
    } yield cur

  def getRequestCount(userId: String): ZIO[Redis, Throwable, Long] =
    for {
      currentTime <- getCurrentTime
      key = formKey(userId, currentTime)
      redis <- ZIO.service[Redis]
      cur   <- redis.get(key).returning[String]
      value <- ZIO.attempt(cur.getOrElse("0").toLong)
    } yield value

  def resetKeyValue(userId: String) =
    for {
      currentTime <- getCurrentTime
      key = formKey(userId, currentTime)
      redis <- ZIO.service[Redis]
      _     <- redis.getDel(key).returning[String].debug
      _     <- redis.expire(key, 60.seconds)
    } yield ()

  def rateLimitKey(
      userId: String,
      maxRequestsPerUser: Int
  ): RIO[Redis, Unit] =
    (for {
      currentCount <- getRequestCount(userId).debug
      value <- if (currentCount >= maxRequestsPerUser) ZIO.left(TooManyRequests)
      else recordRequest(userId).map(Right(_))
    } yield value)
      .flatMap {
        case Left(_)  => ZIO.logError(s"Too many requests")
        case Right(_) => ZIO.logInfo("Success")
      }

  val testUserId: String = "1"

  def myApp(userId: String): ZIO[Redis, Serializable, Unit] =
    for {
      _      <- resetKeyValue(userId)
      _      <- ZIO.foreachDiscard(1 to 24)(_ => rateLimitKey(userId, 20))
      result <- getRequestCount(userId)
      _      <- ZIO.logInfo(s"Result is $result")
    } yield ()

  override def run =
    myApp(testUserId).provide(persistenceLayers)
}
