output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "eks_cluster_id" {
  description = "EKS Cluster ID"
  value       = module.eks.cluster_id
}

output "eks_cluster_endpoint" {
  description = "EKS Cluster Endpoint"
  value       = module.eks.cluster_endpoint
}

output "eks_kubectl_config_command" {
  description = "Command to update kubeconfig"
  value       = "aws eks update-kubeconfig --name ${module.eks.cluster_name} --region ${var.region}"
}

output "docdb_endpoint" {
  description = "DocumentDB Cluster Endpoint"
  value       = module.documentdb.endpoint
}

output "docdb_password_secret_name" {
  description = "AWS Secret name containing the DocDB password (if using secrets manager, otherwise handle securely)"
  value       = "Check random_password state or implement Secrets Manager"
  sensitive   = true
}

output "redis_primary_endpoint" {
  description = "Redis Primary Endpoint"
  value       = aws_elasticache_replication_group.default.primary_endpoint_address
}
