# infrastructure/environments/shared/variables.tf

variable "aws_region" {
  type    = string
  default = "ap-southeast-2"
}

variable "ecr_repository_name" {
  type    = string
  default = "qd-auth"
}

variable "github_org" {
  description = "GitHub organisation or username owning the repository."
  type        = string
}

variable "github_repo" {
  description = "GitHub repository name."
  type        = string
}
