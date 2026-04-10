output "db_secret_arn" {
  value = aws_secretsmanager_secret.db.arn
}

output "jwt_secret_arn" {
  value = aws_secretsmanager_secret.jwt_secret.arn
}

output "internal_api_key_arn" {
  value = aws_secretsmanager_secret.internal_api_key.arn
}
