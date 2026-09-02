# Franchise API

[![CI](https://github.com/neisonpenadeveloper/franchise-api/actions/workflows/ci.yml/badge.svg)](https://github.com/neisonpenadeveloper/franchise-api/actions/workflows/ci.yml)

API reactiva para gestionar **franquicias**, sus **sucursales** y los **productos** ofertados en cada
sucursal. Prueba tecnica de desarrollador backend.

Una franquicia tiene un nombre y una lista de sucursales; una sucursal tiene un nombre y una lista de
productos; un producto tiene un nombre y una cantidad de stock.

## Demo en vivo

La solucion esta desplegada y funcionando contra MongoDB Atlas:

| | |
|---|---|
| Consola de demo | <https://franchise-api-di1g.onrender.com/> |
| API | <https://franchise-api-di1g.onrender.com/api/v1/franchises> |
| Documentacion interactiva | <https://franchise-api-di1g.onrender.com/swagger-ui.html> |
| Health | <https://franchise-api-di1g.onrender.com/actuator/health> |

> **Primera peticion lenta:** el plan gratuito de Render suspende el servicio tras 15 minutos sin
> trafico. La primera llamada despues de ese tiempo tarda unos 50 segundos mientras el contenedor
> vuelve a arrancar; las siguientes responden con normalidad. No es un problema de la aplicacion.

---

## Stack

| Pieza | Eleccion | Por que |
|---|---|---|
| Lenguaje | Java 21 | Records y `Optional` encajan con un dominio inmutable |
| Framework | Spring Boot 3.4 + **WebFlux** | Programacion reactiva de extremo a extremo (punto extra) |
| Reactividad | Project Reactor (`Mono` / `Flux`) | Sin hilos bloqueados en ningun punto del flujo |
| Persistencia | **MongoDB** (driver reactivo) | El agregado franquicia -> sucursales -> productos es un documento natural |
| Documentacion | springdoc-openapi (Swagger UI) | Contrato navegable sin escribirlo a mano |
| Tests | JUnit 5, Mockito, StepVerifier, ArchUnit, Mongo embebido | 60 tests: dominio, casos de uso, contrato HTTP, 6 reglas de arquitectura y 5 de integracion |
| Empaquetado | Docker multi-stage + Docker Compose | Punto extra |
| Infraestructura | Terraform (MongoDB Atlas) | Punto extra |
| Despliegue | Render (contenedor Docker) + Atlas | Punto extra: la solucion corre en la nube |

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
- **Las escrituras simultaneas no se pisan.** Modificar una franquicia es leerla, aplicar la regla y
  volver a guardarla; entre esos dos pasos otra peticion puede escribir. El documento lleva una
  version (`@Version`) que Mongo comprueba al guardar: si otra escritura se adelanto, el guardado
  falla en lugar de borrar el cambio ajeno y el caso de uso reintenta la operacion completa sobre el
  estado ya actualizado. Ver [Concurrencia](#concurrencia).
- **ArchUnit verifica todo lo anterior en cada build.** Un import equivocado rompe los tests.

---

## Como ejecutarlo en local

### Opcion A — Docker Compose (no necesitas Java ni Mongo instalados)

```bash
docker compose up --build
```

Levanta MongoDB y la API; la API espera a que Mongo responda antes de arrancar.

- Consola de demo: <http://localhost:8080/>
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

Docker Compose lee ese `.env` por si mismo. Al ejecutar con Maven no se lee automaticamente, asi que
las variables se exportan antes de arrancar:

```bash
# Linux / macOS
export $(grep -v '^#' .env | xargs) && mvn spring-boot:run

# Windows PowerShell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $k, $v = $_ -split '=', 2; [Environment]::SetEnvironmentVariable($k.Trim(), $v.Trim())
}
mvn spring-boot:run
```

| Variable | Por defecto | Descripcion |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/franchisedb` | Conexion a Mongo (local o Atlas) |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `LOG_LEVEL` | `INFO` | Nivel de log de la aplicacion |

### Tests

```bash
mvn test                 # 60 tests
# Cobertura: target/site/jacoco/index.html
```

La piramide de tests tiene cuatro niveles:

| Nivel | Que verifica | Como |
|---|---|---|
| Dominio | Las reglas de negocio | JUnit puro, sin Spring |
| Aplicacion | La orquestacion del caso de uso | Mockito sobre el puerto + StepVerifier |
| Contrato HTTP | Rutas, codigos y traduccion de errores | `@WebFluxTest` con el caso de uso simulado |
| Integracion | La aplicacion entera contra Mongo | `@SpringBootTest` + Mongo embebido |

El test de integracion cubre lo que ningun mock puede: que el mapeo a documento y de vuelta no
pierda datos, que las consultas que Spring Data deriva del nombre del metodo hagan lo que prometen,
que el indice unico exista de verdad y que el bloqueo optimista funcione contra el motor. Arranca un
`mongod` real en un puerto libre; se prefirio a Testcontainers porque **no exige tener Docker**, asi
que la build corre igual en local que en CI.

Cada push a `main` y cada pull request ejecutan los tests y construyen la imagen de Docker en GitHub
Actions (`.github/workflows/ci.yml`), asi que el estado del badge refleja el de la rama.

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
| `409` | Ya existe otro con ese nombre en el mismo ambito, o dos escrituras simultaneas chocaron |

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

## Consola de demo

En la raiz (`/`) la aplicacion sirve una pagina que permite ejercitar los siete criterios de
aceptacion desde el navegador: crear franquicias, agregar sucursales y productos, cambiar el stock,
renombrar, eliminar y calcular el producto con mas stock por sucursal. Cada accion dispara una
peticion real y la pagina muestra el metodo, la ruta y el codigo de respuesta, de modo que se ve que
detras hay una API REST y no datos inventados en el cliente.

Es **un unico archivo estatico** (`src/main/resources/static/index.html`) sin frameworks, sin
dependencias externas y sin paso de compilacion. Se hizo asi a proposito: la prueba es de backend,
de modo que la consola no debia convertirse en un segundo proyecto que mantener y desplegar. Al ser
un recurso estatico que Spring Boot ya publica, no toca la arquitectura hexagonal ni agrega ninguna
dependencia al `pom.xml`.

---

## Concurrencia

Una franquicia se modifica leyendo el agregado completo, aplicando la regla en el dominio y volviendo
a guardarlo. Entre la lectura y la escritura hay una ventana: si dos peticiones agregan un producto a
la misma sucursal a la vez, las dos leen el mismo documento y la segunda escritura sobrescribe a la
primera. El producto de la primera desaparece **sin ningun error**, que es la peor forma de fallar.

La solucion es **bloqueo optimista**, no un bloqueo de base de datos:

1. `FranchiseDocument` lleva un campo `@Version`. Spring Data lo incrementa en cada guardado y anade
   la version esperada a la condicion del update.
2. Si otra escritura se adelanto, la condicion no encuentra el documento y Mongo falla la escritura
   en lugar de aplicarla. El adaptador traduce esa excepcion de Spring a
   `ConcurrentUpdateException`, del dominio, para que las capas internas no conozcan el framework.
3. `FranchiseUseCase` reintenta la **operacion entera** (hasta 3 veces, con espera creciente y
   aleatoria de 20/40/80 ms). Al repetirse se vuelve a leer la franquicia, ya con el cambio de la
   otra peticion incluido, y la regla se aplica sobre el estado actual. La espera es reactiva: no
   bloquea ningun hilo.
4. Si el conflicto persiste tras los reintentos, la API responde `409 CONCURRENT_UPDATE` en vez de
   fingir que la operacion funciono.

Se eligio optimista y no pesimista porque el conflicto es raro: bloquear el documento en cada
escritura costaria en todas las peticiones para protegerse de un caso que casi nunca ocurre.

El caso duplicado del **nombre de franquicia** se cubre aparte, con el indice unico de Mongo: dos
creaciones simultaneas pueden pasar las dos la comprobacion previa, y es el indice el que garantiza
que solo una se guarde. El error resultante se traduce tambien a `409`.

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

## Despliegue en la nube

```
  Cliente  ──HTTPS──►  Render (contenedor Docker)  ──mongodb+srv──►  MongoDB Atlas M0
                       franchise-api                                 franchisedb
```

- **Aplicacion:** Render construye la imagen a partir del `Dockerfile` del repositorio en cada push a
  `main`, y el servicio queda descrito como codigo en `render.yaml` (plan, runtime, healthcheck).
- **Base de datos:** cluster M0 de MongoDB Atlas, con un usuario limitado a `readWrite` sobre
  `franchisedb`.
- **Credenciales:** `MONGODB_URI` se declara en `render.yaml` con `sync: false`, de modo que el valor
  se define una sola vez en el panel de Render y nunca viaja en el repositorio.

La aplicacion escucha el puerto que la plataforma inyecta en `PORT`, con `SERVER_PORT` como
alternativa local; un puerto fijo dejaria el servicio inaccesible en Render.

Para reproducir el despliegue: crear el cluster con el Terraform de `infra/`, y en Render usar
`New +` -> `Blueprint` apuntando al repositorio, indicando `MONGODB_URI` cuando lo solicite.

---

## Estructura del repositorio

```
franchise-api/
├── src/main/java/...      # Codigo de la aplicacion (dominio / aplicacion / infraestructura)
├── src/main/resources/static/index.html   # Consola de demo (un solo archivo, sin frameworks)
├── src/test/java/...      # Tests unitarios, de API y de arquitectura
├── infra/                 # Terraform (MongoDB Atlas)
├── render.yaml            # Blueprint del servicio en Render
├── Dockerfile             # Imagen multi-stage, usuario sin privilegios, healthcheck
├── docker-compose.yml     # Mongo + API para desarrollo local
└── .env.example           # Plantilla de variables de entorno
```
