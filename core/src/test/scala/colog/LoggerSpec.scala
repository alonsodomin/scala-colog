/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats._
import cats.effect.IO
import cats.effect.laws.discipline.arbitrary._
import cats.effect.laws.util.TestContext
import cats.effect.laws.util.TestInstances._
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline.{ContravariantMonoidalTests, MonoidKTests}
import cats.laws.discipline.eq._
import cats.tests.CatsSuite

import org.scalacheck.{Arbitrary, Cogen}

class LoggerSpec extends CatsSuite {

  implicit val testCtx = TestContext()

  implicit def loggerEq[F[_], A](implicit A: Arbitrary[A], FU: Eq[F[Unit]]): Eq[Logger[F, A]] =
    Eq.by[Logger[F, A], A => F[Unit]](_.log)

  implicit def loggerArbitrary[F[_], A](implicit A: Arbitrary[A], CA: Cogen[A], FU: Arbitrary[F[Unit]]): Arbitrary[Logger[F, A]] =
    Arbitrary(Arbitrary.arbitrary[A => F[Unit]].map(Logger(_)))

  checkAll("Monoid[Logger]", MonoidTests[Logger[IO, String]].monoid)
  checkAll("MonoidK[Logger]", MonoidKTests[Logger[IO, ?]].monoidK[String])
  checkAll("ContravariantMonoidal[Logger]", ContravariantMonoidalTests[Logger[IO, ?]].contravariantMonoidal[String, String, String])

}
