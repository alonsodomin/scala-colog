package colog

import cats._
import cats.data.Kleisli
import cats.implicits._
import cats.mtl.implicits._
import cats.mtl.lifting.{ApplicativeLayer, ApplicativeLayerFunctor}

abstract case class LoggerT[F[_], M, A] private (private[colog] val logReader: Kleisli[F, LogAction[LoggerT[F, M, ?], M], A]) {

  def ap[B](ff: LoggerT[F, M, A => B])(implicit F: Apply[F]): LoggerT[F, M, B] =
    new LoggerT(logReader.ap(ff.logReader)) {}

  def map[B](f: A => B)(implicit F: Functor[F]): LoggerT[F, M, B] =
    new LoggerT(logReader.map(f)) {}

  def flatMap[B](f: A => LoggerT[F, M, B])(implicit F: FlatMap[F]): LoggerT[F, M, B] =
    new LoggerT(logReader.flatMap(a => f(a).logReader)) {}

  def viaAction(action: LogAction[F, M])(implicit F: Monad[F]): F[A] = {
    logReader.run(action.lift[LoggerT[F, M, ?]])
  }

}

object LoggerT extends LoggerInstances2 {

  def apply[F[_], Msg, A](f: LogAction[LoggerT[F, Msg, ?], Msg] => F[A]): LoggerT[F, Msg, A] =
    new LoggerT(Kleisli(f)) {}

  def pure[F[_], Msg, A](a: A)(implicit F: Applicative[F]): LoggerT[F, Msg, A] =
    apply(_ => F.pure(a))

  def liftF[F[_], Msg, A](fa: F[A]): LoggerT[F, Msg, A] = apply(_ => fa)

  type LogActionF[F[_], Msg] = LogAction[LoggerT[F, Msg, ?], Msg]

}

private[colog] trait LoggerInstances2 extends LoggerInstances1 {

  implicit def loggerTApplicativeLayer[F[_], Msg](implicit F0: Applicative[F]): ApplicativeLayer[LoggerT[F, Msg, ?], F] =
    new LoggerTApplicativeLayerFunctor[F, Msg] {
      override val outerInstance: Applicative[LoggerT[F, Msg, ?]] = Applicative[LoggerT[F, Msg, ?]]
      override val innerInstance: Applicative[F] = F0
    }

  implicit def loggerTMonad[F[_], Msg](implicit F0: Monad[F]): Monad[LoggerT[F, Msg, ?]] = new LoggerTMonad[F, Msg] {
    override implicit def F: Monad[F] = F0
  }

}

private[colog] trait LoggerInstances1 {
  implicit def loggerTApplicative[F[_], Msg](implicit F0: Applicative[F]): Applicative[LoggerT[F, Msg, ?]] = new LoggerTApplicative[F, Msg] {
    override implicit def F: Applicative[F] = F0
  }
}

private[colog] trait LoggerTInstances0 {

  implicit def loggerTFunctor[F[_], Msg](implicit F0: Functor[F]): Functor[LoggerT[F, Msg, ?]] =
    new LoggerTFunctor[F, Msg] {
      implicit def F: Functor[F] = F0
    }

}

private[colog] trait LoggerTApplicativeLayerFunctor[F[_], Msg] extends ApplicativeLayer[LoggerT[F, Msg, ?], F] {

  override def layer[A](inner: F[A]): LoggerT[F, Msg, A] = LoggerT.liftF(inner)

  override def layerImapK[A](ma: LoggerT[F, Msg, A])(forward: F ~> F, backward: F ~> F): LoggerT[F, Msg, A] = ma
}

private[colog] trait LoggerTMonad[F[_], Msg] extends Monad[LoggerT[F, Msg, ?]]
  with LoggerTApplicative[F, Msg]
  with LoggerTFlatMap[F, Msg] {
  implicit def F: Monad[F]
}

private[colog] trait LoggerTFlatMap[F[_], Msg] extends FlatMap[LoggerT[F, Msg, ?]] with LoggerTApply[F, Msg] {
  implicit def F: FlatMap[F]

  override def flatMap[A, B](fa: LoggerT[F, Msg, A])(f: A => LoggerT[F, Msg, B]): LoggerT[F, Msg, B] =
    fa.flatMap(f)

  override def tailRecM[A, B](a: A)(f: A => LoggerT[F, Msg, Either[A, B]]): LoggerT[F, Msg, B] =
    LoggerT[F, Msg, B]({ b =>
      F.tailRecM(a)(f(_).logReader.run(b))
    })
}

private[colog] trait LoggerTApplicative[F[_], Msg] extends Applicative[LoggerT[F, Msg, ?]] with LoggerTApply[F, Msg] {
  implicit def F: Applicative[F]

  override def pure[A](x: A): LoggerT[F, Msg, A] = LoggerT.pure[F, Msg, A](x)
}

private[colog] trait LoggerTApply[F[_], Msg] extends Apply[LoggerT[F, Msg, ?]] with LoggerTFunctor[F, Msg] {
  implicit def F: Apply[F]

  override def ap[A, B](ff: LoggerT[F, Msg, A => B])(fa: LoggerT[F, Msg, A]): LoggerT[F, Msg, B] = fa.ap(ff)

}

private[colog] trait LoggerTFunctor[F[_], Msg] extends Functor[LoggerT[F, Msg, ?]] {
  implicit def F: Functor[F]

  override def map[A, B](fa: LoggerT[F, Msg, A])(f: A => B): LoggerT[F, Msg, B] = fa.map(f)
}