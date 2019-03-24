/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.example.scalajs

import cats.Monad
import cats.effect._
import cats.implicits._

import colog._

import scala.concurrent.duration._

final class Module[F[_]: Timer, G[_]: Monad](implicit F: Sync[F], logging: StructuredLogging[F, Env[G]]) {

  def doSomething(): F[Unit] =
    for {
      _ <- logging.debug("Starting to do something")
      _ <- Timer[F].sleep(2.seconds)
      _ <- F.delay(println("Application working"))
      _ <- logging.info("Finished doing something")
    } yield ()

}
