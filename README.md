# Franchise API

API reactiva para gestionar **franquicias**, sus **sucursales** y los **productos** ofertados en cada
sucursal. Prueba tecnica de desarrollador backend.

Una franquicia tiene un nombre y una lista de sucursales; una sucursal tiene un nombre y una lista de
productos; un producto tiene un nombre y una cantidad de stock.

---

## Stack

| Pieza | Eleccion | Por que |
|---|---|---|
| Lenguaje | Java 21 | Records y `Optional` encajan con un dominio inmutable |
| Framework | Spring Boot 3.4 + **WebFlux** | Programacion reactiva de extremo a extremo (punto extra) |
| Reactividad | Project Reactor (`Mono` / `Flux`) | Sin hilos bloqueados en ningun punto del flujo |
| Persistencia | **MongoDB** (driver reactivo) | El agregado franquicia -> sucursales -> productos es un documento natural |
| Documentacion | springdoc-openapi (Swagger UI) | Contrato navegable sin escribirlo a mano |
| Tests | JUnit 5, Mockito, StepVerifier, ArchUnit | 52 tests, incluidas 6 reglas de arquitectura |
| Empaquetado | Docker multi-stage + Docker Compose | Punto extra |
| Infraestructura | Terraform (MongoDB Atlas) | Punto extra |

## Arquitectura

Arquitectura hexagonal (puertos y adaptadores) con la regla de dependencia apuntando siempre hacia
adentro: **infraestructura -> aplicacion -> dominio**. El dominio no importa Spring, Mongo ni HTTP.

```
com.accenture.franchise
├── domain                 # Reglas de negocio puras
│   ├── model              # Franchise, Branch, Product (records inmutables)
│   ├── exception          # NotFound, DuplicateName, InvalidData
│   └── port.out           # FranchiseRepositoryPort (contrato de persistencia)
├── application
│   └── usecase            # FranchiseUseCase: orquesta puerto + modelo
└── infrastructure
    ├── adapter.in.web     # Controller REST, DTOs, mapper, manejo de errores
    ├── adapter.out.mongo  # Documentos, repositorio Spring Data, adaptador del puerto
    └── config             # Cableado de beans y metadatos de OpenAPI
```

Decisiones que vale la pena señalar:

- **La franquicia es la raiz del agregado.** Sucursales y productos van embebidos en el mismo
  documento de Mongo: siempre se leen y escriben juntos, asi que cada operacion es una sola escritura
  atomica sin joins.
- **El modelo es inmutable.** Cada operacion devuelve una copia nueva; nada muta en sitio, lo que
  evita sorpresas cuando el objeto viaja entre hilos del flujo reactivo.
- **Las reglas viven en el dominio,** no en el caso de uso ni en el controlador. `FranchiseUseCase`
  solo orquesta: `findById -> aplicar regla -> save`.
- **El caso de uso se declara como `@Bean`** en `infrastructure.config.BeanConfiguration` en vez de
  anotarlo con `@Service`, para que las capas internas no tengan ni una anotacion de Spring.
- **ArchUnit verifica todo lo anterior en cada build.** Un import equivocado rompe los tests.

---

## Como ejecutarlo en local

### Opcion A — Docker Compose (no necesitas Java ni Mongo instalados)

```bash
docker compose up --build
```

Levanta MongoDB y la API; la API espera a que Mongo responda antes de arrancar.

- API: <http://localhost:8080/api/v1/franchises>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>

Para apagarlo todo y borrar los datos: `docker compose down -v`.

### Opcion B — Maven + Mongo local

Requisitos: **JDK 21** y **Maven 3.9+**.

```bash
# 1. Una instancia de Mongo (o usa una propia en el puerto 27017)
docker run -d --name mongo -p 27017:27017 mongo:7

# 2. La aplicacion
mvn spring-boot:run
```

Sin `MONGODB_URI` definida, la aplicacion usa `mongodb://localhost:27017/franchisedb`.

### Variables de entorno

Copia `.env.example` a `.env` y ajusta los valores. `.env` esta en `.gitignore`: **las credenciales no
se suben al repositorio**.

| Variable | Por defecto | Descripcion |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/franchisedb` | Conexion a Mongo (local o Atlas) |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `LOG_LEVEL` | `INFO` | Nivel de log de la aplicacion |

### Tests

```bash
mvn test                 # 52 tests
# Cobertura: target/site/jacoco/index.html
```

---

## Endpoints

Base: `/api/v1/franchises`

| # | Metodo | Ruta | Descripcion |
|---|---|---|---|
| 2 | `POST` | `/` | Agregar una franquicia |
| 3 | `POST` | `/{franchiseId}/branches` | Agregar una sucursal a la franquicia |
| 4 | `POST` | `/{franchiseId}/branches/{branchId}/products` | Agregar un producto a la sucursal |
| 5 | `DELETE` | `/{franchiseId}/branches/{branchId}/products/{productId}` | Eliminar un producto |
| 6 | `PATCH` | `/{franchiseId}/branches/{branchId}/products/{productId}/stock` | Modificar el stock |
| 7 | `GET` | `/{franchiseId}/top-stock-products` | Producto con mas stock por sucursal |
| + | `PATCH` | `/{franchiseId}/name` | Actualizar el nombre de la franquicia |
| + | `PATCH` | `/{franchiseId}/branches/{branchId}/name` | Actualizar el nombre de la sucursal |
| + | `PATCH` | `/{franchiseId}/branches/{branchId}/products/{productId}/name` | Actualizar el nombre del producto |
|   | `GET` | `/` | Listar franquicias |
|   | `GET` | `/{franchiseId}` | Consultar una franquicia |

La columna `#` corresponde al criterio de aceptacion de la prueba; `+` son los puntos extra.

Toda mutacion devuelve la franquicia completa actualizada, para que el cliente vea el estado
resultante sin una segunda peticion.

### Codigos de respuesta

| Codigo | Cuando |
|---|---|
| `201` | Se creo una franquicia, sucursal o producto |
| `200` | Consulta o modificacion correcta |
| `400` | Datos invalidos (nombre vacio, stock negativo, JSON mal formado) |
| `404` | La franquicia, sucursal o producto no existe |
| `409` | Ya existe otro con ese nombre en el mismo ambito |

Los errores comparten un unico formato:

```json
{
  "status": 409,
  "error": "DUPLICATE_NAME",
  "message": "La sucursal ya tiene un producto llamado 'Cafe 500g'",
  "timestamp": "2026-08-27T20:15:00Z"
}
```

### Ejemplo de uso completo

```bash
BASE=http://localhost:8080/api/v1/franchises

# 1. Crear la franquicia
FRANCHISE=$(curl -s -X POST $BASE \
  -H 'Content-Type: application/json' \
  -d '{"name":"Franquicia Centro"}' | jq -r .id)

# 2. Agregar una sucursal
BRANCH=$(curl -s -X POST $BASE/$FRANCHISE/branches \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sucursal Norte"}' | jq -r '.branches[0].id')

# 3. Agregar productos
curl -s -X POST $BASE/$FRANCHISE/branches/$BRANCH/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Cafe 500g","stock":120}'

curl -s -X POST $BASE/$FRANCHISE/branches/$BRANCH/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Te verde","stock":45}'

# 4. Producto con mas stock por sucursal
curl -s $BASE/$FRANCHISE/top-stock-products
```

Respuesta del ultimo llamado:

```json
[
  {
    "branchId": "6b1f...",
    "branchName": "Sucursal Norte",
    "product": { "id": "9c2a...", "name": "Cafe 500g", "stock": 120 }
  }
]
```

---

## Infraestructura como codigo

`infra/` aprovisiona la persistencia en **MongoDB Atlas** con Terraform: proyecto, cluster M0 (capa
gratuita), usuario de base de datos con permiso solo sobre `franchisedb` y lista de IP permitidas.

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars   # completa atlas_org_id

# Credenciales por entorno, nunca en el repositorio
export MONGODB_ATLAS_PUBLIC_KEY='...'
export MONGODB_ATLAS_PRIVATE_KEY='...'
export TF_VAR_db_password='...'

terraform init
terraform plan
terraform apply

# URI lista para la aplicacion
terraform output -raw mongodb_uri
```

Ese valor es el que se pasa como `MONGODB_URI` al desplegar.

---

## Estructura del repositorio

```
franchise-api/
├── src/main/java/...      # Codigo de la aplicacion (dominio / aplicacion / infraestructura)
├── src/test/java/...      # Tests unitarios, de API y de arquitectura
├── infra/                 # Terraform (MongoDB Atlas)
├── Dockerfile             # Imagen multi-stage, usuario sin privilegios, healthcheck
├── docker-compose.yml     # Mongo + API para desarrollo local
└── .env.example           # Plantilla de variables de entorno
```
