package shifter.web.jetty8

trait LifeCycle {
  def createContext: Context
  def destroyContext(ctx: Context)
}
