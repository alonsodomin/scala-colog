/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog

import java.nio.channels.CompletionHandler
import java.nio.charset.Charset

package object standalone {

  private[colog] def toCompletionHandler[A](
      cb: Either[Throwable, A] => Unit
  ): CompletionHandler[A, AnyRef] =
    new CompletionHandler[A, AnyRef] {
      override def completed(result: A, attachment: AnyRef): Unit   = cb(Right(result))
      override def failed(exc: Throwable, attachment: AnyRef): Unit = cb(Left(exc))
    }

  private[colog] def toCompletionHandlerU[A](
      cb: Either[Throwable, Unit] => Unit
  ): CompletionHandler[Void, AnyRef] =
    toCompletionHandler(cb.compose(_.map(_ => ())))

  private[colog] def encodeLines(charset: Charset): String => Array[Byte] = str => {
    val lineSeparator = sys.props.get("line.separator").filter(_.nonEmpty).getOrElse("\n")
    (str + lineSeparator).getBytes(charset)
  }

}
