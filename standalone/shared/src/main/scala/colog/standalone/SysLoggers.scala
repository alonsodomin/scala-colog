/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.standalone

import cats.effect.Sync
import colog.Logger

object SysLoggers {

  def stdout[F[_]](implicit F: Sync[F]): Logger[F, String] =
    Logger(str => F.delay(println(str)))

  def stderr[F[_]](implicit F: Sync[F]): Logger[F, String] =
    IOLoggers.printStream[F](System.err)

}
