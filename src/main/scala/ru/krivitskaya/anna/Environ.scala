package ru.krivitskaya.anna

import ru.krivitskaya.anna.tasks.ProtobufCodecSupplier
import zio.redis.{CodecSupplier, Redis}
import zio.{TaskLayer, ZLayer}

object Environ {
  def persistenceLayers: TaskLayer[Redis] =
    ZLayer.make[Redis](
      Redis.local,
      ZLayer.succeed[CodecSupplier](ProtobufCodecSupplier)
    )
}
