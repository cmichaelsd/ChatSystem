module "vpc" {
  source       = "./modules/vpc"
  project_name = var.project_name
}

module "security_groups" {
  source       = "./modules/security_groups"
  project_name = var.project_name
  vpc_id       = module.vpc.vpc_id
}

module "alb" {
  source             = "./modules/alb"
  project_name       = var.project_name
  vpc_id             = module.vpc.vpc_id
  alb_sg_id          = module.security_groups.alb_sg_id
  internal_alb_sg_id = module.security_groups.internal_alb_sg_id
  public_subnet_ids  = module.vpc.public_subnets
  private_subnet_ids = module.vpc.private_subnets
}

module "dynamodb" {
  source       = "./modules/dynamodb"
  project_name = var.project_name
}

module "rds" {
  source             = "./modules/rds"
  project_name       = var.project_name
  db_name            = var.db_name
  private_subnet_ids = module.vpc.private_subnets
  rds_sg_id          = module.security_groups.rds_sg_id
}

module "elasticache" {
  source             = "./modules/elasticache"
  project_name       = var.project_name
  private_subnet_ids = module.vpc.private_subnets
  elasticache_sg_id  = module.security_groups.elasticache_sg_id
}

module "secrets" {
  source       = "./modules/secrets"
  project_name = var.project_name
  db_name      = var.db_name
  db_password  = module.rds.db_password
  db_address   = module.rds.db_address
}

module "ecs" {
  source               = "./modules/ecs"
  project_name         = var.project_name
  region               = var.aws_region
  db_secret_arn        = module.secrets.db_secret_arn
  jwt_secret_arn       = module.secrets.jwt_secret_arn
  internal_api_key_arn = module.secrets.internal_api_key_arn
  dynamodb_table_arns  = module.dynamodb.table_arns
}

module "static_site" {
  source                  = "./modules/static_site"
  project_name            = var.project_name
  alb_dns_name            = module.alb.public_alb_dns_name
  chatserver_nlb_dns_name = module.alb.chatserver_nlb_dns_name
}

module "cloudtrail" {
  source       = "./modules/cloudtrail"
  project_name = var.project_name
}

module "ecs_services" {
  source                            = "./modules/ecs_services"
  project_name                      = var.project_name
  region                            = var.aws_region
  cluster_id                        = module.ecs.cluster_id
  execution_role_api_arn            = module.ecs.execution_role_api_arn
  execution_role_chatserver_arn     = module.ecs.execution_role_chatserver_arn
  execution_role_presenceserver_arn = module.ecs.execution_role_presenceserver_arn
  chatserver_task_role_arn          = module.ecs.chatserver_task_role_arn
  apiserver_task_role_arn           = module.ecs.apiserver_task_role_arn
  presenceserver_task_role_arn      = module.ecs.presenceserver_task_role_arn
  private_subnet_ids                = module.vpc.private_subnets
  ecs_tasks_sg_id                   = module.security_groups.ecs_tasks_sg_id
  chatserver_nlb_dns_name           = module.alb.chatserver_nlb_dns_name
  internal_alb_dns_name             = module.alb.internal_alb_dns_name
  apiserver_tg_arn                  = module.alb.apiserver_tg_arn
  presenceserver_tg_arn             = module.alb.presenceserver_tg_arn
  chatserver_tg_arn                 = module.alb.chatserver_tg_arn
  db_secret_arn                     = module.secrets.db_secret_arn
  jwt_secret_arn                    = module.secrets.jwt_secret_arn
  internal_api_key_arn              = module.secrets.internal_api_key_arn
  redis_address                     = module.elasticache.redis_address
  apiserver_log_group               = module.ecs.apiserver_log_group
  chatserver_log_group              = module.ecs.chatserver_log_group
  presenceserver_log_group          = module.ecs.presenceserver_log_group
  apiserver_xray_log_group          = module.ecs.apiserver_xray_log_group
  chatserver_xray_log_group         = module.ecs.chatserver_xray_log_group
  presenceserver_xray_log_group     = module.ecs.presenceserver_xray_log_group
  cloudfront_domain                 = module.static_site.cloudfront_domain
}
