package shifter.web.server

class DefaultLifeCycle extends LifeCycle {
  lazy val config = Configuration.load()

  def createContext = Context()

  def destroyContext(ctx: Context) {}
}
