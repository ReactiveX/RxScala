import rx.lang.scala.Observable

object Hello {

  def hello(names: String*): Unit = {
    Observable.from(names) subscribe { n =>
      println(s"Hello $n!")
    }
  }
}
