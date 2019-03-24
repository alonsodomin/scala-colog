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
import cats.mtl.FunctorTell
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

  test("formatWithF must emit a translated statement in the returned monad") {
    val logger = Loggers.pure[LogF, String].formatWithF((str: String) => str.toUpperCase.pure[LogF])
    val (logs, _) = logger.log("hello").run
    logs should contain theSameElementsAs Seq("HELLO")
  }

  test("formatWithOption must not emit a log statement if no result is returned") {
    val logger = Loggers.pure[LogF, String].formatWithOption((_: String) => none[String])
    val (logs, _) = logger.log("hello").run
    logs.length shouldBe 0
  }

  test("extract must emit a log statement with the empty member of a monoid") {
    type LogIntF[A] = MemLog[Int, A]
    val logger = Loggers.pure[LogIntF, Int]
    val (logs, _) = logger.extract.run
    logs should contain theSameElementsAs Seq(0)
  }

  test(">* must retain the type of the left logger") {
    val loggerU = Logger.liftF(FunctorTell[LogF, Vector[String]].tell(Vector("const")))
    val loggerA = Loggers.pure[LogF, String]
    val logger = loggerA >* loggerU

    val (logs, _) = logger.log("hello").run
    logs should contain theSameElementsInOrderAs Seq("hello", "const")
  }

  test("*< must retain the type of the left logger") {
    val loggerU = Logger.liftF(FunctorTell[LogF, Vector[String]].tell(Vector("const")))
    val loggerA = Loggers.pure[LogF, String]
    val logger = loggerU *< loggerA

    val (logs, _) = logger.log("hello").run
    logs should contain theSameElementsInOrderAs Seq("const", "hello")
  }

}
