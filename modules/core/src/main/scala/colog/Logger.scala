/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats._
import cats.effect._
import cats.implicits._
import cats.mtl.lifting.FunctorLayer

import scala.concurrent.ExecutionContext

final class Logger[F[_], A](val log: A => F[Unit]) { self =>

  def apply(msg: A): F[Unit] = self.log(msg)

  def async(ec: ExecutionContext)(implicit F: Async[F], CS: ContextShift[F]): Logger[F, A] =
    Logger(msg => CS.evalOn(ec)(self.log(msg)))

  def <&(msg: A): F[Unit] = apply(msg)

  def >::[B](a: A): Logger[F, B] =
    Logger(_ => apply(a))

  def >*<[B](fb: Logger[F, B])(implicit F: Applicative[F]): Logger[F, (A, B)] =
    Logger({ case (a, b) => self.log(a) *> fb.log(b) })

  def >*(fu: Logger[F, Unit])(implicit F: Applicative[F]): Logger[F, A] =
    Logger(a => self.log(a) <* fu.log(()))

  def *<[B](fb: Logger[F, B])(implicit F: Applicative[F], ev: Unit =:= A): Logger[F, B] =
    Logger(b => self.log(ev(())) *> fb.log(b))

  def =>>(f: Logger[F, A] => F[Unit])(implicit S: Semigroup[A]): Logger[F, A] =
    extend(f)

  def extract(implicit M: Monoid[A]): F[Unit] =
    apply(M.empty)

  def extend(f: Logger[F, A] => F[Unit])(implicit S: Semigroup[A]): Logger[F, A] =
    Logger(m1 => f(Logger(m2 => self.log(m1 |+| m2))))

  def formatWith[B](f: B => A): Logger[F, B] =
    Logger(self.log.compose(f))

  def formatWithF[B](f: B => F[A])(implicit F: FlatMap[F]): Logger[F, B] =
    Logger(b => F.flatMap(f(b))(self.log))

  def formatWithOption[B](f: B => Option[A])(implicit F: Applicative[F]): Logger[F, B] =
    Logger(b => f(b).fold(F.unit)(self.log))

  def filter(f: A => Boolean)(implicit F: Applicative[F]): Logger[F, A] =
    Logger(msg => F.whenA(f(msg))(self.log(msg)))

  def mapK[G[_]](f: F ~> G): Logger[G, A] =
    Logger(msg => f(self.log(msg)))

  def lift[G[_]](implicit G: FunctorLayer[G, F]): Logger[G, A] =
    Logger(msg => G.layer(self.log(msg)))

  def timestampedWith[B](
      f: Timestamped[B] => A
  )(implicit F: Sync[F], timer: Timer[F]): Logger[F, B] = {
    val baseLogger = self.formatWith(f)
    Logger { msg =>
      for {
        millis <- timer.clock.realTime(TimeUnit.MILLISECONDS)
        _      <- baseLogger.log((Instant.ofEpochMilli(millis), msg))
      } yield ()
    }
  }

  @inline
  def >|<[B](fb: Logger[F, B]): Logger[F, Either[A, B]] = orElse(fb)

  def or[B, C](other: Logger[F, B])(f: C => Either[A, B]): Logger[F, C] =
    Logger(f(_).fold(self.log, other.log))

  def orElse[B](fb: Logger[F, B]): Logger[F, Either[A, B]] =
    or(fb)(identity)

}

object Logger extends LoggerFunctions with LoggerInstances1

private[colog] trait LoggerFunctions {

  def apply[F[_], A](f: A => F[Unit]): Logger[F, A] =
    new Logger(f)

  def liftF[F[_]](fu: F[Unit]): Logger[F, Unit] =
    Logger(_ => fu)

  def liftIO[F[_], A](logger: Logger[IO, A])(implicit F: LiftIO[F]): Logger[F, A] =
    Logger(msg => F.liftIO(logger.log(msg)))

}

private[colog] trait LoggerInstances1 extends LoggerInstances0 {

  implicit def loggerHasLogger[F[_], A]: HasLogger[F, Logger[F, A], A] =
    new HasLogger[F, Logger[F, A], A] {
      def getLogger(env: Logger[F, A]): Logger[F, A]                       = env
      def setLogger(env: Logger[F, A])(logger: Logger[F, A]): Logger[F, A] = logger

      override def withLogger(env: Logger[F, A])(
          f: Logger[F, A] => Logger[F, A]
      ): Logger[F, A] = env
    }

  implicit def loggerMonoidK[F[_]](implicit F: Applicative[F]): MonoidK[Logger[F, ?]] =
    new MonoidK[Logger[F, ?]] {
      override def empty[A]: Logger[F, A] = Logger(_ => F.unit)

      override def combineK[A](x: Logger[F, A], y: Logger[F, A]): Logger[F, A] =
        Logger(a => x.log(a) *> y.log(a))
    }

  implicit def loggerMonoid[F[_], A](implicit F: Applicative[F]): Monoid[Logger[F, A]] =
    loggerMonoidK[F].algebra[A]

  implicit def loggerContravariantMonoidal[F[_]](
      implicit F: Applicative[F]
  ): ContravariantMonoidal[Logger[F, ?]] =
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
    fa.formatWith(f)
}
