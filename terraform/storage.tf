# ========== DocumentDB (MongoDB Compatible) ==========

resource "random_password" "docdb_password" {
  length  = 16
  special = false
}

resource "aws_security_group" "docdb" {
  name        = "${var.project_name}-${var.environment}-docdb-sg"
  description = "Allow inbound traffic from EKS"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 27017
    to_port         = 27017
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
}

module "documentdb" {
  source  = "cloudposse/documentdb-cluster/aws"
  version = "0.26.0"

  name           = "${var.project_name}-docdb"
  stage          = var.environment
  namespace      = var.project_name
  
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.database_subnets
  security_group_ids = [aws_security_group.docdb.id]
  
  instance_class = var.docdb_instance_class
  cluster_size   = var.docdb_cluster_size
  
  master_username = "admin"
  master_password = random_password.docdb_password.result
  
  # Encryption
  storage_encrypted = true
  
  # Backups
  retention_period = 7
}

# ========== ElastiCache (Redis) ==========

resource "aws_security_group" "redis" {
  name        = "${var.project_name}-${var.environment}-redis-sg"
  description = "Allow inbound traffic from EKS"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }
}

resource "aws_elasticache_subnet_group" "default" {
  name       = "${var.project_name}-${var.environment}-redis-subnet-group"
  subnet_ids = module.vpc.elasticache_subnets
}

resource "aws_elasticache_replication_group" "default" {
  replication_group_id          = "${var.project_name}-${var.environment}"
  description                   = "Redis cluster for Load Test Platform"
  node_type                     = var.redis_node_type
  port                          = 6379
  parameter_group_name          = "default.redis7"
  subnet_group_name             = aws_elasticache_subnet_group.default.name
  security_group_ids            = [aws_security_group.redis.id]
  
  automatic_failover_enabled    = var.environment == "prod"
  multi_az_enabled              = var.environment == "prod"
  
  num_cache_clusters            = var.environment == "prod" ? 2 : 1
  at_rest_encryption_enabled    = true
  transit_encryption_enabled    = true
}
