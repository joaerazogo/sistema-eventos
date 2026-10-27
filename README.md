# Ticketing Platform — Sistema de Procesamiento de Eventos

Sistema backend reactivo para la gestión de venta de tiquetes para eventos (conciertos, teatro, deportes). Construido con Java 25, Spring Boot 4, Spring WebFlux, DynamoDB y SQS.

La plataforma maneja el ciclo de vida completo de un tiquete: una API REST recibe solicitudes de compra, decrementa el inventario de forma atómica usando optimistic locking en DynamoDB y publica los IDs de orden en SQS. Un procesador separado consume esos mensajes y transiciona las órdenes a través de `RESERVED → PENDING_CONFIRMATION → SOLD`. Un scheduler que corre cada 60 segundos consulta un GSI para encontrar reservas expiradas y restaura el inventario automáticamente. Ambos microservicios comparten las mismas tablas DynamoDB y son desplegables de forma independiente.

---

## Visión General de la Arquitectura

El sistema está dividido en dos microservicios siguiendo **Clean Architecture**:

```
ticketing-service   → API REST (puerto 8080)
ticketing-processor → consumidor SQS + scheduler (puerto 8081)
```

Cada microservicio tiene tres capas:

- **Domain/Model** — entidades de dominio puras e interfaces de gateway (sin dependencias de framework)
- **Domain/UseCase** — lógica de aplicación que orquesta las operaciones de dominio
- **Infrastructure** — adaptadores DynamoDB, sender/listener SQS, handlers REST

```
┌─────────────────────────────────────────────────────────────────┐
│                        ticketing-service                        │
│                                                                 │
│  POST /api/v1/purchases                                         │
│       │                                                         │
│       ▼                                                         │
│  PurchaseTicketUseCase                                          │
│       │  1. updateAvailableTickets (optimistic locking)         │
│       │  2. save Order (status=RESERVED, expiresAt=+10min)      │
│       │  3. publish orderId to SQS                              │
│       ▼                                                         │
│  SQS Queue ──────────────────────────────────────────────────┐  │
└──────────────────────────────────────────────────────────────┼──┘
                                                               │
┌──────────────────────────────────────────────────────────────▼──┐
│                       ticketing-processor                       │
│                                                                 │
│  SQSListener (polling)                                          │
│       │                                                         │
│       ▼                                                         │
│  ProcessOrderUseCase                                            │
│       │  1. load Order by orderId                               │
│       │  2. save Order (status=PENDING_CONFIRMATION)            │
│       │  3. save Order (status=SOLD)                            │
│                                                                 │
│  ReservationExpiryScheduler (every 60s)                         │
│       │                                                         │
│       ▼                                                         │
│  ReleaseExpiredReservationsUseCase                              │
│       │  1. query GSI status-expiresAt-index (RESERVED, ≤now)   │
│       │  2. save Order (status=AVAILABLE)                       │
│       │  3. restore event availableTickets                      │
└─────────────────────────────────────────────────────────────────┘
```

### Máquina de Estados del Tiquete

```
AVAILABLE ──(compra)──► RESERVED ──(consumidor SQS)──► PENDING_CONFIRMATION ──► SOLD
                │                │
                │                └──(asignación administrativa)──► COMPLIMENTARY
                │
                └──(scheduler de expiración, >10min)──► AVAILABLE
```

Los estados `RESERVED` y `PENDING_CONFIRMATION` no son ventas. `SOLD` es final e irreversible. `COMPLIMENTARY` es final pero no facturable.

---

## Decisiones de Diseño Clave

**Optimistic locking en inventario** — `updateAvailableTickets` usa una expresión condicional de DynamoDB (`availableTickets >= :qty`) para decrementar el contador de forma atómica. Las solicitudes concurrentes para el mismo evento fallarán rápido con una `ConditionalCheckFailedException` en lugar de sobrevender.

**Procesamiento de orden en dos fases** — El procesador transiciona `RESERVED → PENDING_CONFIRMATION → SOLD` en dos guardados separados. Esto hace el estado intermedio observable y auditable, y permite la integración futura de confirmación de pago entre los dos pasos.

**Sin re-validación de disponibilidad en el procesador** — El servicio ya garantizó la disponibilidad con optimistic locking al crear la orden. El procesador confía en esa garantía y solo transiciona estados.

**Expiración vía sort key del GSI** — El GSI `status-expiresAt-index` permite consultas de rango eficientes: `status = RESERVED AND expiresAt <= now`. Usar `expiresAt` como sort key (no `reservedAt`) es lo que hace correcta la consulta.

**Cadena de providers de credenciales** — `EnvironmentVariableCredentialsProvider` es el primero, cubriendo tanto Docker Compose (variables de entorno inyectadas por Compose) como LocalStack (configurar `AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test`). La cadena cae hacia `WebIdentityTokenFileCredentialsProvider` (EKS IRSA) y `ContainerCredentialsProvider` (ECS task role) para producción.

**Un solo `application.yaml` por microservicio** — Sin archivos específicos por perfil. Los endpoints apuntan por defecto a `localhost:4566` para desarrollo local. Docker Compose y las configuraciones de ejecución de IntelliJ sobreescriben mediante variables de entorno.

---

## Prerrequisitos

- Docker + Docker Compose
- Java 25 (para build local)
- Gradle (wrapper incluido)

---

## Ejecución con Docker Compose

### 1. Levantar LocalStack

```bash
docker compose up localstack -d
```

Verificar que esté corriendo:

```bash
docker ps
```

Esperar hasta que el estado sea `healthy` antes de continuar.

### 2. Provisionar infraestructura con Terraform

```bash
cd iac/ticketing_service_iac/dynamo
terraform init
terraform apply -var-file="env/dev/terraform-dev.tfvars" -auto-approve
```

```bash
cd ../sqs
terraform init
terraform apply -var-file="env/dev/terraform-dev.tfvars" -auto-approve
```

Esto crea las tablas DynamoDB (`events-dev`, `orders-dev`) y la cola SQS con su DLQ en LocalStack.

### 3. Compilar los JARs

Los Dockerfiles copian el JAR desde `build/libs/`, por lo que hay que compilar antes de levantar los contenedores:

```bash
cd ../../..
cd ms/ticketing-service
./gradlew build -x test

cd ../ticketing-processor
./gradlew build -x test

cd ../..
```

### 4. Levantar los microservicios

```bash
docker compose up ticketing-service ticketing-processor --build -d
```

Verificar que los tres contenedores estén corriendo:

```bash
docker ps
```

Deben aparecer: `ticketing-localstack`, `ticketing-service`, `ticketing-processor`.

### 5. Verificar salud

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

Ambos deben retornar `{"status":"UP"}`.

### 6. Detener los servicios

```bash
docker compose down
```

---

## Ejecución Local (IntelliJ / IDE)

Levantar LocalStack por separado (por ejemplo vía WSL o Docker Desktop), luego agregar estas variables de entorno a cada configuración de ejecución:

**ticketing-service:**
```
AWS_DYNAMODB_ENDPOINT=http://<localstack-host>:4566
ADAPTER_SQS_ENDPOINT=http://<localstack-host>:4566
ADAPTER_SQS_QUEUE_URL=http://<localstack-host>:4566/000000000000/ticketing-queue-dev
```

**ticketing-processor:**
```
AWS_DYNAMODB_ENDPOINT=http://<localstack-host>:4566
ENTRYPOINT_SQS_ENDPOINT=http://<localstack-host>:4566
ENTRYPOINT_SQS_QUEUE_URL=http://<localstack-host>:4566/000000000000/ticketing-queue-dev
```

> Si LocalStack corre en WSL, reemplazar `<localstack-host>` con la IP de WSL (por ejemplo `172.26.53.218`). La IP puede cambiar al reiniciar — verificar con `ip addr show eth0` dentro de WSL.

---

## Ejecución de Pruebas

```bash
# ticketing-service
cd ms/ticketing-service
./gradlew test

# ticketing-processor
cd ../ticketing-processor
./gradlew test
```

---

## Endpoints de la API

URL base: `http://localhost:8080`

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/events` | Crear un evento |
| `GET` | `/api/v1/events` | Listar todos los eventos |
| `GET` | `/api/v1/events/{eventId}/availability` | Consultar disponibilidad del evento |
| `POST` | `/api/v1/purchases` | Comprar tiquetes (crea orden en estado RESERVED) |
| `POST` | `/api/v1/orders/{orderId}/complimentary` | Asignar orden como cortesía (RESERVED → COMPLIMENTARY) |
| `GET` | `/api/v1/orders/{orderId}` | Consultar estado de una orden |

Ver `requests/ticketing.curl` para ejemplos listos para ejecutar, importables en Postman vía **Import → Raw text**:

```bash
# Crear evento
curl -s -X POST http://localhost:8080/api/v1/events \
  -H 'Content-Type: application/json' \
  -d '{"name":"Rock Concert","venue":"National Stadium","date":"2026-12-01T20:00:00Z","totalCapacity":100}'

# Listar eventos
curl -s http://localhost:8080/api/v1/events

# Consultar disponibilidad  (reemplazar <eventId> con el id retornado arriba)
curl -s http://localhost:8080/api/v1/events/<eventId>/availability

# Comprar tiquetes
curl -s -X POST http://localhost:8080/api/v1/purchases \
  -H 'Content-Type: application/json' \
  -d '{"eventId":"<eventId>","userId":"user-001","quantity":2}'

# Asignar cortesía  (reemplazar <orderId> con una orden en estado RESERVED)
curl -s -X POST http://localhost:8080/api/v1/orders/<orderId>/complimentary

# Consultar estado de orden  (reemplazar <orderId> con el id retornado arriba)
curl -s http://localhost:8080/api/v1/orders/<orderId>
```

---

## Diagramas

Todos los diagramas están en `docs/` y pueden abrirse con [draw.io](https://app.diagrams.net) (File → Open from → Device).

| Archivo | Descripción |
|---------|-------------|
| `docs/architecture.drawio` | Arquitectura de alto nivel: clientes, microservicios, SQS + DLQ, DynamoDB, Secrets Manager y máquina de estados |
| `docs/seq-ticketing-service.drawio` | Diagrama de secuencia — los 5 endpoints del ticketing-service con interacciones DynamoDB y SQS |
| `docs/seq-ticketing-processor.drawio` | Diagrama de secuencia — consumidor SQS (RESERVED→PENDING_CONFIRMATION→SOLD, redrive a DLQ) y scheduler de expiración (consulta GSI + restauración de inventario) |

---

## Infraestructura como Código (Terraform)

Ubicado en `iac/ticketing_service_iac/`:

- `dynamo/` — tablas DynamoDB (`events-dev`, `orders-dev`) con GSIs
- `sqs/` — cola SQS con DLQ y redrive policy

```bash
cd iac/ticketing_service_iac/dynamo
terraform init
terraform apply -var-file=env/dev/terraform.tfvars

cd ../sqs
terraform init
terraform apply -var-file=env/dev/terraform.tfvars
```

El provider de Terraform está configurado para apuntar a LocalStack (`http://localhost:4566`) con credenciales dummy, coincidiendo con el setup de desarrollo local.

---

## Diccionario de Datos

### Tabla `events-dev`

Clave primaria: `eventId` (String)

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| `eventId` | String (PK) | UUID generado al crear el evento |
| `name` | String | Nombre del evento |
| `venue` | String | Lugar del evento. GSI PK de `venue-date-index` |
| `date` | String | Fecha del evento en formato ISO 8601 (UTC). GSI SK de `venue-date-index` |
| `totalCapacity` | Number | Capacidad total de tiquetes del evento |
| `availableTickets` | Number | Tiquetes disponibles para venta. Decrementado con optimistic locking en cada compra |

GSIs:
- `venue-date-index` — PK: `venue`, SK: `date`. Usado para validar duplicados de evento por lugar y fecha.

### Tabla `orders-dev`

Clave primaria: `orderId` (String)

| Atributo | Tipo | Descripción |
|----------|------|-------------|
| `orderId` | String (PK) | UUID generado al crear la orden |
| `eventId` | String | Referencia al evento asociado |
| `userId` | String | Identificador del usuario que realizó la compra |
| `quantity` | Number | Cantidad de tiquetes comprados |
| `status` | String | Estado actual de la orden. GSI PK de `status-expiresAt-index`. Valores: `RESERVED`, `PENDING_CONFIRMATION`, `SOLD`, `AVAILABLE`, `COMPLIMENTARY` |
| `reservedAt` | String | Timestamp ISO 8601 de cuando se creó la reserva |
| `expiresAt` | String | Timestamp ISO 8601 de expiración de la reserva (`reservedAt + 10min`). GSI SK de `status-expiresAt-index` |

GSIs:
- `status-expiresAt-index` — PK: `status`, SK: `expiresAt`. Usado por el scheduler para consultar reservas expiradas: `status = RESERVED AND expiresAt <= now`.

---

## Consideraciones o mejoras adicionales

**Qué cambiaría en un entorno AWS real:**

- Sin credenciales hardcodeadas — `EnvironmentVariableCredentialsProvider` ya es el primero en la cadena; usar IRSA (EKS) o task roles (ECS) en producción
- Reemplazar los endpoints de LocalStack con endpoints regionales reales de AWS (en proyecto real)
- Habilitar point-in-time recovery y cifrado en reposo en DynamoDB
- Agregar cifrado server-side en SQS (SSE-SQS o SSE-KMS)
- Configurar `PERSISTENCE=1` en LocalStack solo para dev; producción usa AWS real
- Agregar trazabilidad distribuida (X-Ray)
- Agregar logging estructurado con correlation IDs

**Limitaciones conocidas:**

- El scheduler de expiración corre en proceso — en un despliegue multi-instancia, múltiples instancias competirían por liberar las mismas reservas. Se necesitaría un lock distribuido (por ejemplo escritura condicional en DynamoDB sobre el status) o un servicio scheduler dedicado.
- Sin autenticación/autorización en la API REST — en producción, agregar un API Gateway con Cognito o un filtro JWT.
