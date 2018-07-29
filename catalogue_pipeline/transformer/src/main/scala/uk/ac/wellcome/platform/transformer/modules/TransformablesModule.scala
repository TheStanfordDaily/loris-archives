package uk.ac.wellcome.platform.transformer.modules

import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import io.circe.{KeyDecoder, KeyEncoder}
import uk.ac.wellcome.models.transformable.sierra.SierraItemNumber
import uk.ac.wellcome.models.transformable.{MiroTransformable, SierraTransformable}
import uk.ac.wellcome.storage.ObjectStore
import uk.ac.wellcome.storage.s3.S3StorageBackend
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext

object TransformablesModule extends TwitterModule {
  @Provides
  @Singleton
  def provideMiroTransformableObjectStore(
    injector: Injector): ObjectStore[MiroTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[MiroTransformable]
  }

  implicit val keyDecoder: KeyDecoder[SierraItemNumber] = SierraTransformable.keyDecoder
  implicit val keyEncoder: KeyEncoder[SierraItemNumber] = SierraTransformable.keyEncoder

  @Provides
  @Singleton
  def provideSierraTransformableObjectStore(
    injector: Injector): ObjectStore[SierraTransformable] = {
    implicit val storageBackend = injector.instance[S3StorageBackend]
    implicit val executionContext = injector.instance[ExecutionContext]

    ObjectStore[SierraTransformable]
  }
}
