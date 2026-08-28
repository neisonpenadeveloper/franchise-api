variable "atlas_org_id" {
  description = "Id de la organizacion de MongoDB Atlas donde se crea el proyecto"
  type        = string
}

variable "project_name" {
  description = "Nombre del proyecto en Atlas"
  type        = string
  default     = "franchise-api"
}

variable "cluster_name" {
  description = "Nombre del cluster"
  type        = string
  default     = "franchise-cluster"
}

variable "cloud_provider" {
  description = "Proveedor que respalda el cluster gratuito M0 (AWS, GCP o AZURE)"
  type        = string
  default     = "AWS"
}

variable "region" {
  description = "Region del proveedor, en nomenclatura de Atlas (por ejemplo US_EAST_1)"
  type        = string
  default     = "US_EAST_1"
}

variable "database_name" {
  description = "Base de datos que usa la aplicacion"
  type        = string
  default     = "franchisedb"
}

variable "db_username" {
  description = "Usuario de base de datos que usara la aplicacion"
  type        = string
  default     = "franchise_app"
}

variable "db_password" {
  description = "Password del usuario de base de datos. Se pasa por TF_VAR_db_password, nunca por el repositorio."
  type        = string
  sensitive   = true
}

variable "allowed_cidr_blocks" {
  description = <<-EOT
    Rangos de IP con acceso al cluster. El valor por defecto (0.0.0.0/0) solo
    sirve para una demo: en un entorno real se restringe a la IP de salida del
    servicio que consume la base de datos.
  EOT
  type        = list(string)
  default     = ["0.0.0.0/0"]
}
