/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog
package syntax

import cats.mtl.ApplicativeAsk

trait HasLoggerSyntax {
  implicit final def cologHasLoggerSyntax[F[_], E, A](env: E)(
      implicit A: ApplicativeAsk[F, E],
      HL: HasLogger[F, E, A]
  ): HasLoggerOps[F, E, A] =
    new HasLoggerOps[F, E, A](env)
}

class HasLoggerOps[F[_], E, A](private val env: E)(implicit HL: HasLogger[F, E, A]) {
  def getLogger: Logger[F, A] = HL.getLogger(env)

  def setLogger(logger: Logger[F, A]): E = HL.setLogger(env)(logger)

  def withLogger(f: Logger[F, A] => Logger[F, A]): E = HL.withLogger(env)(f)
}

object haslogger extends HasLoggerSyntax
