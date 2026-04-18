resource "random_password" "db_password" {
  length           = 32
  special          = true
  override_special = "!#$%&*()-_=+[]{}:?"
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = var.private_subnet_ids

  tags = { Name = "${var.project_name}-db-subnet-group" }
}

# Enforce SSL-only connections at the database level
resource "aws_db_parameter_group" "postgres" {
  name        = "${var.project_name}-postgres-params"
  family      = "postgres16"
  description = "Enforce SSL connections to PostgreSQL"

  parameter {
    name         = "rds.force_ssl"
    value        = "1"
    apply_method = "pending-reboot"
  }

  tags = { Name = "${var.project_name}-postgres-params" }
}

resource "aws_db_instance" "postgres" {
  identifier        = "${var.project_name}-postgres"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = "db.t3.micro"
  allocated_storage = 20

  db_name  = var.db_name
  username = var.db_name
  password = random_password.db_password.result

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.rds_sg_id]
  parameter_group_name   = aws_db_parameter_group.postgres.name

  # Encryption at rest
  storage_encrypted = true

  # Backup and recovery
  backup_retention_period   = 7
  skip_final_snapshot       = false
  final_snapshot_identifier = "${var.project_name}-postgres-final"
  deletion_protection       = true

  publicly_accessible = false

  tags = { Name = "${var.project_name}-postgres" }
}
