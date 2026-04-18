resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

resource "random_password" "internal_api_key" {
  length  = 32
  special = false
}

# RDS credentials — stored as a JSON object so individual fields can be
# referenced by ARN key suffix in ECS task definitions
resource "aws_secretsmanager_secret" "db" {
  name                    = "${var.project_name}/db"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username     = var.db_name
    password     = var.db_password
    host         = var.db_address
    port         = "5432"
    dbname       = var.db_name
    database_url = "postgresql+asyncpg://${var.db_name}:${var.db_password}@${var.db_address}:5432/${var.db_name}?ssl=require"
  })
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name                    = "${var.project_name}/jwt_secret"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "jwt_secret" {
  secret_id     = aws_secretsmanager_secret.jwt_secret.id
  secret_string = random_password.jwt_secret.result
}

resource "aws_secretsmanager_secret" "internal_api_key" {
  name                    = "${var.project_name}/internal_api_key"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "internal_api_key" {
  secret_id     = aws_secretsmanager_secret.internal_api_key.id
  secret_string = random_password.internal_api_key.result
}
