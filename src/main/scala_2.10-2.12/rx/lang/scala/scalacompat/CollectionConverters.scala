package rx.lang.scala.scalacompat

import scala.collection.convert.{DecorateAsJava, DecorateAsScala}

private[scala] object CollectionConverters extends DecorateAsJava with DecorateAsScala
