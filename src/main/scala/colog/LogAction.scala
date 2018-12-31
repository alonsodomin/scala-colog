package colog

import cats._
import cats.effect.IO
import cats.implicits._
import cats.mtl.ApplicativeAsk
import cats.mtl.lifting.ApplicativeLayer

case class LogAction[F[_], Msg](run: Msg => F[Unit]) { self =>

  def <&(msg: Msg): F[Unit] = run(msg)

  def mapK[G[_]](f: F ~> G): LogAction[G, Msg] =
    LogAction(msg => f.apply(self.run(msg)))

  /*def lift[G[_[_], _]](implicit G: Applicative[G[F, ?]]): LogAction[G[F, ?], Msg] =
    LogAction(msg => G.pure(self.run(msg)))*/

  def lift[G[_]](implicit G: ApplicativeLayer[G, F]): LogAction[G, Msg] =
    LogAction(msg => G.layer(self.run(msg)))

}

object LogAction {

  def stdout: LogAction[IO, String] = LogAction(str => IO(println(str)))

}