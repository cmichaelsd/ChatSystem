variable "project_name" {
  type = string
}

variable "alb_dns_name" {
  type        = string
  description = "DNS name of the public ALB (apiserver)"
}

variable "chatserver_nlb_dns_name" {
  type        = string
  description = "DNS name of the chatserver NLB (WebSocket)"
}
