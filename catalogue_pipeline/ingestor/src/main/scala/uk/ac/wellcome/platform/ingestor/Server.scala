package uk.ac.wellcome.platform.ingestor

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import uk.ac.wellcome.finatra.modules.{ElasticClientModule, MessageConfigModule, MetricsSenderModule, S3ClientModule, S3ConfigModule, SQSClientModule, SQSConfigModule, _}
import modules.{AWSConfigModule, AkkaModule, _}

object ServerMain extends Server

class Server extends HttpServer {
  override val name = "uk.ac.wellcome.platform.ingestor Ingestor"
  override val modules = Seq(
    AWSConfigModule,
    MetricsSenderModule,
    SQSClientModule,
    MessageConfigModule,
    S3ConfigModule,
    S3ClientModule,
    AkkaModule,
    SQSReaderModule,
    IngestorWorkerModule,
    ElasticClientModule,
    WorksIndexModule,
    IdentifiedWorkKeyPrefixGeneratorModule
  )
  flag[String]("es.index.v1", "V1 ES index name")
  flag[String]("es.index.v2", "V2 ES index name")
  flag[String]("es.type", "item", "ES document type")
  override def configureHttp(router: HttpRouter) {
    router
      .filter[CommonFilters]
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .add[ManagementController]
  }
}
