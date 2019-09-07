/*
 * Copyright (c) 2018 A. Alonso Dominguez
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package colog.standalone

import java.io.PrintStream
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousByteChannel, AsynchronousFileChannel}
import java.nio.charset.{Charset, StandardCharsets}

import cats.effect.{Async, Sync}
import cats.implicits._
import cats.mtl.MonadState

import colog.Logger

object IOLoggers {

  // scalastyle:off
  def printStream[F[_]](printStream: PrintStream)(implicit F: Sync[F]): Logger[F, String] =
    Logger(msg => F.delay(printStream.println(msg)))
  // scalastyle:on

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def channel[F[_]](
      channel: AsynchronousByteChannel,
      charset: Charset = StandardCharsets.UTF_8
  )(implicit F: Async[F]): Logger[F, String] = {
    val byteBufferLogger = Logger[F, Array[Byte]] { buf =>
      F.void {
        F.async[Int](
          cb =>
            channel.write(
              ByteBuffer.wrap(buf),
              null,
              toCompletionHandler[Integer](cb.compose(_.map(_.toInt)))
            )
        )
      }
    }

    byteBufferLogger.contramap(encodeLines(charset))
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def fileChannel[F[_]](
      channel: AsynchronousFileChannel,
      charset: Charset = StandardCharsets.UTF_8
  )(
      implicit F: Async[F],
      ST: MonadState[F, Long]
  ): Logger[F, String] = {
    val byteBufferLogger = Logger[F, Array[Byte]] { buf =>
      def performWrite(pos: Long): F[Int] = F.async[Int] { cb =>
        channel.write(
          ByteBuffer.wrap(buf),
          pos,
          null,
          toCompletionHandler[Integer](cb.compose(_.map(_.toInt)))
        )
      }

      for {
        currPos      <- ST.get
        bytesWritten <- performWrite(currPos)
        _            <- ST.set(currPos + bytesWritten)
      } yield ()
    }

    byteBufferLogger.contramap(encodeLines(charset))
  }

}
