package colog.standalone

import java.io.PrintStream

import cats.effect.{IO, LiftIO}
import colog.Logger

object IOLoggers {

  def printStream[F[_]](printStream: PrintStream)(implicit F: LiftIO[F]): Logger[F, String] =
    Logger(msg => F.liftIO(IO(printStream.println(msg))))

}
