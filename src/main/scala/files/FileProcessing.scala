package files

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.charset.Charset
import java.nio.file.{Path, Paths, StandardOpenOption}


import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.io.Source
import scala.util.{Failure, Success, Try}

object FileProcessing extends App {

  val fileName = "fileopen.txt"
  val fullFileName = "/home/rlitvishko/Projects/edx/async/src/main/resources/fileopen.txt"

  //  read(fileName)

  private def read(res: String) = {
    for {
      line <- Source.fromInputStream(getClass.getClassLoader().getResourceAsStream(res)).getLines
    } println(line)
  }

  def readFileWithTry(resource: String): Try[List[String]] = {
    Try {
      Source.fromFile(fullFileName).getLines.toList // file still open !!
    }
    //      .recover(throw new FileNotFoundException(s"${resource} not found"))
  }

  val filePath: Path = Paths.get(fullFileName)
  var fileChannel: AsynchronousFileChannel = AsynchronousFileChannel.open(filePath, StandardOpenOption.READ)

  def decode(x: ByteBuffer): String = Charset.defaultCharset().decode(x.flip().asInstanceOf[ByteBuffer]).toString

  def handler[A](f: Throwable Either A => Unit): CompletionHandler[Integer, A] =
    new CompletionHandler[Integer, A]() {
      override def completed(result: Integer, attachment: A): Unit = {
        if (result < 0)
          f(Left(new IOException("EOFile"))) else f(Right(attachment))
      }

      override def failed(throwable: Throwable, a: A): Unit = {
        f(Left(throwable))
      }
    }

  def readChunk(ch: AsynchronousFileChannel, pos: Long, b: ByteBuffer): Future[(Long, ByteBuffer)] = {
    val pr = Promise[(Long, ByteBuffer)]

    ch.read(b, pos, (pos, b), handler[(Long, ByteBuffer)](x => pr.complete(x.toTry)))

    pr.future
  }

  def readFile(ch: AsynchronousFileChannel, pos: Long, n: Int)(implicit excecutor: ExecutionContext): Future[Stream[ByteBuffer]] = {
    if (pos >= ch.size()) Future.successful(Stream.empty) else
      for {
        (x, y) <- readChunk(ch, pos, ByteBuffer.allocate(n))
        z <- readFile(ch, x + y.limit(), n)
      } yield y #:: z
  }

  import scala.concurrent.ExecutionContext.Implicits._

  val res = for {
    s <- readFile(fileChannel, 0, 10)
  } yield for (b <- s) println(decode(b))
  Await.result(res, Duration.Inf)

  var buffer: ByteBuffer = ByteBuffer.allocateDirect(1)

  //  import resource._
  //
  //  def using[A](r: Resource)(f: Resource => A): A =
  //    try {
  //      f(r)
  //    } finally {
  //      r.dispose()
  //    }

  println("=========")
  //  readFile(fileName)
  //  val file = readFileWithTry(fileName)
  //  file match {
  //    case Success(list) => list.foreach(println)
  //    case Failure(ex) => println(s"Failed, $ex")
  //  }
  println("exit")

}

