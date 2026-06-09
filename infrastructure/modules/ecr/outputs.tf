output "repository_url" {
  description = "Full ECR repository URL used in docker push and ECS task definitions."
  value       = aws_ecr_repository.this.repository_url
}

output "repository_arn" {
  description = "ARN of the ECR repository, used in IAM policies."
  value       = aws_ecr_repository.this.arn
}

output "registry_id" {
  description = "AWS account ID associated with the registry."
  value       = aws_ecr_repository.this.registry_id
}
