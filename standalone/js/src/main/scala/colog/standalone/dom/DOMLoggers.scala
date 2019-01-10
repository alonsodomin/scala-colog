package colog.standalone.dom

import cats._
import cats.effect._
import cats.implicits._

import colog._

import org.scalajs.dom.console

object DOMLoggers {

  def simple[F[_], A: Show](implicit F: Sync[F]): Logger[F, A] =
    Logger(a => F.delay(console.log(a.show)))

  def structured[F[_]](implicit F: Sync[F]): Logger[F, LogRecord] = {
    def unsafeLog(rec: LogRecord)(f: String => Unit): F[Unit] =
      F.delay(f(rec.message))

    Logger { rec =>
      val logFn: String => Unit = rec.severity match {
        case Severity.Error => str => console.error(str)
        case Severity.Warning => str => console.warn(str)
        case Severity.Info    => str => console.info(str)
        case Severity.Debug   => str => console.log(str)
      }

      unsafeLog(rec)(logFn)
    }
  }

}
