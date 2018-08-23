package uk.ac.wellcome.platform.archive.archivist.fixtures

import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import com.google.inject.Guice
import uk.ac.wellcome.json.JsonUtil._
import uk.ac.wellcome.messaging.test.fixtures.Messaging
import uk.ac.wellcome.messaging.test.fixtures.SNS.Topic
import uk.ac.wellcome.messaging.test.fixtures.SQS.QueuePair
import uk.ac.wellcome.platform.archive.archivist.modules.{
  ConfigModule,
  TestAppConfigModule
}
import uk.ac.wellcome.platform.archive.archivist.{Archivist => ArchivistApp}
import uk.ac.wellcome.platform.archive.common.fixtures.{
  AkkaS3,
  BagIt,
  FileEntry
}
import uk.ac.wellcome.platform.archive.common.models.BagName
import uk.ac.wellcome.platform.archive.common.modules._
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.storage.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures.TestWith

trait Archivist extends AkkaS3 with Messaging with BagIt {

  def sendBag[R](bagName: BagName,
                 file: File,
                 ingestBucket: Bucket,
                 queuePair: QueuePair)(testWith: TestWith[BagName, R]) = {
    val uploadKey = s"upload/path/$bagName.zip"

    s3Client.putObject(ingestBucket.name, uploadKey, file)

    val uploadObjectLocation = ObjectLocation(ingestBucket.name, uploadKey)
    sendNotificationToSQS(queuePair.queue, uploadObjectLocation)

    testWith(bagName)
  }

  def sendFakeBag[R](ingestBucket: Bucket,
                     queuePair: QueuePair,
                     valid: Boolean = true)(testWith: TestWith[BagName, R]) = {

    withBag(12, valid) {
      case (bagName, _, file) =>
        val (zipFile, fileName) = createBagItZip(bagName, 12, valid)

        sendBag(bagName, file, ingestBucket, queuePair) { bag =>
          testWith(bag)
        }
    }
  }

  def withBag[R](dataFileCount: Int = 1, valid: Boolean = true)(
    testWith: TestWith[(BagName, ZipFile, File), R]) = {
    val bagName = BagName(randomAlphanumeric())

    info(s"Creating bag $bagName")

    val (zipFile, file) = createBagItZip(bagName, dataFileCount, valid)

    testWith((bagName, zipFile, file))

    file.delete()
  }

  def withApp[R](storageBucket: Bucket, queuePair: QueuePair, topicArn: Topic)(
    testWith: TestWith[ArchivistApp, R]) = {
    val archivist = new ArchivistApp {
      val injector = Guice.createInjector(
        new TestAppConfigModule(
          queuePair.queue.url,
          storageBucket.name,
          topicArn.arn),
        ConfigModule,
        AkkaModule,
        AkkaS3ClientModule,
        CloudWatchClientModule,
        SQSClientModule,
        SNSAsyncClientModule
      )
    }
    testWith(archivist)
  }

  def withArchivist[R](
    testWith: TestWith[(Bucket, Bucket, QueuePair, Topic, ArchivistApp), R]) = {
    withLocalSqsQueueAndDlqAndTimeout(15)(queuePair => {
      withLocalSnsTopic { snsTopic =>
        withLocalS3Bucket { ingestBucket =>
          withLocalS3Bucket { storageBucket =>
            withApp(storageBucket, queuePair, snsTopic) { archivist =>
              testWith(
                (ingestBucket, storageBucket, queuePair, snsTopic, archivist))
            }
          }
        }
      }
    })
  }

  def createZip(files: List[FileEntry]) = {
    val file = File.createTempFile("archivist-test", ".zip")
    val zipFileOutputStream = new FileOutputStream(file)
    val zipOutputStream = new ZipOutputStream(zipFileOutputStream)
    files.foreach {
      case FileEntry(name, contents) =>
        val zipEntry = new ZipEntry(name)
        zipOutputStream.putNextEntry(zipEntry)
        zipOutputStream.write(contents.getBytes)
        zipOutputStream.closeEntry()
    }
    zipOutputStream.close()
    val zipFile = new ZipFile(file)

    (zipFile, file)
  }

  def createBagItZip(bagName: BagName,
                     dataFileCount: Int = 1,
                     valid: Boolean = true) = {

    val allFiles = createBag(bagName, dataFileCount, valid)

    createZip(allFiles.toList)
  }
}