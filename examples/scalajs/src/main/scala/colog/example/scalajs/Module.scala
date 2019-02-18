/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.example.scalajs

import cats.effect.Sync
import cats.implicits._

import colog._

final class Module[F[_]](implicit F: Sync[F], logging: StructuredLogging[F, Env]) {

  def doSomething(): F[Unit] =
    for {
      _ <- logging.debug("Starting to do something")
      _ <- F.delay(println("Application working"))
      _ <- logging.info("Finished doing something")
    } yield ()

}
