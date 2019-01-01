import cats._
import cats.effect.IO
import cats.mtl.{ApplicativeAsk, ApplicativeLocal}
import cats.implicits._

package object colog {
  type LogActionIO[A] = LogAction[IO, A]
  type Logger[A, B] = LoggerT[Id, A, B]
  type LoggerIO[A, B] = LoggerT[IO, A, B]

  def withLog[F[_], E, A](fa: F[A])(f: LogAction[F, A] => LogAction[F, A])(
    implicit
    Local: ApplicativeLocal[F, E],
    Log: HasLog[F, E, A]
  ): F[A] = Local.local(HasLog.over(f))(fa)

  def logMsgF[F[_]: Monad, E, A](msg: A)(
    implicit
    Ask: ApplicativeAsk[F, E],
    Log: HasLog[F, E, A]
  ): F[Unit] = for {
    logAction <- Ask.reader(Log.getLogAction)
    _         <- logAction.log(msg)
  } yield ()

  def logMsg[F[_]: Monad, A](msg: A): LoggerT[F, A, Unit] =
    logMsgF[LoggerT[F, A, ?], LogAction[F, A], A](msg)

  def infoF[F[_]: Monad, E](msg: String)(
    implicit
    Ask: ApplicativeAsk[F, E],
    Log: HasLog[F, E, LogRecord]
  ): F[Unit] = logF[F, E](LogRecord.Level.Info, msg)

  def info[F[_]: Monad](msg: String): LoggerT[F, LogRecord, Unit] =
    infoF[LoggerT[F, LogRecord, ?], LogAction[F, LogRecord]](msg)

  def logF[F[_]: Monad, E](level: LogRecord.Level, msg: String)(
    implicit
    Ask: ApplicativeAsk[F, E],
    Log: HasLog[F, E, LogRecord]
  ): F[Unit] = logMsgF[F, E, LogRecord](LogRecord(level, msg))

  def log[F[_]: Monad](level: LogRecord.Level, msg: String): LoggerT[F, LogRecord, Unit] =
    logF[LoggerT[F, LogRecord, ?], LogAction[F, LogRecord]](level, msg)

  def usingLoggerT[F[_]: Applicative, A](action: LogAction[F, A])(loggerT: LoggerT[F, A, Unit]): F[Unit] =
    loggerT.runWith(action)

}
