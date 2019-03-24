/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.example.scalajs

import colog.{HasLogger, LogRecord, Logger}

case class Env[F[_]](logger: Logger[F, LogRecord])
object Env {

  implicit def envHasLogger[F[_]]: HasLogger[F, Env[F], LogRecord] =
    new HasLogger[F, Env[F], LogRecord] {
      override def getLogger(env: Env[F]): Logger[F, LogRecord] = env.logger

      override def setLogger(env: Env[F])(newLogger: Logger[F, LogRecord]): Env[F] =
        env.copy(logger = newLogger)
    }

}
