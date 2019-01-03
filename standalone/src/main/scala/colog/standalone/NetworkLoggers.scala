package colog.standalone

import java.net.SocketAddress
import java.nio.channels.{AsynchronousChannelGroup, AsynchronousSocketChannel}
import java.util.concurrent.ExecutorService

import cats.effect.{Async, Resource}
import cats.implicits._

import colog.Logger

object NetworkLoggers {

  def socket[F[_]](socket: SocketAddress, ec: ExecutorService)(implicit F: Async[F]): Resource[F, Logger[F, Array[Byte]]] = {
    def connectSocket(ch: AsynchronousSocketChannel): F[Unit] =
      F.async[Unit](cb => ch.connect(socket, null, toCompletionHandlerU(cb)))

    val channelGroupRes = Resource.make(
      F.delay(AsynchronousChannelGroup.withThreadPool(ec))
    )(ch => F.delay(ch.shutdownNow()))

    val channelRes = channelGroupRes.flatMap { group =>
      Resource.make(F.delay(AsynchronousSocketChannel.open(group)).flatTap(connectSocket))(ch => F.delay(ch.close()))
    }

    channelRes.map(IOLoggers.channel[F])
  }

}
