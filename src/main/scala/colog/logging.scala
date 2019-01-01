package colog

import cats.Monad
import cats.mtl.ApplicativeAsk
import cats.implicits._

trait Logging[F[_], E, A] {
  implicit def F: Monad[F]

  protected def Ask: ApplicativeAsk[F, E]
  protected def HasLogger: HasLogger[F, E, A]

  final def logMsg(msg: A): F[Unit] = for {
    logAction <- Ask.reader(HasLogger.getLogger)
    _         <- logAction.log(msg)
  } yield ()

}

trait StructuredLogging[F[_]] extends Logging[LogT[F, LogRecord, ?], Logger[F, LogRecord], LogRecord] {

  final def error(msg: String): LogT[F, LogRecord, Unit] =
    log(Severity.Error, msg)

  final def error(msg: String, error: Throwable): LogT[F, LogRecord, Unit] =
    log(Severity.Error, msg, error)

  final def warn(msg: String): LogT[F, LogRecord, Unit] =
    log(Severity.Warning, msg)

  final def warn(msg: String, error: Throwable): LogT[F, LogRecord, Unit] =
    log(Severity.Warning, msg, error)

  final def info(msg: String): LogT[F, LogRecord, Unit] =
    log(Severity.Info, msg)

  final def info(msg: String, error: Throwable): LogT[F, LogRecord, Unit] =
    log(Severity.Info, msg, error)

  final def debug(msg: String): LogT[F, LogRecord, Unit] =
    log(Severity.Debug, msg)

  final def debug(msg: String, error: Throwable): LogT[F, LogRecord, Unit] =
    log(Severity.Debug, msg, error)

  final def log(severity: Severity, msg: String): LogT[F, LogRecord, Unit] =
    logMsg(LogRecord(severity, msg))

  final def log(severity: Severity, msg: String, error: Throwable): LogT[F, LogRecord, Unit] =
    logMsg(LogRecord(severity, msg, Some(error)))

}

object Logging {

  def simple[F[_]: Monad]: Logging[LogT[F, String, ?], Logger[F, String], String] =
    new Logging[LogT[F, String, ?], Logger[F, String], String] {
      val F: Monad[LogT[F, String, ?]] = LogT.logTMonad[F, String]

      val Ask: ApplicativeAsk[LogT[F, String, ?], Logger[F, String]] =
        LogT.logTApplicativeLocal[F, String]

      val HasLogger: HasLogger[LogT[F, String, ?], Logger[F, String], String] =
        LogT.logTHasLog[F, String]
    }

  def structured[F[_]: Monad]: StructuredLogging[F] = new StructuredLogging[F] {
    val F: Monad[LogT[F, LogRecord, ?]] = LogT.logTMonad[F, LogRecord]

    val Ask: ApplicativeAsk[LogT[F, LogRecord, ?], Logger[F, LogRecord]] =
      LogT.logTApplicativeLocal[F, LogRecord]

    val HasLogger: HasLogger[LogT[F, LogRecord, ?], Logger[F, LogRecord], LogRecord] =
      LogT.logTHasLog[F, LogRecord]
  }

}