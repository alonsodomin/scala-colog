package colog.standalone

import cats.effect.Sync
import colog.Logger

object SysLoggers {

  def stdout[F[_]](implicit F: Sync[F]): Logger[F, String] =
    Logger(str => F.delay(println(str)))

  def stderr[F[_]](implicit F: Sync[F]): Logger[F, String] =
    IOLoggers.printStream[F](System.err)

}
