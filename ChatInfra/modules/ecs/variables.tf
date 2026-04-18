variable "project_name" {
  type = string
}

variable "region" {
  type = string
}

variable "db_secret_arn" {
  type = string
}

variable "jwt_secret_arn" {
  type = string
}

variable "internal_api_key_arn" {
  type = string
}

variable "dynamodb_table_arns" {
  type = list(string)
}
