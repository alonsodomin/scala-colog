package colog

import cats._
import cats.data.Kleisli
import cats.effect._
import cats.mtl._
import cats.mtl.lifting.ApplicativeLayer

abstract case class LoggerT[F[_], A, B] private[colog] (
    private[colog] val unwrap: Kleisli[F, LogAction[LoggerT[F, A, ?], A], B]
  ) {

  def ap[C](ff: LoggerT[F, A, B => C])(implicit F: Apply[F]): LoggerT[F, A, C] =
    new LoggerT(unwrap.ap(ff.unwrap)) {}

  def map[C](f: B => C)(implicit F: Functor[F]): LoggerT[F, A, C] =
    new LoggerT(unwrap.map(f)) {}

  def local(f: LogAction[F, A] => LogAction[F, A])(implicit F: Applicative[F]): LoggerT[F, A, B] =
    new LoggerT[F, A, B](unwrap.local(act => f(act.mapK(LoggerT.algebra[F, A])).lift[LoggerT[F, A, ?]])) {}

  def flatMap[C](f: B => LoggerT[F, A, C])(implicit F: FlatMap[F]): LoggerT[F, A, C] =
    new LoggerT(unwrap.flatMap(a => f(a).unwrap)) {}

  def imapK[G[_]](f: F ~> G, g: G ~> F): LoggerT[G, A, B] = {
    val newArrow = Kleisli[G, LogAction[LoggerT[G, A, ?], A], B] { act =>
      val y = LogAction[LoggerT[F, A, ?], A](msg => act.log(msg).imapK(g, f))
      unwrap.mapK(f).run(y)
    }
    new LoggerT[G, A, B](newArrow) {}
  }

  def runWith(action: LogAction[F, A])(implicit F: Applicative[F]): F[B] = {
    unwrap.run(action.lift[LoggerT[F, A, ?]])
  }

}

object LoggerT extends LoggerFunctions with LoggerInstances2

private[colog] trait LoggerFunctions {

  private[colog] def algebra[F[_]: Applicative, A](implicit M: Monoid[LogAction[F, A]]): LoggerT[F, A, ?] ~> F =
    new (LoggerT[F, A, ?] ~> F) {
      override def apply[B](fa: LoggerT[F, A, B]): F[B] = fa.runWith(M.empty)
    }

  def apply[F[_], A, B](f: LogAction[F, A] => F[B])(implicit F: Applicative[F]): LoggerT[F, A, B] =
    new LoggerT(Kleisli[F, LogAction[LoggerT[F, A, ?], A], B](act => f(act.mapK(algebra[F, A])))) {}

  def pure[F[_], A, B](a: B)(implicit F: Applicative[F]): LoggerT[F, A, B] =
    apply(_ => F.pure(a))

  def unit[F[_]: Applicative, A]: LoggerT[F, A, Unit] = pure(())
  
  def liftF[F[_]: Applicative, A, B](fa: F[B]): LoggerT[F, A, B] = apply(_ => fa)
  
}

private[colog] trait LoggerInstances2 extends LoggerInstances1 {

  implicit def loggerTApplicativeLayer[F[_], A](implicit F0: Applicative[F]): ApplicativeLayer[LoggerT[F, A, ?], F] =
    new LoggerTApplicativeLayer[F, A] {
      override val outerInstance: Applicative[LoggerT[F, A, ?]] = Applicative[LoggerT[F, A, ?]]
      override val innerInstance: Applicative[F] = F0
    }

  implicit def loggerTHasLog[F[_], A](implicit F: ApplicativeLayer[LoggerT[F, A, ?], F]): HasLog[LoggerT[F, A, ?], LogAction[F, A], A] =
    new HasLog[LoggerT[F, A, ?], LogAction[F, A], A] {
      override def getLogAction(env: LogAction[F, A]): LogAction[LoggerT[F, A, ?], A] =
        env.lift[LoggerT[F, A, ?]]

      override def setLogAction(action: LogAction[LoggerT[F, A, ?], A], env: LogAction[F, A]): LogAction[F, A] = env
    }

  implicit def loggerTApplicativeLocal[F[_], A](
      implicit F: Applicative[F]
  ): ApplicativeLocal[LoggerT[F, A, ?], LogAction[F, A]] =
    new DefaultApplicativeLocal[LoggerT[F, A, ?], LogAction[F, A]] {
      override val applicative: Applicative[LoggerT[F, A, ?]] = loggerTApplicative[F, A]

      override def local[B](f: LogAction[F, A] => LogAction[F, A])(fa: LoggerT[F, A, B]): LoggerT[F, A, B] =
        fa.local(f)

      override def ask: LoggerT[F, A, LogAction[F, A]] =
        LoggerT(action => F.pure(action))
    }

  implicit def loggerTAsync[F[_], A](implicit F0: Async[F]): Async[LoggerT[F, A, ?]] =
    new LoggerTAsync[F, A] {
      override implicit def F: Async[F] = F0
    }

  implicit def loggerTLiftIO[F[_]: Applicative, A](implicit F: LiftIO[F]): LiftIO[LoggerT[F, A, ?]] = new LiftIO[LoggerT[F, A, ?]] {
    override def liftIO[B](ioa: IO[B]): LoggerT[F, A, B] = LoggerT(_ => F.liftIO(ioa))
  }

}

private[colog] trait LoggerInstances1 {

  implicit def loggerTMonad[F[_], A](implicit F0: Monad[F]): Monad[LoggerT[F, A, ?]] = new LoggerTMonad[F, A] {
    override implicit def F: Monad[F] = F0
  }

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

private[colog] trait LoggerTAsync[F[_], Msg] extends Async[LoggerT[F, Msg, ?]] with LoggerTSync[F, Msg] {
  implicit def F: Async[F]

  override def async[A](k: (Either[Throwable, A] => Unit) => Unit): LoggerT[F, Msg, A] =
    LoggerT(_ => F.async(c => k(c)))

  override def asyncF[A](k: (Either[Throwable, A] => Unit) => LoggerT[F, Msg, Unit]): LoggerT[F, Msg, A] =
    LoggerT(action => F.asyncF(c => k(c).runWith(action)))

}

private[colog] trait LoggerTSync[F[_], Msg] extends Sync[LoggerT[F, Msg, ?]] with LoggerTMonadError[F, Msg, Throwable] {
  implicit def F: Sync[F]

  def suspend[A](thunk: => LoggerT[F, Msg, A]): LoggerT[F, Msg, A] =
    LoggerT(action => F.suspend(thunk.runWith(action)))

  def bracketCase[A, B](acquire: LoggerT[F, Msg, A])(
    use: A => LoggerT[F, Msg, B]
  )(
    release: (A, ExitCase[Throwable]) => LoggerT[F, Msg, Unit]
  ): LoggerT[F, Msg, B] =
    LoggerT(action => F.bracketCase(acquire.runWith(action))(a => use(a).runWith(action))((a, x) => release(a, x).runWith(action)))

}

private[colog] trait LoggerTMonadError[F[_], Msg, Err] extends MonadError[LoggerT[F, Msg, ?], Err]
  with LoggerTMonad[F, Msg]
  with LoggerTApplicativeError[F, Msg, Err] {
  def F: MonadError[F, Err]
}

private[colog] trait LoggerTApplicativeLayer[F[_], Msg] extends ApplicativeLayer[LoggerT[F, Msg, ?], F] {

  override def layer[A](inner: F[A]): LoggerT[F, Msg, A] = LoggerT.liftF(inner)(innerInstance)

  override def layerImapK[A](ma: LoggerT[F, Msg, A])(
    forward: F ~> F,
    backward: F ~> F
  ): LoggerT[F, Msg, A] = ma.imapK(forward, backward)
  
}

private[colog] trait LoggerTMonad[F[_], Msg] extends Monad[LoggerT[F, Msg, ?]]
  with LoggerTApplicative[F, Msg] {
  implicit def F: Monad[F]

  override def flatMap[A, B](fa: LoggerT[F, Msg, A])(f: A => LoggerT[F, Msg, B]): LoggerT[F, Msg, B] =
    fa.flatMap(f)

  override def tailRecM[A, B](a: A)(f: A => LoggerT[F, Msg, Either[A, B]]): LoggerT[F, Msg, B] =
    LoggerT[F, Msg, B]({ b =>
      F.tailRecM(a)(f(_).unwrap.run(b.lift[LoggerT[F, Msg, ?]]))
    })
}

private[colog] trait LoggerTApplicativeError[F[_], Msg, Err]
  extends ApplicativeError[LoggerT[F, Msg, ?], Err]
    with LoggerTApplicative[F, Msg] {

  implicit def F: ApplicativeError[F, Err]
  implicit final val M: Monoid[LogAction[F, Msg]] = Monoid[LogAction[F, Msg]]

  override def raiseError[A](e: Err): LoggerT[F, Msg, A] =
    LoggerT.liftF(F.raiseError(e))

  override def handleErrorWith[A](fa: LoggerT[F, Msg, A])(f: Err => LoggerT[F, Msg, A]): LoggerT[F, Msg, A] =
    LoggerT(action => F.handleErrorWith(fa.runWith(action))(err => f(err).runWith(M.empty)))

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