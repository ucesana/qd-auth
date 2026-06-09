# infrastructure/modules/alb/outputs.tf

output "alb_dns_name" {
  description = "ALB DNS name — point your domain's CNAME here."
  value       = aws_lb.this.dns_name
}

output "alb_zone_id" {
  description = "ALB hosted zone ID for Route 53 alias records."
  value       = aws_lb.this.zone_id
}

output "target_group_arn" {
  description = "Target group ARN for the ECS service."
  value       = aws_lb_target_group.this.arn
}

output "alb_security_group_id" {
  description = "ALB security group ID."
  value       = aws_security_group.alb.id
}
