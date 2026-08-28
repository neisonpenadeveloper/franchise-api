# Aprovisiona la persistencia de la aplicacion en MongoDB Atlas:
# proyecto, cluster, usuario de base de datos y lista de IP permitidas.

resource "mongodbatlas_project" "franchise" {
  name   = var.project_name
  org_id = var.atlas_org_id
}

# Cluster compartido M0 (capa gratuita). "TENANT" indica capa compartida y
# backing_provider_name el proveedor real que la respalda.
resource "mongodbatlas_advanced_cluster" "franchise" {
  project_id   = mongodbatlas_project.franchise.id
  name         = var.cluster_name
  cluster_type = "REPLICASET"

  replication_specs {
    region_configs {
      priority              = 7
      provider_name         = "TENANT"
      backing_provider_name = var.cloud_provider
      region_name           = var.region

      electable_specs {
        instance_size = "M0"
      }
    }
  }
}

# Usuario de la aplicacion, con permiso solo sobre su propia base de datos.
resource "mongodbatlas_database_user" "app" {
  project_id         = mongodbatlas_project.franchise.id
  username           = var.db_username
  password           = var.db_password
  auth_database_name = "admin"

  roles {
    role_name     = "readWrite"
    database_name = var.database_name
  }
}

resource "mongodbatlas_project_ip_access_list" "allowed" {
  for_each = toset(var.allowed_cidr_blocks)

  project_id = mongodbatlas_project.franchise.id
  cidr_block = each.value
  comment    = "Acceso a franchise-api"
}
