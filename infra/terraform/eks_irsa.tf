# ── IAM Role for Service Accounts (IRSA) ─────────────────────────────────────
# Allows the backend pod's Kubernetes ServiceAccount to assume this IAM role,
# which grants access to Secrets Manager without embedding AWS credentials.

locals {
  oidc_issuer = trimprefix(var.eks_oidc_issuer_url, "https://")
}

resource "aws_iam_role" "backend_irsa" {
  name = "${var.project_name}-${var.environment}-backend-irsa"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Federated = var.eks_oidc_provider_arn
        }
        Action = "sts:AssumeRoleWithWebIdentity"
        Condition = {
          StringEquals = {
            "${local.oidc_issuer}:sub" = "system:serviceaccount:${var.k8s_namespace}:${var.k8s_service_account_name}"
            "${local.oidc_issuer}:aud" = "sts.amazonaws.com"
          }
        }
      }
    ]
  })

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

resource "aws_iam_role_policy_attachment" "backend_read_secrets" {
  role       = aws_iam_role.backend_irsa.name
  policy_arn = aws_iam_policy.read_secrets.arn
}
