package colog

trait HasLog[F[_], E, M] {
  def getLogAction(env: E): LogAction[F, M]
  def setLogAction(action: LogAction[F, M], env: E): E
}

object HasLog {

  def over[F[_], E, M](f: LogAction[F, M] => LogAction[F, M])(env: E)(implicit F: HasLog[F, E, M]): E =
    F.setLogAction(f(F.getLogAction(env)), env)

}