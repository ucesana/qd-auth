# infrastructure/environments/production/terraform.tfvars

aws_region          = "ap-southeast-2"
ecr_repository_name = "qd-auth"
db_username         = "qdauth"

# infrastructure/environments/production/terraform.tfvars — temporary, remove after destroy
skip_final_snapshot     = true
deletion_protection_rds = false
