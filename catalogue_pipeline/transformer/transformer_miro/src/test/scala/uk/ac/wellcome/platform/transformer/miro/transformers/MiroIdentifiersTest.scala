package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

class MiroIdentifiersTest
    extends FunSpec
    with Matchers
    with IdentifiersGenerators {

  it("fixes the malformed INNOPAC ID on L0035411") {
    val miroRecord = MiroRecord(
      innopacID = Some("L 35411 \n\n15551040")
    )

    val otherIdentifiers =
      transformer.getOtherIdentifiers(miroRecord = miroRecord, miroId = "L0035411")

    otherIdentifiers shouldBe List(
      createSierraSystemSourceIdentifierWith(
        value = "b15551040"
      )
    )
  }

  val transformer = new MiroIdentifiers {}
}
