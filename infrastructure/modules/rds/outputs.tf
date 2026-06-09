# infrastructure/modules/rds/outputs.tf

output "endpoint" {
  description = "RDS instance endpoint hostname."
  value       = aws_db_instance.this.address
}

output "port" {
  description = "RDS instance port."
  value       = aws_db_instance.this.port
}

output "db_name" {
  description = "Database name."
  value       = aws_db_instance.this.db_name
}

output "security_group_id" {
  description = "RDS security group ID."
  value       = aws_security_group.rds.id
}

output "db_password_secret_arn" {
  description = "ARN of the Secrets Manager secret storing the database password."
  value       = aws_secretsmanager_secret.db_password.arn
}
