package uk.ac.wellcome.models.transformable.sierra

import com.fasterxml.jackson.annotation.JsonProperty

// Examples of varFields from the Sierra JSON:
//
//    {
//      "fieldTag": "b",
//      "content": "X111658"
//    }
//
//    {
//      "fieldTag": "a",
//      "marcTag": "949",
//      "ind1": "0",
//      "ind2": "0",
//      "subfields": [
//        {
//          "tag": "1",
//          "content": "STAX"
//        },
//        {
//          "tag": "2",
//          "content": "sepam"
//        }
//      ]
//    }
//
case class MarcSubfield(
  tag: String,
  content: String
)

case class VarField(
  fieldTag: String,
  content: Option[String],
  marcTag: Option[String],
  @JsonProperty("ind1") indicator1: Option[String],
  @JsonProperty("ind2") indicator2: Option[String],
  subfields: Option[List[MarcSubfield]]
)

// Examples of fixedFields from the Sierra JSON:
//
//    "98": {
//      "label": "PDATE",
//      "value": "2017-12-22T12:55:57Z"
//    },
//    "77": {
//      "label": "TOT RENEW",
//      "value": 12
//    }
//
case class FixedField(
  label: String,
  value: String
)