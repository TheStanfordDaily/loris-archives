package uk.ac.wellcome.storage.vhs

import java.io.{ByteArrayInputStream, InputStream}

import com.gu.scanamo.Scanamo
import com.gu.scanamo.syntax._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.Id
import uk.ac.wellcome.storage.s3.{S3StreamStore, S3TypeStore}
import uk.ac.wellcome.storage.test.fixtures.LocalDynamoDb.Table
import uk.ac.wellcome.storage.test.fixtures.LocalVersionedHybridStore
import uk.ac.wellcome.storage.test.fixtures.S3.Bucket
import uk.ac.wellcome.test.fixtures._
import uk.ac.wellcome.test.utils.ExtendedPatience
import uk.ac.wellcome.utils.GlobalExecutionContext._
import uk.ac.wellcome.utils.JsonUtil._

import scala.util.Random

case class ExampleRecord(
  override val id: String,
  content: String
) extends Id

class VersionedHybridStoreTest
    extends FunSpec
    with Matchers
    with ScalaFutures
    with ExtendedPatience
    with LocalVersionedHybridStore {

  import uk.ac.wellcome.storage.dynamo._

  def withS3StreamStoreFixtures[R](
                                  testWith: TestWith[(Bucket, Table, VersionedHybridStore[InputStream, S3StreamStore]), R]
                                ): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withStreamVHS[R](bucket, table) { vhs =>
          testWith((bucket, table, vhs))
        }
      }
    }

  def withS3TypeStoreFixtures[R](
    testWith: TestWith[(Bucket, Table, VersionedHybridStore[ExampleRecord, S3TypeStore[ExampleRecord]]), R]
  ): R =
    withLocalS3Bucket[R] { bucket =>
      withLocalDynamoDbTable[R] { table =>
        withTypeVHS[ExampleRecord, R](bucket, table) { vhs =>
          testWith((bucket, table, vhs))
        }
      }
    }

  it("stores an InputStream") {
    withS3StreamStoreFixtures {
      case (bucket, table, hybridStore) =>

        val id = Random.nextString(5)
        val content = "A thousand thinking thanes thanking a therapod"
        val inputStream = new ByteArrayInputStream(content.getBytes)

        val future =  hybridStore.updateRecord(id)(inputStream)(identity)()

        whenReady(future) { _ =>
          getContentFor(bucket, table, id) shouldBe content
        }
    }
  }


  it("stores a versioned record if it has never been seen before") {
    withS3TypeStoreFixtures {
      case (bucket, table, hybridStore) =>
        val record = ExampleRecord(
          id = "1111",
          content = "One ocelot in orange"
        )

        val future =
          hybridStore.updateRecord(record.id)(record)(identity)()

        whenReady(future) { _ =>
          getJsonFor(bucket, table, record) shouldBe toJson(record).get
        }
    }
  }

  it("applies the given transformation to an existing record") {
    withS3TypeStoreFixtures {
      case (bucket, table, hybridStore) =>
        val record = ExampleRecord(
          id = "1111",
          content = "One ocelot in orange"
        )

        val expectedRecord = record.copy(content = "new content")

        val t = (e: ExampleRecord) => e.copy(content = "new content")

        val future =
          hybridStore
            .updateRecord(record.id)(record)(identity)()
            .flatMap(_ => hybridStore.updateRecord(record.id)(record)(t)())

        whenReady(future) { _ =>
          getJsonFor(bucket, table, record) shouldBe toJson(expectedRecord).get
        }
    }
  }

  it("updates DynamoDB and S3 if it sees a new version of a record") {
    withS3TypeStoreFixtures {
      case (bucket, table, hybridStore) =>
        val record = ExampleRecord(
          id = "2222",
          content = "Two teal turtles in Tenerife"
        )

        val updatedRecord = record.copy(
          content = "Throwing turquoise tangerines in Tanzania"
        )

        val future =
          hybridStore.updateRecord(record.id)(record)(identity)()

        val updatedFuture = future.flatMap { _ =>
          hybridStore.updateRecord(updatedRecord.id)(updatedRecord)(_ =>
            updatedRecord)()
        }

        whenReady(updatedFuture) { _ =>
          getJsonFor(bucket, table, updatedRecord) shouldBe toJson(
            updatedRecord).get
        }
    }
  }

  it("returns a future of None for a non-existent record") {
    withS3TypeStoreFixtures {
      case (_, _, hybridStore) =>
        val future = hybridStore.getRecord(id = "does/notexist")

        whenReady(future) { result =>
          result shouldBe None
        }
    }
  }

  it("returns a future of Some[ExampleRecord] if the record exists") {
    withS3TypeStoreFixtures {
      case (_, _, hybridStore) =>
        val record = ExampleRecord(
          id = "5555",
          content = "Five fishing flinging flint"
        )

        val putFuture =
          hybridStore.updateRecord(record.id)(record)(identity)()

        val getFuture = putFuture.flatMap { _ =>
          hybridStore.getRecord(record.id)
        }

        whenReady(getFuture) { result =>
          result shouldBe Some(record)
        }
    }
  }

  it("can store additional metadata alongside HybridRecord") {
    case class ExtraData(
      data: String,
      number: Int
    )

    val content = "this goes in dynamo"

    val record = ExampleRecord(
      id = "11111",
      content = content
    )

    val data = ExtraData(
      data = "a tragic triangle of triffids",
      number = 6
    )

    withS3TypeStoreFixtures {
      case (_, table, hybridStore) =>
        val future =
          hybridStore.updateRecord(record.id)(record)(identity)(data)

        whenReady(future) { _ =>
          val maybeResult =
            Scanamo.get[ExtraData](dynamoDbClient)(table.name)(
              'id -> record.id)

          maybeResult shouldBe defined
          maybeResult.get.isRight shouldBe true

          val extraData = maybeResult.get.right.get

          extraData.data shouldBe "a tragic triangle of triffids"
          extraData.number shouldBe 6
        }
    }
  }
}
