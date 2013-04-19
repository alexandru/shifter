package shifter.web.jetty9

trait LifeCycle {
  def createContext: Context
  def destroyContext(ctx: Context)
}
