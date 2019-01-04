package colog

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats._
import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import cats.mtl.lifting.FunctorLayer

import scala.concurrent.ExecutionContext

final case class Logger[F[_], A](log: A => F[Unit]) { self =>

  def apply(msg: A): F[Unit] = self.log(msg)

  def async(ec: ExecutionContext)(implicit F: Async[F], CS: ContextShift[F]): Logger[F, A] =
    Logger(msg => CS.evalOn(ec)(self.log(msg)))

  def <&(msg: A): F[Unit] = self.log(msg)

  def >&<[B](f: B => A): Logger[F, B] = format(f)

  def >*<[B](fb: Logger[F, B])(implicit F: Applicative[F]): Logger[F, (A, B)] =
    Logger({ case (a, b) => self.log(a) *> fb.log(b) })

  def >*(fu: Logger[F, Unit])(implicit F: Applicative[F]): Logger[F, A] =
    Logger(a => self.log(a) <* fu.log(()))

  def =>>(f: Logger[F, A] => F[Unit])(implicit S: Semigroup[A]): Logger[F, A] =
    extend(f)

  def extract(implicit M: Monoid[A]): F[Unit] =
    self.log(M.empty)

  def extend(f: Logger[F, A] => F[Unit])(implicit S: Semigroup[A]): Logger[F, A] =
    Logger(m1 => f(Logger(m2 => self.log(m1 |+| m2))))

  def format[B](f: B => A): Logger[F, B] =
    Logger(self.log.compose(f))

  def formatF[B](f: B => F[A])(implicit F: FlatMap[F]): Logger[F, B] =
    Logger(Kleisli(f).andThen(self.log).run)

  def filter(f: A => Boolean)(implicit F: Applicative[F]): Logger[F, A] =
    Logger(msg => F.whenA(f(msg))(self.log(msg)))

  def mapK[G[_]](f: F ~> G): Logger[G, A] =
    Logger(msg => f.apply(self.log(msg)))

  def lift[G[_]](implicit G: FunctorLayer[G, F]): Logger[G, A] =
    Logger(msg => G.layer(self.log(msg)))

  def timestamped[B](f: Timestamped[B] => A)(implicit F: Sync[F], timer: Timer[F]): Logger[F, B] = {
    val baseLogger = self.format(f)
    Logger { msg =>
      for {
        millis <- timer.clock.realTime(TimeUnit.MILLISECONDS)
        _      <- baseLogger.log((Instant.ofEpochMilli(millis), msg))
      } yield ()
    }
  }

  def or[B, C](other: Logger[F, B])(f: C => Either[A, B]): Logger[F, C] =
    Logger(f(_).fold(self.log, other.log))

  def orElse[B](fb: Logger[F, B]): Logger[F, Either[A, B]] =
    or(fb)(identity)

}

object Logger extends LoggerFunctions with LoggerInstances1

private[colog] trait LoggerFunctions {

  def liftIO[F[_], A](logger: Logger[IO, A])(implicit F: LiftIO[F]): Logger[F, A] =
    Logger(msg => F.liftIO(logger.log(msg)))

}

private[colog] trait LoggerInstances1 extends LoggerInstances0 {

  implicit def loggerHasLogger[F[_], A]: HasLogger[F, Logger[F, A], A] = new HasLogger[F, Logger[F, A], A] {
    def getLogger(env: Logger[F, A]): Logger[F, A] = env
    def setLogger(logger: Logger[F, A], env: Logger[F, A]): Logger[F, A] = logger
  }

  implicit def loggerMonoidK[F[_]](implicit F: Applicative[F]): MonoidK[Logger[F, ?]] = new MonoidK[Logger[F, ?]] {
    override def empty[A]: Logger[F, A] = Logger(_ => F.pure(()))

    override def combineK[A](x: Logger[F, A], y: Logger[F, A]): Logger[F, A] =
      Logger(a => x.log(a) *> y.log(a))
  }

  implicit def loggerMonoid[F[_], A](implicit F: Applicative[F]): Monoid[Logger[F, A]] =
    loggerMonoidK[F].algebra[A]

  implicit def loggerContravariantMonoidal[F[_]](implicit F: Applicative[F]): ContravariantMonoidal[Logger[F, ?]] =
    new LoggerContravariant[F] with ContravariantMonoidal[Logger[F, ?]] {

      def unit: Logger[F, Unit] = loggerMonoid[F, Unit].empty

      def product[A, B](fa: Logger[F, A], fb: Logger[F, B]): Logger[F, (A, B)] = fa >*< fb
    }

}

private[colog] trait LoggerInstances0 {
  implicit def loggerContravariant[F[_]]: Contravariant[Logger[F, ?]] = new LoggerContravariant[F]
}

private[colog] class LoggerContravariant[F[_]] extends Contravariant[Logger[F, ?]] {
  override def contramap[A, B](fa: Logger[F, A])(f: B => A): Logger[F, B] =
    fa.format(f)
}