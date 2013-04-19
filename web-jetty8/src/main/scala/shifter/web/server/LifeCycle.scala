package shifter.web.server

trait LifeCycle {
  def createContext: Context
  def destroyContext(ctx: Context)
}
