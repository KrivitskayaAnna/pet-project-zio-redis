package ru.krivitskaya.anna.tasks

import ru.krivitskaya.anna.Environ.persistenceLayers
import zio._
import zio.redis._

case class LeaderBoard(redis: Redis) {
  val leaderboardKey = "leaderboard"

  def addScore(playerId: String, score: Int): IO[RedisError, Double] =
    redis.zIncrBy(leaderboardKey, score, playerId)

  def getTop(n: Int) =
    redis
      .zRangeWithScores(
        leaderboardKey,
        SortedSetRange.Range(RangeMinimum(0), RangeMaximum.Exclusive(n)),
        rev = true
      )
      .returning[String]

  def getRank(playerId: String) =
    redis.zRank(leaderboardKey, playerId)
}

object Task3 extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      leaderboard <- ZIO.service[LeaderBoard]
      _           <- leaderboard.addScore("alice", 100).debug
      _           <- leaderboard.addScore("bob", 200).debug
      _           <- leaderboard.addScore("charlie", 150).debug
      _           <- leaderboard.getTop(3).debug
      _           <- leaderboard.getRank("charlie").debug
      _           <- leaderboard.getRank("alice").debug
      _           <- leaderboard.getRank("bob").debug
    } yield ()).provide(
      persistenceLayers,
      ZLayer.fromFunction(LeaderBoard.apply _)
    )
}
