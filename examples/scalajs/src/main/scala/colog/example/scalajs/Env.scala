package colog.example.scalajs

import cats.effect.IO
import colog.{HasLogger, LogRecord, Logger}

case class Env(logger: Logger[IO, LogRecord])
object Env {

  implicit val envHasLogger: HasLogger[IO, Env, LogRecord] = new HasLogger[IO, Env, LogRecord] {
    override def getLogger(env: Env): Logger[IO, LogRecord] = env.logger

    override def setLogger(newLogger: Logger[IO, LogRecord], env: Env): Env =
      env.copy(logger = newLogger)
  }

}
