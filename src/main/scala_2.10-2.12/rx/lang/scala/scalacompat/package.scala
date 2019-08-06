/*
 * File based on [scala-collection-compat library](https://github.com/scala/scala-collection-compat)
 * Integrated here for customization and to avoid a dependency
 *
 * Copyright (c) 2002-2019 EPFL
 * Copyright (c) 2011-2019 Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 */

package rx.lang.scala

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable

package object scalacompat {

  /**
    * A factory that builds a collection of type `C` with elements of type `A`.
    *
    * @tparam A Type of elements (e.g. `Int`, `Boolean`, etc.)
    * @tparam C Type of collection (e.g. `List[Int]`, `TreeMap[Int, String]`, etc.)
    */
  private[scala] type Factory[-A, +C] = CanBuildFrom[Nothing, A, C]

  private[scala] implicit class FactoryOps[-A, +C](val factory: Factory[A, C]) extends AnyVal {

    /** Get a Builder for the collection. For non-strict collection types this will use an intermediate buffer.
      * Building collections with `fromSpecific` is preferred because it can be lazy for lazy collections. */
    def newBuilder: mutable.Builder[A, C] = factory()
  }

}
