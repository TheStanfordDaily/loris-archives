package uk.ac.wellcome.platform.archive.common.models

import java.net.{URI, URISyntaxException}
import java.util.UUID
import uk.ac.wellcome.json.JsonUtil._

case class ArchiveCompleteNotification(
                                           archiveRequestId: UUID,
                                           bagLocation: BagLocation,
                                           archiveCompleteCallbackUrl: Option[URI] = None)

object ArchiveCompleteNotification {

  import io.circe._
  import io.circe.generic.semiauto._

  implicit val uriEncoder: Encoder[URI] =
    Encoder.encodeString.contramap[URI](_.toString)
  implicit val uriDecoder: Decoder[URI] = Decoder.instance { cursor =>
    cursor.as[String] match {
      case Right(str) =>
        try Right(new URI(str))
        catch {
          case _: URISyntaxException =>
            Left(DecodingFailure("URI", cursor.history))
        }
      case l@Left(_) => l.asInstanceOf[Decoder.Result[URI]]
    }
  }

  implicit val bagArchiveCompleteNotificationDecoder
  : Decoder[ArchiveCompleteNotification] = deriveDecoder
  implicit val bagArchiveCompleteNotificationEncoder
  : Encoder[ArchiveCompleteNotification] = deriveEncoder

  def apply(
             bagLocation: BagLocation,
             context: IngestRequestContext
           ): ArchiveCompleteNotification =
    ArchiveCompleteNotification(
      context.id,
      bagLocation,
      context.callbackUrl
    )

}
