terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    dynamodb = "http://localhost:4566"
  }
}

resource "aws_dynamodb_table" "events" {
  name         = local.events_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "venue"
    type = "S"
  }

  attribute {
    name = "date"
    type = "S"
  }

  global_secondary_index {
    name            = "venue-date-index"
    hash_key        = "venue"
    range_key       = "date"
    projection_type = "ALL"
  }

  tags = var.tags
}

resource "aws_dynamodb_table" "orders" {
  name         = local.orders_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "orderId"

  attribute {
    name = "orderId"
    type = "S"
  }

  attribute {
    name = "eventId"
    type = "S"
  }

  attribute {
    name = "status"
    type = "S"
  }

  attribute {
    name = "expiresAt"
    type = "S"
  }

  global_secondary_index {
    name            = "eventId-index"
    hash_key        = "eventId"
    projection_type = "ALL"
  }

  global_secondary_index {
    name            = "status-expiresAt-index"
    hash_key        = "status"
    range_key       = "expiresAt"
    projection_type = "ALL"
  }

  tags = var.tags
}
