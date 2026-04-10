output "db_address" {
  value = aws_db_instance.postgres.address
}

output "db_password" {
  value     = random_password.db_password.result
  sensitive = true
}
