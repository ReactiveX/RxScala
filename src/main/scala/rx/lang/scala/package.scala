/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang

import _root_.scala.util.Try
import _root_.scala.concurrent.{ExecutionContext, Future}

/**
 * This package contains all classes that RxScala users need.
 *
 * It basically mirrors the structure of package `rx`, but some changes were made to make it more Scala-idiomatic.
 */
package object scala {

  /** Adds a [[toObservable]] extension method to [[_root_.scala.collection.Iterable Iterable]] */
  implicit class ObservableExtensions[T](val source: Iterable[T]) extends AnyVal {
    def toObservable: Observable[T] = Observable.from(source)
  }

  /** Adds a [[toObservable]] extension method to [[_root_.scala.util.Try Try]] */
  implicit class TryToObservable[T](val tryT: Try[T]) extends AnyVal {
    def toObservable: Observable[T] = Observable.from(tryT)
  }

  /** Adds a [[toObservable]] extension method to [[_root_.scala.Option Option]] */
  implicit class OptionToObservable[T](val opt: Option[T]) extends AnyVal {
    def toObservable: Observable[T] = Observable.from(opt)
  }

  /** Adds a [[toObservable]] extension method to [[_root_.scala.concurrent.Future Future]] */
  implicit class FutureToObservable[T](val future: Future[T]) extends AnyVal {
    def toObservable(implicit ec: ExecutionContext): Observable[T] = Observable.from(future)
  }
}
