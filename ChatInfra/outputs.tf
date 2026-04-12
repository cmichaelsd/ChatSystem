output "apiserver_url" {
  description = "Public URL for the ApiServer (via CloudFront)"
  value       = "https://${module.static_site.cloudfront_domain}"
}

output "chatserver_ws_url" {
  description = "WebSocket URL for the ChatServer (via CloudFront)"
  value       = "wss://${module.static_site.cloudfront_domain}/ws"
}

output "chatserver_api_url" {
  description = "HTTP URL for the ChatServer REST API"
  value       = "http://${module.alb.chatserver_nlb_dns_name}"
}

output "chatclient_url" {
  description = "CloudFront URL for the ChatClient"
  value       = "https://${module.static_site.cloudfront_domain}"
}

output "chatclient_bucket" {
  description = "S3 bucket name for deploying the ChatClient"
  value       = module.static_site.bucket_name
}

output "chatclient_distribution_id" {
  description = "CloudFront distribution ID for cache invalidation"
  value       = module.static_site.distribution_id
}
