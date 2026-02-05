# Terraform Variables for EcoGo

variable "aws_region" {
  description = "AWS region for deployment"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "ecogo"
}

variable "environment" {
  description = "Environment (dev, staging, production)"
  type        = string
  default     = "staging"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "ecs_task_cpu" {
  description = "CPU units for ECS task"
  type        = string
  default     = "256"
}

variable "ecs_task_memory" {
  description = "Memory for ECS task"
  type        = string
  default     = "512"
}

variable "ecs_service_desired_count" {
  description = "Desired number of ECS tasks"
  type        = number
  default     = 1
}

variable "ecr_repository_url" {
  description = "ECR repository URL for Docker image"
  type        = string
  default     = "your-account-id.dkr.ecr.us-east-1.amazonaws.com/ecogo"
}

variable "mongodb_uri" {
  description = "MongoDB connection URI"
  type        = string
  sensitive   = true
  default     = "mongodb://localhost:27017/EcoGo"
}
