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
  def setLogger(env: E)(logger: Logger[F, M]): E

  def withLogger(env: E)(f: Logger[F, M] => Logger[F, M]): E =
    setLogger(env)(f(getLogger(env)))
}

object HasLogger {

  def apply[F[_], E, M](implicit ev: HasLogger[F, E, M]): HasLogger[F, E, M] = ev

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

    def setLogger(env: E)(logger: Logger[G, M]): E = env
  }

}
