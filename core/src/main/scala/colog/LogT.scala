package colog

import cats._
import cats.data.Kleisli
import cats.effect._
import cats.mtl._
import cats.mtl.lifting.{ApplicativeLayer, MonadLayer}

abstract case class LogT[F[_], A, B] private[colog](
    private[colog] val unwrap: Kleisli[F, Logger[LogT[F, A, ?], A], B]
  ) { self =>

  def ap[C](ff: LogT[F, A, B => C])(implicit F: Apply[F]): LogT[F, A, C] =
    new LogT(unwrap.ap(ff.unwrap)) {}

  def leftMap[C](f: A => C)(implicit F: Applicative[F]): LogT[F, C, B] =
    local[C](l => l.format(f))

  def bimap[C, D](f: A => C, g: B => D)(implicit F: Applicative[F]): LogT[F, C, D] =
    leftMap(f).map(g)

  def map[C](f: B => C)(implicit F: Functor[F]): LogT[F, A, C] =
    new LogT(unwrap.map(f)) {}

  def local[AA](f: Logger[F, AA] => Logger[F, A])(implicit F: Applicative[F]): LogT[F, AA, B] =
    new LogT[F, AA, B](unwrap.local(logger => f(logger.mapK(LogT.algebra[F, AA])).lift[LogT[F, A, ?]])) {}

  def flatMapF[C](f: B => F[C])(implicit F: Monad[F]): LogT[F, A, C] =
    flatMap(b => LogT.liftF(f(b)))

  def flatMap[C](f: B => LogT[F, A, C])(implicit F: FlatMap[F]): LogT[F, A, C] =
    new LogT(unwrap.flatMap(a => f(a).unwrap)) {}

  def imapK[G[_]](f: F ~> G, g: G ~> F): LogT[G, A, B] = {
    val newArrow = Kleisli[G, Logger[LogT[G, A, ?], A], B] { logger =>
      val y = Logger[LogT[F, A, ?], A](msg => logger.log(msg).imapK(g, f))
      unwrap.mapK(f).run(y)
    }
    new LogT[G, A, B](newArrow) {}
  }

  def silent(implicit F: Applicative[F]): F[B] =
    via(Monoid[Logger[F, A]].empty)

  def via(logger: Logger[F, A])(implicit F: Applicative[F]): F[B] =
    unwrap.run(logger.lift[LogT[F, A, ?]])

}

object LogT extends LogTFunctions with LogTEffectInstances

private[colog] trait LogTFunctions {

  private[colog] def algebra[F[_]: Applicative, A](implicit M: Monoid[Logger[F, A]]): LogT[F, A, ?] ~> F =
    new (LogT[F, A, ?] ~> F) {
      override def apply[B](fa: LogT[F, A, B]): F[B] = fa.via(M.empty)
    }

  def apply[F[_], A, B](f: Logger[F, A] => F[B])(implicit F: Applicative[F]): LogT[F, A, B] =
    new LogT(Kleisli[F, Logger[LogT[F, A, ?], A], B](logger => f(logger.mapK(algebra[F, A])))) {}

  def pure[F[_], A, B](a: B)(implicit F: Applicative[F]): LogT[F, A, B] =
    apply(_ => F.pure(a))

  def getLogger[F[_], A](implicit F: Applicative[F]): LogT[F, A, Logger[F, A]] = apply(F.pure)

  def unit[F[_]: Applicative, A]: LogT[F, A, Unit] = pure(())

  def liftF[F[_]: Applicative, A, B](fa: F[B]): LogT[F, A, B] = apply(_ => fa)

}

private[colog] trait LogTEffectInstances extends LogTEffectInstances0 {

  implicit def logTConcurrent[F[_], A](implicit F0: Concurrent[F]): Concurrent[LogT[F, A, ?]] =
    new LogTConcurrent[F, A] {
      val F: Concurrent[F] = F0
    }

}

private[colog] trait LogTEffectInstances0 extends LogTMtlInstances {

  implicit def logTAsync[F[_], A](implicit F0: Async[F]): Async[LogT[F, A, ?]] =
    new LogTAsync[F, A] {
      val F: Async[F] = F0
    }

  implicit def logTLiftIO[F[_]: Applicative, A](implicit F: LiftIO[F]): LiftIO[LogT[F, A, ?]] = new LiftIO[LogT[F, A, ?]] {
    def liftIO[B](ioa: IO[B]): LogT[F, A, B] = LogT(_ => F.liftIO(ioa))
  }

}

private[colog] trait LogTMtlInstances extends LogTMtlInstances1 {

  implicit def logTApplicativeLocal[F[_], A](
    implicit F: Applicative[F]
  ): ApplicativeLocal[LogT[F, A, ?], Logger[F, A]] =
    new DefaultApplicativeLocal[LogT[F, A, ?], Logger[F, A]] {
      val applicative: Applicative[LogT[F, A, ?]] = logTApplicative[F, A]

      def local[B](f: Logger[F, A] => Logger[F, A])(fa: LogT[F, A, B]): LogT[F, A, B] =
        fa.local(f)

      def ask: LogT[F, A, Logger[F, A]] = LogT.getLogger[F, A]
    }

}

private[colog] trait LogTMtlInstances1 extends LogTMtlInstances0 {

  implicit def logTMonadLayer[F[_], A](implicit F0: Monad[F]): MonadLayer[LogT[F, A, ?], F] =
    new LogTApplicativeLayer[F, A] with MonadLayer[LogT[F, A, ?], F] {
      val outerInstance: Monad[LogT[F, A, ?]] = logTMonad[F, A]
      val innerInstance: Monad[F] = F0
    }

}

private[colog] trait LogTMtlInstances0 extends DirectLogTInstances {

  implicit def logTApplicativeLayer[F[_], A](implicit F0: Applicative[F]): ApplicativeLayer[LogT[F, A, ?], F] =
    new LogTApplicativeLayer[F, A] {
      val outerInstance: Applicative[LogT[F, A, ?]] = logTApplicative[F, A]
      val innerInstance: Applicative[F] = F0
    }

}

private[colog] trait DirectLogTInstances extends DirectLogTInstances2

private[colog] trait DirectLogTInstances2 extends DirectLogTInstances1 {

  implicit def logTMonad[F[_], A](implicit F0: Monad[F]): Monad[LogT[F, A, ?]] = new LogTMonad[F, A] {
    val F: Monad[F] = F0
  }

}

private[colog] trait DirectLogTInstances1 extends DirectLogTInstances0 {

  implicit def logTApplicative[F[_], Msg](implicit F0: Applicative[F]): Applicative[LogT[F, Msg, ?]] = new LogTApplicative[F, Msg] {
    val F: Applicative[F] = F0
  }

}

private[colog] trait DirectLogTInstances0 {

  implicit def logTBifunctor[F[_]](implicit F0: Applicative[F]): Bifunctor[LogT[F, ?, ?]] =
    new LogTBifunctor[F] {
      val F: Applicative[F] = F0
    }

  implicit def logTFunctor[F[_], Msg](implicit F0: Functor[F]): Functor[LogT[F, Msg, ?]] =
    new LogTFunctor[F, Msg] {
      val F: Functor[F] = F0
    }

}

private[colog] trait LogTConcurrent[F[_], Msg] extends Concurrent[LogT[F, Msg, ?]] with LogTAsync[F, Msg] {
  implicit val F: Concurrent[F]

  type LogTFiber[A] = Fiber[LogT[F, Msg, ?], A]

  def start[A](fa: LogT[F, Msg, A]): LogT[F, Msg, LogTFiber[A]] =
    LogT(logger => F.map(F.start[A](fa.via(logger)))(fiberT))

  def racePair[A, B](fa: LogT[F, Msg, A], fb: LogT[F, Msg, B]): LogT[F, Msg, Either[(A, LogTFiber[B]), (LogTFiber[A], B)]] =
    LogT { logger =>
      F.map(F.racePair(fa.via(logger), fb.via(logger))) {
        case Left((a, fiber)) => Left((a, fiberT(fiber)))
        case Right((fiber, b)) => Right((fiberT(fiber), b))
      }
    }

  protected def fiberT[A](fiber: Fiber[F, A]): LogTFiber[A] =
    Fiber(LogT.liftF(fiber.join), LogT.liftF(fiber.cancel))

}

private[colog] trait LogTAsync[F[_], Msg] extends Async[LogT[F, Msg, ?]] with LogTSync[F, Msg] {
  implicit val F: Async[F]

  override def async[A](k: (Either[Throwable, A] => Unit) => Unit): LogT[F, Msg, A] =
    LogT(_ => F.async(c => k(c)))

  override def asyncF[A](k: (Either[Throwable, A] => Unit) => LogT[F, Msg, Unit]): LogT[F, Msg, A] =
    LogT(logger => F.asyncF(c => k(c).via(logger)))

}

private[colog] trait LogTSync[F[_], Msg] extends Sync[LogT[F, Msg, ?]] with LogTMonadError[F, Msg, Throwable] {
  implicit val F: Sync[F]

  def suspend[A](thunk: => LogT[F, Msg, A]): LogT[F, Msg, A] =
    LogT(logger => F.suspend(thunk.via(logger)))

  def bracketCase[A, B](acquire: LogT[F, Msg, A])(
    use: A => LogT[F, Msg, B]
  )(
    release: (A, ExitCase[Throwable]) => LogT[F, Msg, Unit]
  ): LogT[F, Msg, B] =
    LogT(logger => F.bracketCase(acquire.via(logger))(a => use(a).via(logger))((a, x) => release(a, x).via(logger)))

}

private[colog] trait LogTMonadError[F[_], Msg, Err] extends MonadError[LogT[F, Msg, ?], Err]
  with LogTMonad[F, Msg]
  with LogTApplicativeError[F, Msg, Err] {

  val F: MonadError[F, Err]
}

private[colog] trait LogTApplicativeLayer[F[_], Msg] extends ApplicativeLayer[LogT[F, Msg, ?], F] {

  override def layer[A](inner: F[A]): LogT[F, Msg, A] = LogT.liftF(inner)(innerInstance)

  override def layerImapK[A](ma: LogT[F, Msg, A])(
    forward: F ~> F,
    backward: F ~> F
  ): LogT[F, Msg, A] = ma.imapK(forward, backward)

}

private[colog] trait LogTMonad[F[_], Msg] extends Monad[LogT[F, Msg, ?]]
  with LogTApplicative[F, Msg] {
  implicit val F: Monad[F]

  override def flatMap[A, B](fa: LogT[F, Msg, A])(f: A => LogT[F, Msg, B]): LogT[F, Msg, B] =
    fa.flatMap(f)

  override def tailRecM[A, B](a: A)(f: A => LogT[F, Msg, Either[A, B]]): LogT[F, Msg, B] =
    LogT[F, Msg, B]({ b =>
      F.tailRecM(a)(f(_).unwrap.run(b.lift[LogT[F, Msg, ?]]))
    })
}

private[colog] trait LogTApplicativeError[F[_], Msg, Err]
  extends ApplicativeError[LogT[F, Msg, ?], Err]
    with LogTApplicative[F, Msg] {

  implicit val F: ApplicativeError[F, Err]

  override def raiseError[A](e: Err): LogT[F, Msg, A] =
    LogT.liftF(F.raiseError(e))

  override def handleErrorWith[A](fa: LogT[F, Msg, A])(f: Err => LogT[F, Msg, A]): LogT[F, Msg, A] =
    LogT(logger => F.handleErrorWith(fa.via(logger))(err => f(err).via(Logger.loggerMonoid[F, Msg].empty)))

}

private[colog] trait LogTApplicative[F[_], Msg] extends Applicative[LogT[F, Msg, ?]] with LogTApply[F, Msg] {
  implicit val F: Applicative[F]

  override def pure[A](x: A): LogT[F, Msg, A] = LogT.pure[F, Msg, A](x)
}

private[colog] trait LogTApply[F[_], Msg] extends Apply[LogT[F, Msg, ?]] with LogTFunctor[F, Msg] {
  implicit val F: Apply[F]

  override def ap[A, B](ff: LogT[F, Msg, A => B])(fa: LogT[F, Msg, A]): LogT[F, Msg, B] = fa.ap(ff)

}

private[colog] trait LogTBifunctor[F[_]] extends Bifunctor[LogT[F, ?, ?]] {
  implicit val F: Applicative[F]

  def bimap[A, B, C, D](fab: LogT[F, A, B])(f: A => C, g: B => D): LogT[F, C, D] =
    fab.bimap(f, g)
}

private[colog] trait LogTFunctor[F[_], Msg] extends Functor[LogT[F, Msg, ?]] {
  implicit val F: Functor[F]

  override def map[A, B](fa: LogT[F, Msg, A])(f: A => B): LogT[F, Msg, B] = fa.map(f)
}