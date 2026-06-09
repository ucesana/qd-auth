variable "aws_region" {
  description = "AWS region for the remote state backend resources."
  type        = string
  default     = "ap-southeast-4"
}

variable "state_bucket_name" {
  description = "Globally unique S3 bucket name for Terraform state storage."
  type        = string
}
