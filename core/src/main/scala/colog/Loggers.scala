/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import cats.Applicative
import cats.mtl.FunctorTell

object Loggers {

  def pure[F[_], A](implicit F: FunctorTell[F, Vector[A]]): Logger[F, A] = Logger { msg =>
    F.tell(Vector(msg))
  }

  def noop[F[_], A](implicit F: Applicative[F]): Logger[F, A] =
    Logger(_ => F.unit)

  def const[F[_], A](msg: String)(implicit F: Applicative[F]): Logger[F, String] => Logger[F, A] =
    base => Logger(_ => base.log(msg))

}
