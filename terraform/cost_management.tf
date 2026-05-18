# ========== AWS Budgets (Cost Management) ==========
# Highly professional feature to monitor costs and prevent bill shock.
# Alerts are sent when actual or forecasted costs exceed the threshold.

resource "aws_budgets_budget" "infrastructure_budget" {
  name              = "${local.name_prefix}-monthly-budget"
  budget_type       = "COST"
  limit_amount      = "1000"
  limit_unit        = "USD"
  time_unit         = "MONTHLY"
  time_period_start = "2023-01-01_00:00" # Arbitrary past date to start immediately

  # Alert when actual costs hit 80%
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = ["admin@example.com"]
  }

  # Alert when actual costs hit 100%
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = ["admin@example.com"]
  }

  # Alert when forecasted costs hit 110%
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 110
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = ["admin@example.com"]
  }
}
