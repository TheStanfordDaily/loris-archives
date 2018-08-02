package uk.ac.wellcome.platform.archiver.flow

import java.nio.file.FileSystems
import java.util.UUID.randomUUID
import java.util.zip.ZipFile

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import akka.stream.scaladsl.{FileIO, Flow, Source}
import akka.util.ByteString
import uk.ac.wellcome.storage.ObjectLocation

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


object DownloadZipFlow {
  val tmpDir = System.getProperty("java.io.tmpdir")

  def apply(s3Client: S3Client, materializer: ActorMaterializer, executionContext: ExecutionContext): Flow[ObjectLocation, ZipFile, NotUsed] = {
    implicit val m = materializer
    implicit val e = executionContext

    Flow[ObjectLocation].flatMapConcat(objectLocation => {
      val downloadStream: Source[ByteString, NotUsed] = s3Client.download(objectLocation.namespace, objectLocation.key)._1

      // TODO: Use File.createTempFile
      val fileName = randomUUID().toString
      val path = FileSystems.getDefault.getPath(tmpDir, fileName)

      val fileSink = FileIO
        .toPath(FileSystems.getDefault.getPath(tmpDir, fileName))

      Source.fromFuture(downloadStream
        .runWith(fileSink)
        .map(_.status)
        .map {
          case Success(_) => new ZipFile(path.toFile)
          case Failure(e) => throw e
        }
      )
    })
  }
}

