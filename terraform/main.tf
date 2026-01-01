terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
  
  backend "s3" {
    bucket = "loadtest-terraform-state"
    key    = "prod/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
}

module "eks" {
  source = "./modules/eks"
  
  cluster_name    = "loadtest-prod"
  cluster_version = "1.28"
  vpc_id          = var.vpc_id
  subnet_ids      = var.subnet_ids
}

module "rds" {
  source = "./modules/rds"
  
  identifier     = "loadtest-mongodb"
  instance_class = "db.t3.medium"
  allocated_storage = 100
}

module "elasticache" {
  source = "./modules/elasticache"
  
  cluster_id      = "loadtest-redis"
  node_type       = "cache.t3.medium"
  num_cache_nodes = 2
}