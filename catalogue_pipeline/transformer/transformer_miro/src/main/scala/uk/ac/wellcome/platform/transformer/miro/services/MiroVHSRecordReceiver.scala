package uk.ac.wellcome.platform.transformer.miro.services

import grizzled.slf4j.Logging
import io.circe.ParsingFailure
import uk.ac.wellcome.json.JsonUtil.fromJson
import uk.ac.wellcome.messaging.message.MessageWriter
import uk.ac.wellcome.messaging.sns.{NotificationMessage, PublishAttempt}
import uk.ac.wellcome.models.work.internal.TransformedBaseWork
import uk.ac.wellcome.platform.transformer.exceptions.TransformerException
import uk.ac.wellcome.platform.transformer.miro.models.MiroMetadata
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.vhs.HybridRecord

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MiroVHSRecordReceiver(
  objectStore: ObjectStore[MiroRecord],
  messageWriter: MessageWriter[TransformedBaseWork])(implicit ec: ExecutionContext)
  extends Logging {

  def receiveMessage(
    message: NotificationMessage,
    transformToWork: (MiroRecord, MiroMetadata, Int) => Try[TransformedBaseWork]): Future[Unit] = {
    debug(s"Starting to process message $message")

    val futurePublishAttempt = for {
      hybridRecord <- Future.fromTry(fromJson[HybridRecord](message.body))
      miroMetadata <- Future.fromTry(fromJson[MiroMetadata](message.body))
      transformableRecord <- getTransformable(hybridRecord)
      work <- Future.fromTry(
        transformToWork(transformableRecord, miroMetadata, hybridRecord.version)
      )
      publishResult <- publishMessage(work)
      _ = debug(
        s"Published work: ${work.sourceIdentifier} with message $publishResult")
    } yield publishResult

    futurePublishAttempt
      .recover {
        case e: ParsingFailure =>
          info("Recoverable failure parsing HybridRecord from message", e)
          throw TransformerException(e)
      }
      .map(_ => ())

  }

  private def getTransformable(hybridRecord: HybridRecord): Future[MiroRecord] =
    objectStore.get(hybridRecord.location)

  private def publishMessage(work: TransformedBaseWork): Future[PublishAttempt] =
    messageWriter.write(
      message = work,
      subject = s"source: ${this.getClass.getSimpleName}.publishMessage"
    )
}
