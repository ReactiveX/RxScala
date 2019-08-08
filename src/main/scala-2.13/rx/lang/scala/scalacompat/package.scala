package rx.lang.scala

package object scalacompat {
  private[scala] type Factory[-A, +C] = scala.collection.Factory[A, C]
  private[scala] final val Factory = scala.collection.Factory

  private[scala] final val CollectionConverters = scala.jdk.CollectionConverters
}
