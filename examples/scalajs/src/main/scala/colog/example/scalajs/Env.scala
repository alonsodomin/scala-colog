/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.example.scalajs

import cats.effect.IO
import colog.{HasLogger, LogRecord, Logger}

case class Env(logger: Logger[IO, LogRecord])
object Env {

  implicit val envHasLogger: HasLogger[IO, Env, LogRecord] = new HasLogger[IO, Env, LogRecord] {
    override def getLogger(env: Env): Logger[IO, LogRecord] = env.logger

    override def setLogger(newLogger: Logger[IO, LogRecord], env: Env): Env =
      env.copy(logger = newLogger)
  }

}
