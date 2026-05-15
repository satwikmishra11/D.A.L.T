# tfsec:ignore:aws-eks-no-public-cluster-access
# tfsec:ignore:aws-eks-no-public-cluster-access-to-cidr
# tfsec:ignore:aws-ec2-no-public-egress-sgr
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "${var.project_name}-${var.environment}"
  cluster_version = "1.28"

  cluster_endpoint_public_access       = true
  cluster_endpoint_private_access      = true
  cluster_endpoint_public_access_cidrs = ["0.0.0.0/0"]

  # Enable Control Plane Logging for audit and compliance
  cluster_enabled_log_types = ["audit", "api", "authenticator", "controllerManager", "scheduler"]

  create_kms_key = false
  cluster_encryption_config = {
    provider_key_arn = aws_kms_key.main.arn
    resources        = ["secrets"]
  }

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  enable_irsa = true

  eks_managed_node_group_defaults = {
    disk_size = 50
  }

  eks_managed_node_groups = {
    # Critical services (Controller, DBs if on K8s)
    core = {
      min_size     = 2
      max_size     = 5
      desired_size = 2

      instance_types = ["t3.large"]
      capacity_type  = "ON_DEMAND"

      labels = {
        workload = "core"
      }
      
      iam_role_additional_policies = {
        CloudWatchAgentServerPolicy = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
      }
    }

    # Scalable workers (Load Generators)
    workers = {
      min_size     = 0
      max_size     = 50
      desired_size = 0 # Controlled by Cluster Autoscaler/Karpenter

      instance_types = ["c6i.large", "c6a.large"]
      capacity_type  = "SPOT" # Cost optimization for load testing

      labels = {
        workload = "generator"
      }

      taints = {
        dedicated = {
          key    = "workload"
          value  = "generator"
          effect = "NO_SCHEDULE"
        }
      }
      
      iam_role_additional_policies = {
        CloudWatchAgentServerPolicy = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
      }
    }
  }

  # Cluster Autoscaler IRSA
  # Note: You would typically install the Cluster Autoscaler helm chart using this role
}

# Allow workers to scale
# tfsec:ignore:aws-iam-no-policy-wildcards
resource "aws_iam_policy" "worker_autoscaling" {
  name        = "${var.project_name}-${var.environment}-worker-autoscaling"
  description = "EKS worker node autoscaling policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "autoscaling:DescribeAutoScalingGroups",
          "autoscaling:DescribeAutoScalingInstances",
          "autoscaling:DescribeLaunchConfigurations",
          "autoscaling:DescribeTags",
          "autoscaling:SetDesiredCapacity",
          "autoscaling:TerminateInstanceInAutoScalingGroup"
        ]
        Effect   = "Allow"
        Resource = "*"
      },
    ]
  })
}

# ========== IAM Roles for Service Accounts (IRSA) ==========

module "load_balancer_controller_irsa_role" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.30"

  role_name                              = "${var.project_name}-${var.environment}-alb-controller"
  attach_load_balancer_controller_policy = true

  oidc_providers = {
    main = {
      provider_arn               = module.eks.oidc_provider_arn
      namespace_service_accounts = ["kube-system:aws-load-balancer-controller"]
    }
  }
}

# ========== Karpenter (Modern Node Autoscaling) ==========
# Highly professional and cutting edge alternative to Cluster Autoscaler
# Provides rapid, intelligent, just-in-time node provisioning.

module "karpenter" {
  source  = "terraform-aws-modules/eks/aws//modules/karpenter"
  version = "~> 19.0"

  cluster_name = module.eks.cluster_name

  irsa_oidc_provider_arn          = module.eks.oidc_provider_arn
  irsa_namespace_service_accounts = ["karpenter:karpenter"]

  # Attach additional IAM policies to the Karpenter node IAM role
  iam_role_additional_policies = {
    AmazonSSMManagedInstanceCore = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
  }

  tags = {
    Environment = var.environment
    Service     = "Karpenter"
  }
}
