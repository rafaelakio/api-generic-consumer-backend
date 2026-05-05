output "irsa_role_arn" {
  description = "ARN of the IRSA role — set as annotation on the K8s ServiceAccount"
  value       = aws_iam_role.backend_irsa.arn
}

output "ecr_repository_uri" {
  description = "ECR URI for the backend image — used in CI/CD and deployment.yaml"
  value       = aws_ecr_repository.backend.repository_url
}
