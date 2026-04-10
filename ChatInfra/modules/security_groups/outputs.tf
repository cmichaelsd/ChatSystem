output "rds_sg_id" {
    value = aws_security_group.rds.id
}

output "elasticache_sg_id" {
    value = aws_security_group.elasticache.id
}

output "ecs_tasks_sg_id" {
    value = aws_security_group.ecs_tasks.id
}

output "alb_sg_id" {
    value = aws_security_group.alb.id
}