package colog.standalone

import java.io.PrintStream
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousByteChannel, AsynchronousFileChannel}

import cats.effect.{Async, Sync}
import cats.implicits._
import cats.mtl.MonadState

import colog.Logger

object IOLoggers {

  def printStream[F[_]](printStream: PrintStream)(implicit F: Sync[F]): Logger[F, String] =
    Logger(msg => F.delay(printStream.println(msg)))

  def channel[F[_]](channel: AsynchronousByteChannel)(implicit F: Async[F]): Logger[F, Array[Byte]] =
    Logger[F, Array[Byte]] { buf =>
      F.void {
        F.async[Int](cb => channel.write(ByteBuffer.wrap(buf), null, toCompletionHandler[Integer](cb.compose(_.map(_.toInt)))))
      }
    }

  def fileChannel[F[_]](channel: AsynchronousFileChannel)(
    implicit F: Async[F], ST: MonadState[F, Long]
  ): Logger[F, Array[Byte]] = Logger[F, Array[Byte]] { buf =>
    def performWrite(pos: Long): F[Int] = F.async[Int] {
      cb => channel.write(ByteBuffer.wrap(buf), pos, null, toCompletionHandler[Integer](cb.compose(_.map(_.toInt))))
    }

    for {
      currPos      <- ST.get
      bytesWritten <- performWrite(currPos)
      _            <- ST.set(currPos + bytesWritten)
    } yield ()
  }

}
