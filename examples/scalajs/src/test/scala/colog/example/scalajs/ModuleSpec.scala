/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.example.scalajs

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import cats.mtl.implicits._

import colog._

import org.scalatest._

import scala.concurrent.ExecutionContext
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class ModuleSpec extends AsyncFlatSpec with Matchers {

  type LogIO[A] = MemLogT[IO, LogRecord, A]
  type AppIO[A] = Kleisli[LogIO, Env[LogIO], A]

  final val env = Env[LogIO](
    Loggers.pure[LogIO, LogRecord]
  )

  override implicit val executionContext = ExecutionContext.global
  implicit val timer = IO.timer(executionContext)
  implicit val logging = Logging.structured[AppIO, Env[LogIO]]

  "Module" should "emit logs" in {
    val module = new Module[AppIO, LogIO]

    val result = module.doSomething().run(env).run.unsafeToFuture()
    result.map { case (logs, _) =>
      logs.length shouldBe 2
    }
  }

}