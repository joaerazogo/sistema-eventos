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
