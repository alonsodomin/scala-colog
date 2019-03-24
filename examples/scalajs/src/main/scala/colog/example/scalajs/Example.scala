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
import cats.mtl.implicits._
import cats.implicits._

import colog._
import colog.standalone._

object Example extends IOApp {

  type AppEff[A] = Kleisli[IO, Env[IO], A]

  final val env = Env[IO](
    SysLoggers
      .stdout[IO]
      .formatWithF(LogRecord.defaultFormat[IO])
      .timestampedWith(LogRecord.defaultTimestampedFormat)
  )

  implicit val logging = Logging.structured[AppEff, Env[IO]]

  def run(args: List[String]): IO[ExitCode] = {
    val mod = new Module[AppEff, IO]
    mod.doSomething().run(env).as(ExitCode.Success)
  }

}
