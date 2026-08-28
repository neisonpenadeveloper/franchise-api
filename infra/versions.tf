# Versiones fijadas para que el plan sea reproducible: un proveedor que cambia
# de version mayor puede alterar el esquema de los recursos.
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    mongodbatlas = {
      source  = "mongodb/mongodbatlas"
      version = "~> 1.29"
    }
  }
}

# Las credenciales llegan por variables de entorno y no se escriben en el codigo:
#   MONGODB_ATLAS_PUBLIC_KEY / MONGODB_ATLAS_PRIVATE_KEY
provider "mongodbatlas" {}
