package ru.krivitskaya.anna.tasks

import ru.krivitskaya.anna.Environ.persistenceLayers
import zio._
import zio.redis._
import zio.schema._
import zio.schema.codec._

case class User(name: String, age: Int)

object User {
  implicit val schema: Schema[User] = DeriveSchema.gen[User]
}

object ProtobufCodecSupplier extends CodecSupplier {
  def get[A: Schema]: BinaryCodec[A] = ProtobufCodec.protobufCodec
}

object Task1 extends ZIOAppDefault {
  val myApp: ZIO[Redis, RedisError, Unit] =
    for {
      redis <- ZIO.service[Redis]
      _     <- redis.set("hello", "world", Some(10.minute))
      v     <- redis.get("hello").returning[String]
      _     <- Console.printLine(s"Value of hello: $v").orDie
      _     <- ZIO.sleep(6.seconds)
      v3    <- redis.get("hello").returning[String]
      _     <- Console.printLine(s"Value of hello: $v3").orDie
      _     <- redis.set("user:1", User("Anna", 28))
      u     <- redis.get("user:1").returning[User]
      _     <- Console.printLine(s"user: $u").orDie
      v2    <- redis.get("nonexistent").returning[String]
      _     <- Console.printLine(s"Value of nonexistent: $v2").orDie
    } yield ()

  override def run =
    myApp.provide(persistenceLayers)
}
