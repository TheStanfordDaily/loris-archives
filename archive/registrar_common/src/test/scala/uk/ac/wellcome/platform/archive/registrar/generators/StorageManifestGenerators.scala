package uk.ac.wellcome.platform.archive.registrar.generators

import java.time.Instant

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{BagInfo, StorageSpace}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  StorageLocation,
  StorageProvider
}
import uk.ac.wellcome.platform.archive.registrar.common.models._
import uk.ac.wellcome.storage.ObjectLocation

trait StorageManifestGenerators extends RandomThings {
  def createStorageManifestWith(
    space: StorageSpace = randomStorageSpace,
    bagInfo: BagInfo = randomBagInfo,
    checksumAlgorithm: String = "sha256",
    providerId: String = "providerId",
    bucket: String = "bukkit",
    path: String = "path"
  ): StorageManifest =
    StorageManifest(
      space = space,
      info = bagInfo,
      manifest = FileManifest(
        checksumAlgorithm = ChecksumAlgorithm(checksumAlgorithm),
        files = Nil
      ),
      tagManifest = FileManifest(
        checksumAlgorithm = ChecksumAlgorithm(checksumAlgorithm),
        files = List(BagDigestFile(Checksum("a"), BagFilePath("bag-info.txt")))
      ),
      StorageLocation(
        StorageProvider(providerId),
        ObjectLocation(bucket, path)),
      Instant.now
    )

  def createStorageManifest: StorageManifest = createStorageManifestWith()
}