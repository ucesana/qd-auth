# infrastructure/modules/alb/variables.tf

variable "name" {
  description = "Name prefix applied to all resources."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID."
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet IDs for the ALB."
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "ECS security group ID to permit ALB ingress."
  type        = string
}

variable "container_port" {
  description = "Port the ECS tasks listen on."
  type        = number
  default     = 8080
}

variable "deletion_protection" {
  description = "Prevent accidental deletion of the ALB."
  type        = bool
  default     = false
}
