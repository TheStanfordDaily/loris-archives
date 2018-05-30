package uk.ac.wellcome.display.models.v2

import org.scalatest.{FunSpec, Matchers}
import uk.ac.wellcome.models.work.internal.License_CCBY

class DisplayLicenseV2Test extends FunSpec with Matchers {

  it("should read a License as a DisplayLicenseV2 correctly") {
    val displayLicense = DisplayLicenseV2(License_CCBY)

    displayLicense.licenseType shouldBe License_CCBY.id
    displayLicense.label shouldBe License_CCBY.label
    displayLicense.url shouldBe License_CCBY.url
    displayLicense.ontologyType shouldBe "License"
  }
}
