/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.scala.completeness

import org.junit.Test
import org.scalatest.junit.JUnitSuite

import scala.collection.immutable.SortedMap
import scala.reflect.runtime.universe.{Symbol, Type, typeOf}

/**
 * If adding a new [[CompletenessKit]], please also update [[CompletenessTables.completenessKits]] to generate its comparison table.
 */
trait CompletenessKit extends JUnitSuite {

  /**
   * Return the type of the Java class to check
   */
  def rxJavaType: Type

  /**
   * Return the type of the Scala class to check
   */
  def rxScalaType: Type

  /**
   * Whether we should omit parentheses for methods of arity-0 when comparing Java methods and Scala methods
   */
  def isOmittingParenthesesForArity0Method: Boolean = true

  /**
   * Manually added mappings from Java methods to Scala methods. Sometimes, it's hard to map some Java methods to Scala methods
   * automatically. Use this one to create the mappings manually.
   */
  protected def correspondenceChanges: Map[String, String]

  /**
   * Return all public Java instance and static methods
   */
  final def rxJavaPublicInstanceAndCompanionMethods: Iterable[String] = getPublicInstanceAndCompanionMethods(rxJavaType)


  /**
   * Return all public Scala methods and companion methods.
   */
  final def rxScalaPublicInstanceAndCompanionMethods: Iterable[String] = getPublicInstanceAndCompanionMethods(rxScalaType)

  /**
   * Maps each method from the Java class to its corresponding method in the Scala class
   */
  final def correspondence = defaultMethodCorrespondence ++ correspondenceChanges // ++ overrides LHS with RHS

  /**
   * Creates default method correspondence mappings, assuming that Scala methods have the same
   * name and the same argument types as in Java
   */
  private def defaultMethodCorrespondence: Map[String, String] = {
    val allMethods = getPublicInstanceAndCompanionMethods(rxJavaType)
    val tuples = for (javaM <- allMethods) yield (javaM, javaMethodSignatureToScala(javaM))
    tuples.toMap
  }

  private def removePackage(s: String) = s.replaceAll("(\\w+\\.)+(\\w+)", "$2")

  private def methodMembersToMethodStrings(members: Iterable[Symbol]): Iterable[String] = {
    for (member <- members; alt <- member.asTerm.alternatives) yield {
      val m = alt.asMethod
      // multiple parameter lists in case of curried functions
      val paramListStrs = for (paramList <- m.paramss) yield {
        paramList.map(
          symb => removePackage(symb.typeSignature.toString.replaceAll(",(\\S)", ", $1"))
        ).mkString("(", ", ", ")")
      }
      val name = alt.asMethod.name.decodedName.toString
      name + paramListStrs.mkString("")
    }
  }

  private def getPublicInstanceMethods(tp: Type): Iterable[String] = {
    val ignoredSuperTypes = Set(typeOf[AnyRef], typeOf[Any], typeOf[AnyVal], typeOf[Object])
    val superTypes = tp.baseClasses.map(_.asType.toType).filter(!ignoredSuperTypes(_))
    // declarations: => only those declared in
    // members => also those of superclasses
    methodMembersToMethodStrings(superTypes.flatMap(_.declarations).filter {
      m =>
        m.isMethod && m.isPublic &&
          m.annotations.forall(_.toString != "java.lang.Deprecated") // don't check deprecated classes
    })
      // TODO how can we filter out instance methods which were put into companion because
      // of extends AnyVal in a way which does not depend on implementation-chosen name '$extension'?
      .filter(! _.contains("$extension"))
      // `access$000` is public. How to distinguish it from others without hard-code?
      .filter(! _.contains("access$000"))
      // Ignore constructors
      .filter(! _.startsWith("<init>"))
  }

  /**
   * Return all public instance methods and companion methods of a type. Also applicable for Java types.
   */
  private def getPublicInstanceAndCompanionMethods(tp: Type): Iterable[String] =
    getPublicInstanceMethods(tp) ++
      getPublicInstanceMethods(tp.typeSymbol.companionSymbol.typeSignature)

  private def javaMethodSignatureToScala(s: String): String = {
    val r = s.replaceAllLiterally("Long, Long, TimeUnit", "Duration, Duration")
      .replaceAllLiterally("Long, TimeUnit", "Duration")
      .replaceAll("Action0", "() => Unit")
      // nested [] can't be parsed with regex, so these will have to be added manually
      .replaceAll("Action1\\[([^]]*)\\]", "$1 => Unit")
      .replaceAll("Action2\\[([^]]*), ([^]]*)\\]", "($1, $2) => Unit")
      .replaceAll("Func0\\[([^]]*)\\]", "() => $1")
      .replaceAll("Func1\\[([^]]*), ([^]]*)\\]", "$1 => $2")
      .replaceAll("Func2\\[([^]]*), ([^]]*), ([^]]*)\\]", "($1, $2) => $3")
      .replaceAllLiterally("_ <: ", "")
      .replaceAllLiterally("_ >: ", "")
      .replaceAllLiterally("<repeated...>[T]", "T*")

    if (isOmittingParenthesesForArity0Method) {
      r.replaceAll("(\\w+)\\(\\)", "$1")
    } else {
      r
    }
  }

  @Test
  def checkScalaMethodPresenceVerbose(): Unit = {
    println("\nTesting that all mentioned Scala methods exist")
    println(  "----------------------------------------------\n")

    val actualMethods = rxScalaPublicInstanceAndCompanionMethods.toSet
    var good = 0
    var bad = 0
    for ((javaM, scalaM) <- SortedMap(correspondence.toSeq :_*)) {
      if (actualMethods.contains(scalaM) || scalaM.charAt(0) == '[') {
        good += 1
      } else {
        bad += 1
        println(s"Warning:")
        println(s"$scalaM is NOT present in Scala ${rxScalaType}")
        println(s"$javaM is the method in Java ${rxJavaType} generating this warning")
      }
    }

    checkMethodPresenceStatus(good, bad, rxScalaType)
  }

  def checkMethodPresenceStatus(goodCount: Int, badCount: Int, instance: Any): Unit = {
    if (badCount == 0) {
      println(s"SUCCESS: $goodCount out of ${badCount+goodCount} methods were found in $instance")
    } else {
      fail(s"FAILURE: Only $goodCount out of ${badCount+goodCount} methods were found in $instance")
    }
  }

  @Test
  def checkJavaMethodPresence(): Unit = {
    println("\nTesting that all mentioned Java methods exist")
    println("---------------------------------------------\n")
    checkMethodPresence(correspondence.keys, rxJavaPublicInstanceAndCompanionMethods, rxJavaType)
  }

  def checkMethodPresence(expectedMethods: Iterable[String], actualMethods: Iterable[String], tp: Type): Unit = {
    val actualMethodsSet = actualMethods.toSet
    val expMethodsSorted = expectedMethods.toList.sorted
    var good = 0
    var bad = 0
    for (m <- expMethodsSorted) if (actualMethodsSet.contains(m) || m.charAt(0) == '[') {
      good += 1
    } else {
      bad += 1
      println(s"Warning: $m is NOT present in $tp")
    }

    checkMethodPresenceStatus(good, bad, tp)
  }

}
