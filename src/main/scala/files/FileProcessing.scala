package files

import java.io.FileNotFoundException

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

object FileProcessing extends App {

  val fileName = "fileopen.txt"

  read(fileName)

  private def read(res: String) = {
    for {
      line <- Source.fromInputStream(getClass.getClassLoader().getResourceAsStream(res)).getLines
    } println(line)
  }

  def readFileWithTry(resource: String): Try[List[String]] = {
    Try {
      Source.fromFile(resource).getLines.toList
    }
      .recover(throw new FileNotFoundException(s"{resource} not found"))
  }

  println("=========")
//  readFile(fileName)
  val file = readFileWithTry("/home/rlitvishko/Projects/edx/async/src/main/resources/fileopen.txt")
  file match {
    case Success(list) => list.foreach(println)
    case Failure(ex) => println(s"Failed, $ex")
  }

}
