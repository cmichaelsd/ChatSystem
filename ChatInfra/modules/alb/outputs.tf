output "public_alb_dns_name" {
  value = aws_lb.public_alb.dns_name
}

output "internal_alb_dns_name" {
  value = aws_lb.internal_alb.dns_name
}

output "chatserver_nlb_dns_name" {
  value = aws_lb.chatserver_nlb.dns_name
}

output "apiserver_tg_arn" {
  value = aws_lb_target_group.apiserver.arn
}

output "presenceserver_tg_arn" {
  value = aws_lb_target_group.presenceserver.arn
}

output "chatserver_tg_arn" {
  value = aws_lb_target_group.chatserver.arn
}
