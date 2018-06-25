package uk.ac.wellcome.platform.idminter.steps

import com.google.inject.Inject
import com.twitter.inject.{Logging, TwitterModuleFlags}
import uk.ac.wellcome.models.work.internal.SourceIdentifier
import uk.ac.wellcome.platform.idminter.database.IdentifiersDao
import uk.ac.wellcome.platform.idminter.models.Identifier
import uk.ac.wellcome.platform.idminter.utils.Identifiable

import scala.util.Try

class IdentifierGenerator @Inject()(identifiersDao: IdentifiersDao)
    extends Logging
    with TwitterModuleFlags {

  def retrieveOrGenerateCanonicalId(
    identifier: SourceIdentifier
  ): Try[String] = {
    Try {
      identifiersDao
        .lookupId(
          sourceIdentifier = identifier
        )
        .flatMap {
          case Some(id) => Try(id.CanonicalId)
          case None     => generateAndSaveCanonicalId(identifier)
        }
    }.flatten
  }

  private def generateAndSaveCanonicalId(
    identifier: SourceIdentifier
  ): Try[String] = {

    val canonicalId = Identifiable.generate
    identifiersDao
      .saveIdentifier(
        Identifier(
          CanonicalId = canonicalId,
          OntologyType = identifier.ontologyType,
          SourceSystem = identifier.identifierType.id,
          SourceId = identifier.value
        ))
      .map { _ =>
        canonicalId
      }
  }
}
