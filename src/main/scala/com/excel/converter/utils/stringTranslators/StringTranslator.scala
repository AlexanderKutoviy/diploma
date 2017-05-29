package com.excel.converter.utils.stringTranslators

import scalaz._

class StringTranslator(val predicate : String => Boolean, val transformer : String => String)

object Implicits {
  implicit def exactToExact( param : (String, String)) : StringTranslator = param match {
    case (src, dest) => new StringTranslator(src.equalsIgnoreCase, Function.const(dest))
  }

  implicit def poolItemToExact( param : (Seq[String], String)) : StringTranslator = param match {
    case (pool, dest) => new StringTranslator(s => pool.exists(s.equalsIgnoreCase), Function.const(dest))
  }
}

object StringTranslator {
  val id = new StringTranslator(Function.const(true), identity[String])
}