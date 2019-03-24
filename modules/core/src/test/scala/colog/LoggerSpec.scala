/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats.effect.IO
//import cats.effect.laws.discipline.arbitrary._
import cats.kernel.laws.discipline.MonoidTests
import cats.laws.discipline.{ContravariantMonoidalTests, MonoidKTests}
import cats.laws.discipline.arbitrary._
import cats.mtl.implicits._

class LoggerSpec extends CologSuite {
  type LogIOF[A] = MemLogT[IO, String, A]
  type LogF[A] = MemLog[String, A]

  checkAllAsync("Monoid[Logger]", implicit ec => MonoidTests[Logger[LogF, String]].monoid)
  checkAllAsync("MonoidK[Logger]", implicit ec => MonoidKTests[Logger[LogF, ?]].monoidK[String])
  checkAllAsync(
    "ContravariantMonoidal[Logger]",
    implicit ec =>
      ContravariantMonoidalTests[Logger[LogF, ?]].contravariantMonoidal[String, String, String]
  )

  test("filter must discard log statements that don't meet the criteria") {
    val logger = Loggers.pure[LogF, String]
      .filter(_.length >= 3)

    val logged = for {
      _ <- logger.log("ac")
      _ <- logger.log("abcd")
      _ <- logger.log("abc")
    } yield ()

    val (logs, _) = logged.run
    logs should contain theSameElementsInOrderAs Seq("abcd", "abc")
  }

}
