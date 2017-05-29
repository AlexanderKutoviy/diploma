package com.excel.converter

import java.io.{FileInputStream, FileNotFoundException}
import java.net.URLDecoder

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import scalaz.{-\/, \/, \/-}

/**
  * Created by Alosha on 10/25/2016.
  */
object WorkbookFromFile {
  def apply(filepath : String) : String \/ Workbook = {
    val extenstion = filepath.split("\\.").last

    val decodedFilepath = URLDecoder.decode(filepath, "UTF-8")

    try {
      val inputStream = new FileInputStream(decodedFilepath)

      extenstion match {
        case "xls" =>
          \/-(new HSSFWorkbook(inputStream))
        case "xlsx" =>
          \/-(new XSSFWorkbook(inputStream))
        case _ => {
          -\/("Only xls and xlsx formats are supported")
        }
      }
    } catch {
      case e : Exception =>
        -\/("File Not Found: " + filepath)
    }
  }
}
