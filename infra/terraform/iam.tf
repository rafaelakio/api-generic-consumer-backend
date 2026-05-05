# ── Secrets Manager read policy ───────────────────────────────────────────────
resource "aws_iam_policy" "read_secrets" {
  name        = "${var.project_name}-${var.environment}-backend-read-secrets"
  description = "Allows the backend to read API credential secrets from Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadSecrets"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
        ]
        Resource = [
          # Default API credentials secret
          "arn:aws:secretsmanager:${var.aws_region}:*:secret:${var.project_name}/api-credentials*",
          # Pattern for per-API named secrets
          "arn:aws:secretsmanager:${var.aws_region}:*:secret:${var.project_name}/*",
        ]
      }
    ]
  })
}

# ── ECR pull policy (attached to EKS node role, or use image pull secret) ────
resource "aws_iam_policy" "ecr_pull" {
  name        = "${var.project_name}-${var.environment}-ecr-pull"
  description = "Allows EKS nodes to pull the backend image from ECR"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ECRPull"
        Effect = "Allow"
        Action = [
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetAuthorizationToken",
        ]
        Resource = "*"
      }
    ]
  })
}
