data "aws_iam_policy_document" "s3_read_miro_json" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    resources = [
      "${var.s3_miro_data_arn}/json/*",
      "${var.s3_miro_data_arn}/source/exceptions.csv",
      "${var.s3_miro_data_arn}/source/contrib.csv",
    ]
  }
}