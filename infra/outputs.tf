output "connection_string" {
  description = "Cadena de conexion SRV del cluster (sin credenciales)"
  value       = mongodbatlas_advanced_cluster.franchise.connection_strings[0].standard_srv
}

output "mongodb_uri" {
  description = <<-EOT
    URI lista para la variable de entorno MONGODB_URI de la aplicacion.
    Es sensible porque incluye la password del usuario de base de datos:
    se consulta con `terraform output -raw mongodb_uri`.
  EOT
  sensitive   = true
  value = format(
    "mongodb+srv://%s:%s@%s/%s?retryWrites=true&w=majority",
    var.db_username,
    var.db_password,
    replace(mongodbatlas_advanced_cluster.franchise.connection_strings[0].standard_srv, "mongodb+srv://", ""),
    var.database_name
  )
}

output "project_id" {
  description = "Id del proyecto creado en Atlas"
  value       = mongodbatlas_project.franchise.id
}
