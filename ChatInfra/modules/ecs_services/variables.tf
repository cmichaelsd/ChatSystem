variable "project_name" {
  type = string
}

variable "region" {
  type = string
}

variable "cluster_id" {
  type = string
}

variable "execution_role_arn" {
  type = string
}

variable "chatserver_task_role_arn" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "ecs_tasks_sg_id" {
  type = string
}

variable "chatserver_nlb_dns_name" {
  type = string
}

variable "internal_alb_dns_name" {
  type = string
}

variable "apiserver_tg_arn" {
  type = string
}

variable "presenceserver_tg_arn" {
  type = string
}

variable "chatserver_tg_arn" {
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

variable "redis_address" {
  type = string
}

variable "apiserver_log_group" {
  type = string
}

variable "chatserver_log_group" {
  type = string
}

variable "presenceserver_log_group" {
  type = string
}
