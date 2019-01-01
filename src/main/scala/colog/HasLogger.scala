package colog

trait HasLogger[F[_], E, M] {
  def getLogger(env: E): Logger[F, M]
  def setLogger(action: Logger[F, M], env: E): E
}

object HasLogger {

  def over[F[_], E, M](f: Logger[F, M] => Logger[F, M])(env: E)(implicit F: HasLogger[F, E, M]): E =
    F.setLogger(f(F.getLogger(env)), env)

}