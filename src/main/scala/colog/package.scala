import cats.Monad
import cats.data.Kleisli
import cats.mtl.ApplicativeAsk
import cats.implicits._

package object colog {

  def logMsg[F[_]: Monad, E, Msg](msg: Msg)(implicit ask: ApplicativeAsk[F, E], log: HasLog[F, E, Msg]): F[Unit] =
    for {
      logAction <- ask.reader(log.getLogAction)
      _         <- logAction.run(msg)
    } yield ()

}
