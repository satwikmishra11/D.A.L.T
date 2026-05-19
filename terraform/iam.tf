# ========== GitHub OIDC for Passwordless CI/CD ==========
# Highly professional feature: eliminates the need to store long-lived
# AWS credentials in GitHub Secrets. Uses short-lived tokens via OIDC.

resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1", "1c58a3a8518e8759bf075b76b750d4f2df264fcd"] # Standard GitHub thumbprints
}

resource "aws_iam_role" "github_actions" {
  name        = "${local.name_prefix}-github-actions-role"
  description = "Role for GitHub Actions to deploy infrastructure via OIDC"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = aws_iam_openid_connect_provider.github.arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringLike = {
            "token.actions.githubusercontent.com:sub" : "repo:satwikmishra11/D.A.L.T:*"
          }
          StringEquals = {
            "token.actions.githubusercontent.com:aud" : "sts.amazonaws.com"
          }
        }
      }
    ]
  })

  tags = {
    Environment = var.environment
    Service     = "CICD"
  }
}

# Attach an appropriate policy. For Terraform deployment, this often needs to be broad,
# but in a strictly professional setup, you would scope this down significantly.
resource "aws_iam_role_policy_attachment" "github_actions_admin" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AdministratorAccess"
}
