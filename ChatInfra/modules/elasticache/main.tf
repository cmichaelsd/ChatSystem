resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.project_name}-redis-subnet-group"
  subnet_ids = var.private_subnet_ids
}

# aws_elasticache_cluster does not support encryption — replication_group is
# required by the AWS API to enable at_rest/transit encryption, even for a
# single-node deployment with no actual replication.
resource "aws_elasticache_replication_group" "this" {
  replication_group_id = "${var.project_name}-redis"
  description          = "Redis presence store"
  node_type            = "cache.t3.micro"
  num_cache_clusters   = 1
  parameter_group_name = "default.redis7"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.this.name
  security_group_ids   = [var.elasticache_sg_id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  tags = { Name = "${var.project_name}-redis" }
}
