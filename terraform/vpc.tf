module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${var.project_name}-${var.environment}-vpc"
  cidr = var.vpc_cidr

  azs             = var.availability_zones
  private_subnets = [for k, v in var.availability_zones : cidrsubnet(var.vpc_cidr, 4, k)]
  public_subnets  = [for k, v in var.availability_zones : cidrsubnet(var.vpc_cidr, 8, k + 48)]
  database_subnets = [for k, v in var.availability_zones : cidrsubnet(var.vpc_cidr, 8, k + 52)]
  elasticache_subnets = [for k, v in var.availability_zones : cidrsubnet(var.vpc_cidr, 8, k + 56)]

  enable_nat_gateway     = true
  single_nat_gateway     = var.environment != "prod" # Cost saving for non-prod
  one_nat_gateway_per_az = var.environment == "prod" # High availability for prod

  enable_dns_hostnames = true
  enable_dns_support   = true

  # Tags for EKS auto-discovery
  public_subnet_tags = {
    "kubernetes.io/role/elb" = 1
  }

  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = 1
  }
}
