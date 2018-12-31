package colog

trait HasLog[F[_], E, M] {
  def getLogAction(env: E): LogAction[F, M]
  def setLogAction(action: LogAction[F, M], env: E): E
}

object HasLog {

  implicit def logActionHasLog[F[_], Msg]: HasLog[F, LogAction[F, Msg], Msg] = new HasLog[F, LogAction[F, Msg], Msg] {

    def getLogAction(env: LogAction[F, Msg]): LogAction[F, Msg] = env

    def setLogAction(action: LogAction[F, Msg], env: LogAction[F, Msg]): LogAction[F, Msg] = action

  }

}
