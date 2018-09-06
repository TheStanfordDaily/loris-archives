package uk.ac.wellcome.messaging.message

import java.text.SimpleDateFormat
import java.util.Date

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.sns.AmazonSNS
import com.google.inject.Inject
import grizzled.slf4j.Logging
import io.circe.Encoder
import uk.ac.wellcome.messaging.sns.{PublishAttempt, SNSConfig, SNSWriter}
import uk.ac.wellcome.storage.s3.S3Config
import uk.ac.wellcome.storage.{KeyPrefix, ObjectStore}
import uk.ac.wellcome.json.JsonUtil._

import scala.concurrent.{ExecutionContext, Future}

case class MessageWriterConfig(
  snsConfig: SNSConfig,
  s3Config: S3Config
)

class MessageWriter[T] @Inject()(
  messageConfig: MessageWriterConfig,
  snsClient: AmazonSNS,
  s3Client: AmazonS3
)(implicit objectStore: ObjectStore[String], ec: ExecutionContext)
    extends Logging {

  private val sns = new SNSWriter(
    snsClient = snsClient,
    snsConfig = messageConfig.snsConfig
  )

  private val dateFormat = new SimpleDateFormat("YYYY/MM/dd")

  private def getKeyPrefix(): String = {
    val topicName = messageConfig.snsConfig.topicArn.split(":").last
    val currentTime = new Date()
    s"$topicName/${dateFormat.format(currentTime)}/${currentTime.getTime.toString}"
  }

  def write(message: T, subject: String)(implicit encoder: Encoder[T], notificationEncoder: Encoder[MessageNotification[T]]): Future[PublishAttempt] =
    for {
      encodedString <- Future.fromTry(toJson(message))

      // If the encoded message is less than 250KB, we can send it inline
      // in SNS/SQS (although the limit is 256KB, there's a bit of overhead
      // caused by the notification wrapper, so we're conservative).
      //
      // Max SQS message size:
      // https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-limits.html#limits-messages
      //
      // Max SNS message size:
      // https://aws.amazon.com/sns/faqs/
      //
      notification: MessageNotification[T] <- if (encodedString.length > 250 * 1000) {
        writeToS3[T](encodedString)
      } else {
        Future.successful(InlineNotification(message))
      }

      publishAttempt <- sns.writeMessage(
        message = notification,
        subject = subject
      )
      _ = debug(publishAttempt)
    } yield publishAttempt

  private def writeToS3[T](encodedString: String): Future[RemoteNotification[T]] =
    for {
      location <- objectStore.put(messageConfig.s3Config.bucketName)(
        encodedString,
        keyPrefix = KeyPrefix(getKeyPrefix())
      )
      _ = debug(s"Successfully stored message <<$encodedString>> in location: $location")
    } yield RemoteNotification(location = location)
}
