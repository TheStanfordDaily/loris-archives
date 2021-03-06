module "monitoring-271118" {
  source = "stack"

  namespace = "monitoring-271118"

  monitoring_bucket = "${aws_s3_bucket.monitoring.bucket}"
  account_id        = "${data.aws_caller_identity.current.account_id}"

  namespace_id = "${aws_service_discovery_private_dns_namespace.namespace.id}"
  vpc_id       = "${local.vpc_id}"

  efs_id                = "${module.grafana_efs.efs_id}"
  efs_security_group_id = "${aws_security_group.efs_security_group.id}"

  domain = "monitoring.wellcomecollection.org"

  public_subnets  = "${local.public_subnets}"
  private_subnets = "${local.private_subnets}"

  infra_bucket       = "${local.infra_bucket}"
  key_name           = "${local.key_name}"
  aws_region         = "${var.aws_region}"
  admin_cidr_ingress = "${local.admin_cidr_ingress}"

  lambda_error_alarm_arn = "${local.lambda_error_alarm_arn}"

  every_day_at_8am_rule_name = "${aws_cloudwatch_event_rule.every_day_at_8am.name}"
  every_minute_rule_arn      = "${aws_cloudwatch_event_rule.every_minute.arn}"
  every_minute_rule_name     = "${aws_cloudwatch_event_rule.every_minute.name}"

  # slack_budget_bot

  slack_budget_bot_container_uri = "${local.slack_budget_bot_container_uri}"

  # grafana

  grafana_admin_user        = "${local.grafana_admin_user}"
  grafana_anonymous_role    = "${local.grafana_anonymous_role}"
  grafana_admin_password    = "${local.grafana_admin_password}"
  grafana_anonymous_enabled = "${local.grafana_anonymous_enabled}"

  # update_service_list

  dashboard_bucket = "${aws_s3_bucket.dashboard.bucket}"
  dashboard_assumable_roles = [
    // Experience
    "arn:aws:iam::130871440101:role/platform-team-assume-role",

    // Workflow
    "arn:aws:iam::299497370133:role/monitoring",

    // Storage
    "arn:aws:iam::975596993436:role/monitoring",

    // Platform
    "arn:aws:iam::760097843905:role/monitoring",
  ]

  # post_to_slack

  dlq_alarm_arn                  = "${local.dlq_alarm_arn}"
  gateway_server_error_alarm_arn = "${local.gateway_server_error_alarm_arn}"
  cloudfront_errors_topic_arn    = "${local.cloudfront_errors_topic_arn}"
  critical_slack_webhook         = "${local.critical_slack_webhook}"
  non_critical_slack_webhook     = "${local.noncritical_slack_webhook}"
  bitly_access_token             = "${local.bitly_access_token}"

  # terraform_tracker

  terraform_apply_topic_name = "${local.terraform_apply_topic_name}"

  # IAM

  allow_cloudwatch_read_metrics_policy_json = "${data.aws_iam_policy_document.allow_cloudwatch_read_metrics.json}"
  describe_services_policy_json             = "${data.aws_iam_policy_document.describe_services.json}"
  assume_roles_policy_json                  = "${data.aws_iam_policy_document.assume_roles.json}"
  cloudwatch_allow_filterlogs_policy_json   = "${data.aws_iam_policy_document.cloudwatch_allow_filterlogs.json}"
  allow_s3_write_policy_json                = "${data.aws_iam_policy_document.allow_s3_write.json}"
  allow_describe_budgets_policy_json        = "${data.aws_iam_policy_document.allow_describe_budgets.json}"
  s3_put_dashboard_status_policy_json       = "${data.aws_iam_policy_document.s3_put_dashboard_status.json}"
}
