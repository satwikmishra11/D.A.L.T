# ========== Amazon Managed Prometheus (AMP) ==========

resource "aws_prometheus_workspace" "main" {
  alias = "${var.project_name}-${var.environment}-prometheus"

  tags = {
    Environment = var.environment
    Service     = "Observability"
  }
}

# ========== Amazon Managed Grafana (AMG) ==========

resource "aws_iam_role" "grafana" {
  name = "${var.project_name}-${var.environment}-grafana-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "grafana.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_grafana_workspace" "main" {
  account_access_type      = "CURRENT_ACCOUNT"
  authentication_providers = ["AWS_SSO"]
  permission_type          = "SERVICE_MANAGED"
  role_arn                 = aws_iam_role.grafana.arn
  data_sources             = ["PROMETHEUS", "CLOUDWATCH"]

  name = "${var.project_name}-${var.environment}-grafana"
  
  tags = {
    Environment = var.environment
    Service     = "Observability"
  }
}

# Provide Grafana workspace access to CloudWatch and Prometheus
resource "aws_iam_role_policy_attachment" "grafana_cloudwatch" {
  role       = aws_iam_role.grafana.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchReadOnlyAccess"
}

resource "aws_iam_role_policy" "grafana_prometheus" {
  name = "${var.project_name}-grafana-prometheus-access"
  role = aws_iam_role.grafana.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "aps:QueryMetrics",
          "aps:GetSeries",
          "aps:GetLabels",
          "aps:GetMetricMetadata"
        ]
        Resource = aws_prometheus_workspace.main.arn
      }
    ]
  })
}

# ========== CloudWatch Log Groups ==========

resource "aws_cloudwatch_log_group" "eks_cluster" {
  name              = "/aws/eks/${var.project_name}-${var.environment}/cluster"
  retention_in_days = 14
  kms_key_id        = aws_kms_key.main.arn

  tags = {
    Environment = var.environment
  }
}

resource "aws_cloudwatch_log_group" "application" {
  name              = "/${var.project_name}/${var.environment}/app"
  retention_in_days = 30
  kms_key_id        = aws_kms_key.main.arn

  tags = {
    Environment = var.environment
  }
}
