output "apiserver_url" {
  description = "Public URL for the ApiServer"
  value       = "http://${module.alb.public_alb_dns_name}"
}

output "chatserver_ws_url" {
  description = "WebSocket URL for the ChatServer"
  value       = "ws://${module.alb.chatserver_nlb_dns_name}/chat"
}

output "chatserver_api_url" {
  description = "HTTP URL for the ChatServer REST API"
  value       = "http://${module.alb.chatserver_nlb_dns_name}"
}
