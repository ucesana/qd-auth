terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket       = "qd-terraform-state-580241343441"
    key          = "production/terraform.tfstate"
    region       = "ap-southeast-4"
    use_lockfile = true
    encrypt      = true
  }
}

provider "aws" {
  region = var.aws_region
}

data "terraform_remote_state" "shared" {
  backend = "s3"
  config = {
    bucket = "qd-terraform-state-580241343441"
    key    = "shared/terraform.tfstate"
    region = "ap-southeast-4"
  }
}

module "vpc" {
  source = "../../modules/vpc"
  name   = "qd-auth-production"
}

resource "aws_security_group" "ecs" {
  name        = "qd-auth-production-ecs-sg"
  description = "ECS tasks security group."
  vpc_id      = module.vpc.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "qd-auth-production-ecs-sg"
  }
}

module "rds" {
  source = "../../modules/rds"

  name                  = "qd-auth-production"
  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnet_ids
  ecs_security_group_id = aws_security_group.ecs.id
  db_username           = var.db_username
  db_password           = var.db_password

  instance_class          = "db.t3.small"
  multi_az                = true
  skip_final_snapshot     = false
  deletion_protection     = true
  backup_retention_period = 7
}

resource "aws_secretsmanager_secret" "rsa_private_key" {
  name                    = "qd-auth-production/rsa-private-key"
  recovery_window_in_days = 0

  tags = { Name = "qd-auth-production-rsa-private-key" }
}

resource "aws_secretsmanager_secret_version" "rsa_private_key" {
  secret_id     = aws_secretsmanager_secret.rsa_private_key.id
  secret_string = var.rsa_private_key
}

resource "aws_secretsmanager_secret" "rsa_public_key" {
  name                    = "qd-auth-production/rsa-public-key"
  recovery_window_in_days = 0

  tags = { Name = "qd-auth-production-rsa-public-key" }
}

resource "aws_secretsmanager_secret_version" "rsa_public_key" {
  secret_id     = aws_secretsmanager_secret.rsa_public_key.id
  secret_string = var.rsa_public_key
}

module "ecs" {
  source = "../../modules/ecs"

  name                       = "qd-auth-staging"
  aws_region                 = var.aws_region
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  ecs_security_group_id      = aws_security_group.ecs.id
  target_group_arn           = module.alb.target_group_arn
  container_image            = "${data.terraform_remote_state.shared.outputs.ecr_repository_url}:latest"
  db_endpoint                = module.rds.endpoint
  db_name                    = module.rds.db_name
  db_password_secret_arn     = module.rds.db_password_secret_arn
  rsa_private_key_secret_arn = aws_secretsmanager_secret.rsa_private_key.arn
  rsa_public_key_secret_arn  = aws_secretsmanager_secret.rsa_public_key.arn

  secret_arns = [
    module.rds.db_password_secret_arn,
    aws_secretsmanager_secret.rsa_private_key.arn,
    aws_secretsmanager_secret.rsa_public_key.arn,
  ]

  task_cpu      = 512
  task_memory   = 1024
  desired_count = 2
}

module "alb" {
  source = "../../modules/alb"

  name                  = "qd-auth-production"
  vpc_id                = module.vpc.vpc_id
  public_subnet_ids     = module.vpc.public_subnet_ids
  ecs_security_group_id = aws_security_group.ecs.id
  deletion_protection   = true
}
