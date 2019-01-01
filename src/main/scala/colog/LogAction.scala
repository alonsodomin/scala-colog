package colog

import cats._
import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import cats.mtl.lifting.ApplicativeLayer

final case class LogAction[F[_], A](log: A => F[Unit]) { self =>

  def apply(msg: A): F[Unit] = self.log(msg)

  def <&(msg: A): F[Unit] = self.log(msg)

  def extract(implicit M: Monoid[A]): F[Unit] =
    self.log(M.empty)

  def extend(f: LogAction[F, A] => F[Unit])(implicit S: Semigroup[A]): LogAction[F, A] =
    LogAction(m1 => f(LogAction(m2 => self.log(m1 |+| m2))))

  def decideWith[B, C](other: LogAction[F, B])(f: C => Either[A, B]): LogAction[F, C] =
    LogAction(f(_).fold(self.log, other.log))

  def contramap[B](f: B => A): LogAction[F, B] =
    LogAction(self.log.compose(f))

  def contramapF[B](f: B => F[A])(implicit F: FlatMap[F]): LogAction[F, B] =
    LogAction(Kleisli(f).andThen(self.log).run)

  def filter(f: A => Boolean)(implicit F: Applicative[F]): LogAction[F, A] =
    LogAction(msg => F.whenA(f(msg))(self.log(msg)))

  def mapK[G[_]](f: F ~> G): LogAction[G, A] =
    LogAction(msg => f.apply(self.log(msg)))

  def lift[G[_]](implicit G: ApplicativeLayer[G, F]): LogAction[G, A] =
    LogAction(msg => G.layer(self.log(msg)))

}

object LogAction extends LogActionFunctions with LogActionInstances1

private[colog] trait LogActionFunctions {

  def stdout[F[_]](implicit F: LiftIO[F]): LogAction[F, String] =
    LogAction(str => F.liftIO(IO(println(str))))

  def stderr[F[_]](implicit F: LiftIO[F]): LogAction[F, String] =
    LogAction(str => F.liftIO(IO(System.err.println(str))))

  def liftIO[F[_], A](action: LogAction[IO, A])(implicit F: LiftIO[F]): LogAction[F, A] =
    LogAction(msg => F.liftIO(action.log(msg)))

}

private[colog] trait LogActionInstances1 extends LogActionInstances0 {

  implicit def logActionHasLog[F[_], A]: HasLog[F, LogAction[F, A], A] = new HasLog[F, LogAction[F, A], A] {

    def getLogAction(env: LogAction[F, A]): LogAction[F, A] = env

    def setLogAction(action: LogAction[F, A], env: LogAction[F, A]): LogAction[F, A] = action

  }

  implicit def logActionMonoidK[F[_]](implicit F: Applicative[F]): MonoidK[LogAction[F, ?]] = new MonoidK[LogAction[F, ?]] {
    override def empty[A]: LogAction[F, A] = LogAction(_ => F.pure(()))

    override def combineK[A](x: LogAction[F, A], y: LogAction[F, A]): LogAction[F, A] =
      LogAction(a => x.log(a) *> y.log(a))
  }

  implicit def logActionMonoid[F[_], A](implicit F: Applicative[F]): Monoid[LogAction[F, A]] =
    MonoidK[LogAction[F, ?]].algebra[A]

  implicit def logActionContravariantMonoidal[F[_]](implicit F: Applicative[F]): ContravariantMonoidal[LogAction[F, ?]] =
    new LogActionContravariant[F] with ContravariantMonoidal[LogAction[F, ?]] {

      override def unit: LogAction[F, Unit] =
        Monoid[LogAction[F, Unit]].empty

      override def product[A, B](fa: LogAction[F, A], fb: LogAction[F, B]): LogAction[F, (A, B)] =
        LogAction({ case (a, b) => fa.log(a) *> fb.log(b) })
    }

}

private[colog] trait LogActionInstances0 {
  implicit def logActionContravariant[F[_]]: Contravariant[LogAction[F, ?]] = new LogActionContravariant[F]
}

private[colog] class LogActionContravariant[F[_]] extends Contravariant[LogAction[F, ?]] {
  override def contramap[A, B](fa: LogAction[F, A])(f: B => A): LogAction[F, B] =
    fa.contramap(f)
}