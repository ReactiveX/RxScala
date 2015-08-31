package rx.lang.scala.completeness

import java.util.Calendar

/**
 * Generate comparison tables for Scala classes and Java classes. Run `sbt 'test:run rx.lang.scala.completeness.CompletenessTest'` to generate them.
 */
object CompletenessTables {

  /**
   * CompletenessKits to generate completeness tables.
   */
  val completenessKits = List(
    new ObservableCompletenessKit,
    new BlockingObservableCompletenessKit,
    new TestSchedulerCompletenessKit,
    new TestSubscriberCompletenessKit)

  def setTodoForMissingMethods(completenessKit: CompletenessKit): Map[String, String] = {
    val actualMethods = completenessKit.rxScalaPublicInstanceAndCompanionMethods.toSet
    for ((javaM, scalaM) <- completenessKit.correspondence) yield
      (javaM, if (actualMethods.contains(scalaM) || scalaM.charAt(0) == '[') scalaM else "[**TODO: missing**]")
  }

  def scalaToJavaSignature(s: String) =
    s.replaceAllLiterally("_ <:", "? extends")
     .replaceAllLiterally("_ >:", "? super")
     .replaceAllLiterally("[", "<")
     .replaceAllLiterally("]", ">")
     .replaceAllLiterally("Array<T>", "T[]")

  def escapeJava(s: String) =
    s.replaceAllLiterally("<", "&lt;")
     .replaceAllLiterally(">", "&gt;")


  def printMarkdownCorrespondenceTables() {
    println("""
---
layout: comparison
title: Comparison of Scala Classes and Java Classes
---

Note:

*    These tables contain both static methods and instance methods.
*    If a signature is too long, move your mouse over it to get the full signature.
""")

    completenessKits.foreach(printMarkdownCorrespondenceTable)

    val completenessTablesClassName = getClass.getCanonicalName.dropRight(1) // Drop "$"
    println(s"\nThese tables were generated on ${Calendar.getInstance().getTime}.")
    println(s"**Do not edit**. Instead, edit `${completenessTablesClassName}` and run `sbt 'test:run ${completenessTablesClassName}'` to generate these tables.")
  }

  def printMarkdownCorrespondenceTable(completenessKit: CompletenessKit): Unit = {
    def groupingKey(p: (String, String)): (String, String) =
      (if (p._1.startsWith("average")) "average" else p._1.takeWhile(_ != '('), p._2)
    def formatJavaCol(name: String, alternatives: Iterable[String]): String = {
      alternatives.toList.sorted.map(scalaToJavaSignature).map(s => {
        if (s.length > 64) {
          val toolTip = escapeJava(s)
          "<span title=\"" + toolTip + "\"><code>" + name + "(...)</code></span>"
        } else {
          "`" + s + "`"
        }
      }).mkString("<br/>")
    }
    def formatScalaCol(s: String): String =
      if (s.startsWith("[") && s.endsWith("]")) s.drop(1).dropRight(1) else "`" + s + "`"

    val ps = setTodoForMissingMethods(completenessKit)

    println(s"""
               |## Comparison of Scala ${completenessKit.rxScalaType.typeSymbol.name} and Java ${completenessKit.rxJavaType.typeSymbol.name}
               |
               || Java Method | Scala Method |
               ||-------------|--------------|""".stripMargin)
    (for (((javaName, scalaCol), pairs) <- ps.groupBy(groupingKey).toList.sortBy(_._1._1)) yield {
      "| " + formatJavaCol(javaName, pairs.map(_._1)) + " | " + formatScalaCol(scalaCol) + " |"
    }).foreach(println(_))
  }

  def main(args: Array[String]): Unit = {
    printMarkdownCorrespondenceTables()
  }
}
