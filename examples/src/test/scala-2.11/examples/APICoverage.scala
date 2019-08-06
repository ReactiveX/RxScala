/**
 * Copyright 2014 Netflix, Inc.
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
package examples

import scala.collection.mutable

import org.apache.bcel.Repository
import org.apache.bcel.classfile.Utility
import org.apache.bcel.util.ByteSequence
import org.junit.Assert._
import org.junit.Test
import org.scalatestplus.junit.JUnitSuite

class APICoverage extends JUnitSuite {

  val INVOKEVIRTUAL_PREFIX = "invokevirtual\t"
  val INVOKEINTERFACE_PREFIX = "invokeinterface\t"

  def searchInvokedMethodsInClass(className : String): (Set[String], Set[String]) = {
    val clazz = Repository.lookupClass(className)
    val virtualMethods = mutable.Set[String]()
    val interfaceMethods = mutable.Set[String]()
    for (method <- clazz.getMethods) {
      val code = method.getCode
      if (code != null) {
        val stream = new ByteSequence(code.getCode)
        while (stream.available() > 0) {
          val instruction = Utility.codeToString(stream, code.getConstantPool, false)
          if (instruction.startsWith(INVOKEVIRTUAL_PREFIX)) {
            // instruction is something like
            // invokevirtual<tab>rx.lang.scala.Observable$.interval<space>(Lscala/concurrent/duration/Duration;)Lrx/lang/scala/Observable;
            val invokedMethod = instruction.substring(INVOKEVIRTUAL_PREFIX.length).replaceAll(" ", "")
            virtualMethods += invokedMethod
          } else if (instruction.startsWith(INVOKEINTERFACE_PREFIX)) {
            // instruction is something like
            // invokeinterface<tab>rx.lang.scala.Observable.firstOrElse<space>(Lscala/Function0;)Lrx/lang/scala/Observable;2<tab>0
            val invokedMethod = instruction.substring(INVOKEINTERFACE_PREFIX.length, instruction.lastIndexOf('\t') - 1).replaceAll(" ", "")
            interfaceMethods += invokedMethod
          }
        }
      }
    }
    (virtualMethods.toSet, interfaceMethods.toSet)
  }

  def searchInvokedMethodsInPackage(classInThisPackage: Class[_]): (Set[String], Set[String]) = {
    val virtualMethods = mutable.Set[String]()
    val interfaceMethods = mutable.Set[String]()
    val packageName = classInThisPackage.getPackage.getName
    val packageDir = new java.io.File(classInThisPackage.getResource(".").toURI)
    for(file <- packageDir.listFiles if file.getName.endsWith(".class")) {
      val simpleClassName = file.getName.substring(0, file.getName.length - ".class".length)
      val className = packageName + "." + simpleClassName
      val (vMethods, iMethods) = searchInvokedMethodsInClass(className)
      virtualMethods ++= vMethods
      interfaceMethods ++= iMethods
    }
    (virtualMethods.toSet, interfaceMethods.toSet)
  }

  val nonPublicAPIs = Set(
    "<clinit>",
    "jObsOfListToScObsOfSeq",
    "jObsOfJObsToScObsOfScObs"
  )

  def getPublicMethods(className: String): Set[String] = {
    val clazz = Repository.lookupClass(className)
    clazz.getMethods.filter(method => method.isPublic && !nonPublicAPIs.contains(method.getName)).map {
      method => className + "." + method.getName + method.getSignature
    }.toSet
  }

  @Test
  def assertExampleCoverage(): Unit = {
    val objectMethods = getPublicMethods("rx.lang.scala.Observable$")
    val classMethods = getPublicMethods("rx.lang.scala.Observable")

    val (usedObjectMethods, usedClassMethods) = searchInvokedMethodsInPackage(getClass)

    val uncoveredObjectMethods = objectMethods.diff(usedObjectMethods)
    val uncoveredClassMethods = classMethods.diff(usedClassMethods)

    if (uncoveredObjectMethods.nonEmpty || uncoveredClassMethods.nonEmpty) {
      println("Please add examples for the following APIs")
      uncoveredObjectMethods.foreach(println)
      uncoveredClassMethods.foreach(println)
    }

    // TODO enable these assertions
    // assertTrue("Please add missing examples", uncoveredObjectMethods.isEmpty)
    // assertTrue("Please add missing examples", uncoveredClassMethods.isEmpty)
  }

}
