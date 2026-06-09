# infrastructure/modules/ecs/variables.tf

variable "name" {
  description = "Name prefix applied to all resources."
  type        = string
}

variable "aws_region" {
  description = "AWS region."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for ECS tasks."
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group ID for ECS tasks."
  type        = string
}

variable "target_group_arn" {
  description = "ALB target group ARN."
  type        = string
}

variable "container_image" {
  description = "Full ECR image URI including tag."
  type        = string
}

variable "container_port" {
  description = "Port the container listens on."
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "Fargate task CPU units (256, 512, 1024, 2048, 4096)."
  type        = number
  default     = 256
}

variable "task_memory" {
  description = "Fargate task memory in MB."
  type        = number
  default     = 512
}

variable "desired_count" {
  description = "Desired number of running tasks."
  type        = number
  default     = 1
}

variable "log_retention_days" {
  description = "CloudWatch log retention in days."
  type        = number
  default     = 7
}

variable "db_endpoint" {
  description = "RDS endpoint hostname."
  type        = string
}

variable "db_name" {
  description = "Database name."
  type        = string
}

variable "db_password_secret_arn" {
  description = "ARN of the Secrets Manager secret storing database credentials."
  type        = string
}

variable "rsa_private_key_secret_arn" {
  description = "ARN of the Secrets Manager secret storing the RSA private key."
  type        = string
}

variable "rsa_public_key_secret_arn" {
  description = "ARN of the Secrets Manager secret storing the RSA public key."
  type        = string
}

variable "secret_arns" {
  description = "List of Secrets Manager ARNs the execution role may read."
  type        = list(string)
}
