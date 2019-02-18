/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats.Monad
import cats.implicits._
import cats.mtl.ApplicativeAsk
import cats.mtl.lifting.MonadLayer

trait HasLogger[F[_], E, M] {
  def getLogger(env: E): Logger[F, M]
  def setLogger(logger: Logger[F, M], env: E): E
}

object HasLogger {

  def apply[F[_], E, M](implicit ev: HasLogger[F, E, M]): HasLogger[F, E, M] = ev

  def over[F[_], E, M](f: Logger[F, M] => Logger[F, M])(env: E)(implicit F: HasLogger[F, E, M]): E =
    F.setLogger(f(F.getLogger(env)), env)

  implicit def hasLoggerAutoDerive[G[_], F[_], E, M](
    implicit
    G: Monad[G],
    L: MonadLayer[G, F],
    A: ApplicativeAsk[G, E],
    HL: HasLogger[F, E, M]
  ): HasLogger[G, E, M] = new HasLogger[G, E, M] {

    def getLogger(env: E): Logger[G, M] = Logger { msg =>
      for {
        logger <- A.reader(HL.getLogger)
        _      <- L.layer(logger.log(msg))
      } yield ()
    }

    def setLogger(logger: Logger[G, M], env: E): E = env
  }

}