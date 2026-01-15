module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "${var.project_name}-${var.environment}"
  cluster_version = "1.28"

  cluster_endpoint_public_access  = true
  cluster_endpoint_private_access = true

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
    }

    # Scalable workers (Load Generators)
    workers = {
      min_size     = 0
      max_size     = 50
      desired_size = 0 # Controlled by Cluster Autoscaler

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
    }
  }

  # Cluster Autoscaler IRSA
  # Note: You would typically install the Cluster Autoscaler helm chart using this role
}

# Allow workers to scale
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
