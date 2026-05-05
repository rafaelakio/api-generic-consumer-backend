variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "project_name" {
  type    = string
  default = "api-consumer"
}

variable "eks_cluster_name" {
  description = "Name of the existing EKS cluster"
  type        = string
}

variable "eks_oidc_provider_arn" {
  description = "OIDC provider ARN of the EKS cluster (from aws_eks_cluster data source or outputs)"
  type        = string
}

variable "eks_oidc_issuer_url" {
  description = "OIDC issuer URL of the EKS cluster (without https://)"
  type        = string
}

variable "k8s_namespace" {
  description = "Kubernetes namespace where the backend runs"
  type        = string
  default     = "api-consumer"
}

variable "k8s_service_account_name" {
  description = "Name of the Kubernetes ServiceAccount for the backend pod"
  type        = string
  default     = "api-consumer-backend"
}
