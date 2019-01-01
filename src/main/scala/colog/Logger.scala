package colog

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats._
import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import cats.mtl.lifting.ApplicativeLayer

final case class Logger[F[_], A](log: A => F[Unit]) { self =>

  def apply(msg: A): F[Unit] = self.log(msg)

  def <&(msg: A): F[Unit] = self.log(msg)

  def extract(implicit M: Monoid[A]): F[Unit] =
    self.log(M.empty)

  def extend(f: Logger[F, A] => F[Unit])(implicit S: Semigroup[A]): Logger[F, A] =
    Logger(m1 => f(Logger(m2 => self.log(m1 |+| m2))))

  def decideWith[B, C](other: Logger[F, B])(f: C => Either[A, B]): Logger[F, C] =
    Logger(f(_).fold(self.log, other.log))

  def contramap[B](f: B => A): Logger[F, B] =
    Logger(self.log.compose(f))

  def contramapF[B](f: B => F[A])(implicit F: FlatMap[F]): Logger[F, B] =
    Logger(Kleisli(f).andThen(self.log).run)

  def filter(f: A => Boolean)(implicit F: Applicative[F]): Logger[F, A] =
    Logger(msg => F.whenA(f(msg))(self.log(msg)))

  def mapK[G[_]](f: F ~> G): Logger[G, A] =
    Logger(msg => f.apply(self.log(msg)))

  def lift[G[_]](implicit G: ApplicativeLayer[G, F]): Logger[G, A] =
    Logger(msg => G.layer(self.log(msg)))

}

object Logger extends LoggerFunctions with LoggerInstances1

private[colog] trait LoggerFunctions {

  def stdout[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    Logger(str => F.liftIO(IO(println(str))))

  def stderr[F[_]](implicit F: LiftIO[F]): Logger[F, String] =
    Logger(str => F.liftIO(IO(System.err.println(str))))

  def noop[F[_], A](implicit F: Applicative[F]): Logger[F, A] =
    Logger(_ => F.unit)

  def withTimestamps[F[_]](logger: Logger[F, LogRecord])(format: TimestampedLogRecord => LogRecord)(
    implicit F: Sync[F], timer: Timer[F]
  ): Logger[F, LogRecord] = Logger[F, LogRecord] { rec =>
    for {
      millis <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      now    <- F.pure(Instant.ofEpochMilli(millis))
      _      <- logger.contramap(format).log(TimestampedLogRecord(now, rec))
    } yield ()
  }

  def liftIO[F[_], A](logger: Logger[IO, A])(implicit F: LiftIO[F]): Logger[F, A] =
    Logger(msg => F.liftIO(logger.log(msg)))

}

private[colog] trait LoggerInstances1 extends LoggerInstances0 {

  implicit def loggerHasLog[F[_], A]: HasLogger[F, Logger[F, A], A] = new HasLogger[F, Logger[F, A], A] {

    def getLogger(env: Logger[F, A]): Logger[F, A] = env

    def setLogger(logger: Logger[F, A], env: Logger[F, A]): Logger[F, A] = logger

  }

  implicit def loggerMonoidK[F[_]](implicit F: Applicative[F]): MonoidK[Logger[F, ?]] = new MonoidK[Logger[F, ?]] {
    override def empty[A]: Logger[F, A] = Logger(_ => F.pure(()))

    override def combineK[A](x: Logger[F, A], y: Logger[F, A]): Logger[F, A] =
      Logger(a => x.log(a) *> y.log(a))
  }

  implicit def loggerMonoid[F[_], A](implicit F: Applicative[F]): Monoid[Logger[F, A]] =
    MonoidK[Logger[F, ?]].algebra[A]

  implicit def loggerContravariantMonoidal[F[_]](implicit F: Applicative[F]): ContravariantMonoidal[Logger[F, ?]] =
    new LoggerContravariant[F] with ContravariantMonoidal[Logger[F, ?]] {

      override def unit: Logger[F, Unit] =
        Monoid[Logger[F, Unit]].empty

      override def product[A, B](fa: Logger[F, A], fb: Logger[F, B]): Logger[F, (A, B)] =
        Logger({ case (a, b) => fa.log(a) *> fb.log(b) })
    }

}

private[colog] trait LoggerInstances0 {
  implicit def loggerContravariant[F[_]]: Contravariant[Logger[F, ?]] = new LoggerContravariant[F]
}

private[colog] class LoggerContravariant[F[_]] extends Contravariant[Logger[F, ?]] {
  override def contramap[A, B](fa: Logger[F, A])(f: B => A): Logger[F, B] =
    fa.contramap(f)
}