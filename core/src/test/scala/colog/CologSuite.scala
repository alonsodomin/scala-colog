/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats._
import cats.effect._
import cats.effect.laws.util.TestContext
import cats.tests.CatsSuite
import cats.laws.discipline.eq._
import org.scalacheck.{Arbitrary, Cogen}

abstract class CologSuite extends CatsSuite {

  implicit val testCtx: TestContext = TestContext()

  implicit def contextShift[F[_]](implicit F: Async[F]): ContextShift[F] = testCtx.contextShift[F]

  implicit def loggerEq[F[_], A](implicit A: Arbitrary[A], FU: Eq[F[Unit]]): Eq[Logger[F, A]] =
    Eq.by[Logger[F, A], A => F[Unit]](_.log)

  implicit def loggerArbitrary[F[_], A](
      implicit A: Arbitrary[A],
      CA: Cogen[A],
      FU: Arbitrary[F[Unit]]
  ): Arbitrary[Logger[F, A]] =
    Arbitrary(Arbitrary.arbitrary[A => F[Unit]].map(Logger(_)))

  implicit def loggerCogen[F[_], A](
      implicit
      F: Applicative[F],
      CU: Cogen[F[Unit]]
  ): Cogen[Logger[F, A]] =
    Cogen((seed, _) => CU.perturb(seed, F.unit))

  implicit def logTEq[F[_], A, B](
      implicit A: Arbitrary[Logger[F, A]],
      F: Applicative[F],
      FB: Eq[F[B]]
  ): Eq[LogT[F, A, B]] =
    Eq.by[LogT[F, A, B], Logger[F, A] => F[B]](_.via)

  implicit def logTArbitrary[F[_], A, B](
      implicit A: Arbitrary[A],
      CA: Cogen[Logger[F, A]],
      F: Applicative[F],
      FB: Arbitrary[F[B]]
  ): Arbitrary[LogT[F, A, B]] =
    Arbitrary(Arbitrary.arbitrary[Logger[F, A] => F[B]].map(LogT.apply(_)))

}
