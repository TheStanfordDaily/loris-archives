package uk.ac.wellcome.display.models

import org.scalatest.{FunSpec, Matchers}

class WorksIncludesTest extends FunSpec with Matchers {

  it("parses a present value as true") {
    val includes = WorksIncludes("identifiers")
    includes.identifiers shouldBe true
  }

  it("can be constructed if no query parameter is provided") {
    val includes = WorksIncludes(queryParam = None)
    includes.identifiers shouldBe false
  }

  it("rejects an incorrect string") {
    intercept[WorksIncludesParsingException] {
      WorksIncludes(queryParam = "foo,bar")
    }
  }
}
