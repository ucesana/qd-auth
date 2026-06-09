variable "aws_region" {
  description = "AWS region."
  type        = string
  default     = "ap-southeast-2"
}

variable "ecr_repository_name" {
  description = "ECR repository name."
  type        = string
  default     = "qd-auth"
}

variable "db_username" {
  description = "RDS master username."
  type        = string
}

variable "db_password" {
  description = "RDS master password."
  type        = string
  sensitive   = true
}

variable "rsa_private_key" {
  description = "Base64-encoded RSA private key."
  type        = string
  sensitive   = true
}

variable "rsa_public_key" {
  description = "Base64-encoded RSA public key."
  type        = string
  sensitive   = true
}

variable "skip_final_snapshot" {
  type    = bool
  default = false
}

variable "deletion_protection_rds" {
  type    = bool
  default = true
}
