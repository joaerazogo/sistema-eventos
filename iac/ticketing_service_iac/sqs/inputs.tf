######################################
##        Variables Globales        ##
######################################

variable "env" {
  description = "Ambiente donde será desplegado el componente."
  type        = string
  validation {
    condition     = contains(["dev", "qa", "pdn"], var.env)
    error_message = "Valores permitidos: dev, qa, pdn."
  }
}

variable "tags" {
  description = "Tags adicionales para aplicar a los recursos."
  type        = map(string)
  default     = {}
}

######################################
##          Variables SQS           ##
######################################

variable "visibility_timeout" {
  description = "Tiempo en segundos que un mensaje es invisible tras ser recibido."
  type        = number
  default     = 30
}

variable "message_retention" {
  description = "Tiempo en segundos que SQS retiene un mensaje."
  type        = number
  default     = 86400
}

variable "max_receive_count" {
  description = "Número de reintentos antes de enviar el mensaje a la DLQ."
  type        = number
  default     = 3
}
