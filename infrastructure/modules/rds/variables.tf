# infrastructure/modules/rds/variables.tf

variable "name" {
  description = "Name prefix applied to all resources."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID."
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for the RDS subnet group."
  type        = list(string)
}

variable "ecs_security_group_id" {
  description = "Security group ID of the ECS tasks permitted to connect to RDS."
  type        = string
}

variable "db_name" {
  description = "Name of the initial database."
  type        = string
  default     = "qdauth"
}

variable "db_username" {
  description = "Master database username."
  type        = string
}

variable "db_password" {
  description = "Master database password. Inject from Secrets Manager — never hardcode."
  type        = string
  sensitive   = true
}

variable "instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t3.micro"
}

variable "allocated_storage" {
  description = "Allocated storage in gigabytes."
  type        = number
  default     = 20
}

variable "multi_az" {
  description = "Enable Multi-AZ deployment for high availability."
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "Skip final snapshot on deletion. Set false for production."
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "Prevent accidental deletion. Set true for production."
  type        = bool
  default     = false
}

variable "backup_retention_period" {
  description = "Number of days to retain automated backups."
  type        = number
  default     = 7
}
