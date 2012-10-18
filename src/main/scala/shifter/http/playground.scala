package shifter.http.playground

object Session {
  case class Get(val format: String) { 
    def unapplySeq(request: String) = { 
      if (format.replace("{id}", "12") == request)
	Some(Seq("12"))
      else
	None
    } 
  }

  def doit() {
    val GetPerson = Get("/api/person/{id}/")
    
    println("/api/person/" match {
      case req @ GetPerson(id) => req + " matches: " + id
      case x => x + " does not match"
    })

    println("/api/person/12/" match {
      case req @ GetPerson(id) => req + " matches: " + id
      case x => x + " does not match"
    })
  }
}
