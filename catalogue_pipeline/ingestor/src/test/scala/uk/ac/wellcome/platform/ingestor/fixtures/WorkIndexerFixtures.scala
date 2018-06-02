package uk.ac.wellcome.platform.ingestor.fixtures

import com.sksamuel.elastic4s.http.HttpClient
import org.scalatest.Suite
import uk.ac.wellcome.monitoring.MetricsSender
import uk.ac.wellcome.monitoring.test.fixtures.MetricsSenderFixture
import uk.ac.wellcome.platform.ingestor.services.WorkIndexer
import uk.ac.wellcome.test.fixtures._

trait WorkIndexerFixtures extends Akka with MetricsSenderFixture {
  this: Suite =>
  def withWorkIndexer[R](
    esType: String,
    elasticClient: HttpClient,
    metricsSender: MetricsSender)(testWith: TestWith[WorkIndexer, R]): R = {
    val workIndexer = new WorkIndexer(
      elasticClient = elasticClient,
      metricsSender = metricsSender
    )

    testWith(workIndexer)
  }

  def withWorkIndexerFixtures[R](esType: String, elasticClient: HttpClient)(
    testWith: TestWith[WorkIndexer, R]): R = {
    withActorSystem { actorSystem =>
      withMetricsSender(actorSystem) { metricsSender =>
        withWorkIndexer(
          elasticClient = elasticClient,
          metricsSender = metricsSender) { workIndexer =>
          testWith(workIndexer)
        }
      }
    }
  }
}
