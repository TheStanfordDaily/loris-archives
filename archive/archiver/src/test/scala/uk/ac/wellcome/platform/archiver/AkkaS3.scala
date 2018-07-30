package uk.ac.wellcome.platform.archiver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3Client
import uk.ac.wellcome.storage.fixtures.S3
import uk.ac.wellcome.test.fixtures.TestWith

trait AkkaS3 extends S3 {

  def withS3AkkaClient[R](
    actorSystem: ActorSystem,
    materializer: ActorMaterializer)(testWith: TestWith[S3Client, R]): R = {

    val s3AkkaClient = AkkaS3ClientModule.buildAkkaS3Client(
      region = "localhost",
      actorSystem = actorSystem,
      endpoint = localS3EndpointUrl,
      accessKey = accessKey,
      secretKey = secretKey
    )

    testWith(s3AkkaClient)
  }
}