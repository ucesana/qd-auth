# infrastructure/modules/rds/main.tf

# Security group — controls which resources can connect to the RDS instance
resource "aws_security_group" "rds" {
  name        = "${var.name}-rds-sg"
  description = "Allow MySQL inbound from ECS tasks only."
  vpc_id      = var.vpc_id

  ingress {
    description     = "MySQL from ECS security group"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.name}-rds-sg"
  }
}

# Subnet group — tells RDS which subnets it may place instances in
resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-rds-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name = "${var.name}-rds-subnet-group"
  }
}

# Parameter group — MySQL server configuration
resource "aws_db_parameter_group" "this" {
  name   = "${var.name}-mysql8"
  family = "mysql8.0"

  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }

  tags = {
    Name = "${var.name}-mysql8"
  }
}

# RDS instance
resource "aws_db_instance" "this" {
  identifier        = "${var.name}-mysql"
  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = var.instance_class
  allocated_storage = var.allocated_storage
  storage_type      = "gp2"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  parameter_group_name   = aws_db_parameter_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az            = var.multi_az
  publicly_accessible = false
  skip_final_snapshot = var.skip_final_snapshot
  deletion_protection = var.deletion_protection

  backup_retention_period = var.backup_retention_period
  backup_window           = "03:00-04:00"
  maintenance_window      = "mon:04:00-mon:05:00"

  tags = {
    Name = "${var.name}-mysql"
  }
}

resource "aws_secretsmanager_secret" "db_password" {
  name                    = "${var.name}/db-password"
  recovery_window_in_days = var.deletion_protection ? 7 : 0

  tags = {
    Name = "${var.name}-db-password"
  }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id = aws_secretsmanager_secret.db_password.id
  secret_string = jsonencode({
    username = var.db_username
    password = var.db_password
  })
}
