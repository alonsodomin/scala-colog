package colog.standalone

import cats.effect.{IO, LiftIO}
import colog.Logger

object SysLoggers {

  def stdout[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    Logger(str => F.liftIO(IO(println(str))))

  def stderr[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    IOLoggers.printStream[F](System.err)

}
