package ru.krivitskaya.anna.tasks

import ru.krivitskaya.anna.Environ.persistenceLayers
import zio._
import zio.redis._

case class TaskQueue(redis: Redis) {
  val queueKey = "queue"

  def enqueue(taskId: String): IO[RedisError, Long] =
    redis.rPush(queueKey, taskId)

  def dequeue: IO[RedisError, Option[String]] =
    redis.lPop(queueKey).returning[String]

  def dequeueBlocking(timeout: Duration): IO[RedisError, Option[(String, String)]] =
    redis.blPop(queueKey)(timeout).returning[String]
}

object Task5 extends ZIOAppDefault {
  def produce: ZIO[TaskQueue, RedisError, Unit] =
    ZIO.serviceWithZIO[TaskQueue] { taskQueue =>
      ZIO.foreachDiscard((1 to 100).toList) { idx =>
        taskQueue.enqueue(idx.toString).debug *> ZIO.sleep(100.millis)
      }
    }

  //hangs with low timeouts due to Redis rounding it to seconds when shared single connection to Redis
  def consume: ZIO[TaskQueue, RedisError, Nothing] =
    ZIO.serviceWithZIO[TaskQueue] { taskQueue =>
      taskQueue.dequeueBlocking(100.millis).debug *> consume
    }

  val redisLayers = ZLayer.make[TaskQueue](
    persistenceLayers,
    ZLayer.fromFunction(TaskQueue.apply _)
  )

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    produce.provide(redisLayers) <&> consume.provide(redisLayers)
}
