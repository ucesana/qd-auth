output "state_bucket_name" {
  description = "S3 bucket name for Terraform remote state."
  value       = aws_s3_bucket.terraform_state.bucket
}

output "aws_region" {
  description = "AWS region where backend resources are provisioned."
  value       = var.aws_region
}
