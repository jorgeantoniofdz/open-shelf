# Arquitectura técnica

OpenShelf es una aplicación Java 21 organizada en capas con dependencias orientadas
hacia el dominio. El proyecto usa Maven, PostgreSQL para persistir el catálogo y
repositorios en memoria para las demás entidades.

## Estructura del código

```text
src/main/java/com/openshelf/
├── Main.java
├── domain/          entidades, reglas, value objects y contratos
├── application/     servicios de aplicación y puertos de salida
└── infrastructure/ adaptadores concretos de persistencia y notificación
```

### Dominio — `com.openshelf.domain`

- `book`: `Book`, `Author`, categorías, idiomas y `BookRepository`.
- `user`: usuarios, roles y `UserRepository`.
- `loan`: `Loan`, sus estados y `LoanRepository`.
- `reservation`: `Reservation`, estados y `ReservationRepository`.
- `fine`: `Fine` y estrategias para calcular multas.
- `notification`: modelo de notificación y contratos `NotificationService` y
  `NotificationSender`.
- `shared`: precondiciones y validadores de email e ISBN.

Los contratos de repositorio son puertos del dominio. Sus implementaciones no viven
en esta capa.

### Aplicación — `com.openshelf.application`

- `service.LoanService`: coordina checkout, devolución, persistencia y notificación a
  observadores; recibe repositorios por constructor.
- `service.FineService`: aplica una `FineCalculationStrategy` recibida por constructor.
- `loan.LoanEventListener`: puerto para observar devoluciones persistidas.
- `reservation.ReservationNotifier`: adaptador de aplicación que reacciona a una
  devolución y notifica a miembros con reservas pendientes.

### Infraestructura — `com.openshelf.infrastructure`

- `inmemory`: adaptadores en memoria de libros, usuarios, préstamos y reservas.
- `postgres`: `PostgresBookRepository`, manejo de excepciones JDBC y `DatabaseMigrator`.
- `notification`: adaptadores de email, SMS y push simulados, más `NotificationFactory`.

La fábrica configura simulaciones que escriben en el log o acepta un `NotificationSender`
para integrar una entrega o grabación distinta. No hay integración real con SMTP, SMS,
push, HTTP ni una interfaz gráfica.

## Dirección de dependencias

```text
application ───────► domain
infrastructure ────► domain
```

El dominio no depende de JDBC, PostgreSQL, Docker ni frameworks. La composición de
dependencias todavía es manual: `Main` solo registra que la aplicación inició y no crea
repositorios ni servicios. Las pruebas ensamblan dependencias directamente.

## Persistencia y migraciones

`PostgresBookRepository` implementa `BookRepository`. Actualmente es el único agregado
persistido en PostgreSQL; usuarios, préstamos y reservas usan adaptadores en memoria.
La migración inicial está en
[`src/main/resources/db/migration/V1__create_books.sql`](../src/main/resources/db/migration/V1__create_books.sql).
Flyway se ejecuta explícitamente con `mvn flyway:migrate`; `mvn verify` no migra la base
de desarrollo. Detalles de esquema, baseline y Testcontainers están en
[`postgres.md`](postgres.md).

## Build y ejecución local

Requiere Docker Compose. El servicio `app` tiene Java 21 y Maven; los comandos montan el
repositorio y conservan dependencias en el volumen `maven-cache`.

```bash
# Compilar sin ejecutar pruebas
docker compose run --rm app mvn -DskipTests compile

# Suite unitaria y verificadores configurados
docker compose run --rm app mvn verify

# Pipeline de integración PostgreSQL/Testcontainers (requiere Docker accesible)
docker compose --profile integration run --rm --build integration-tests

# Formatear fuentes Java
docker compose run --rm app mvn fmt:format
```

Para migrar una base PostgreSQL de desarrollo, copiar `.env.example` a `.env`, ajustar
las credenciales y consultar [`postgres.md`](postgres.md). El servicio `app` no espera ni
levanta PostgreSQL automáticamente.

## Alcance y extensiones pendientes

- El flujo actual es sincrónico y está pensado para un solo hilo.
- No existe inventario de copias físicas ni asignación de una copia a una reserva.
- No se persisten usuarios, préstamos, reservas ni multas en PostgreSQL.
- `ISBN`, `Email` y `Money` siguen siendo tipos primitivos/`BigDecimal`; su extracción
  como value objects queda pendiente.
- La devolución usa un puerto de aplicación (`LoanEventListener`), pero todavía no hay
  eventos de dominio independientes ni publicación asíncrona.
- No hay wiring de producción en `Main`.

## Documentación relacionada

- [`roadmap.md`](roadmap.md): etapas de aprendizaje y estado de las funcionalidades.
- [`repositories.md`](repositories.md): contratos, adaptadores y reglas de repositorio.
- [`validations.md`](validations.md): invariantes y validaciones del dominio.
- [`tests.md`](tests.md): convención y escenarios de pruebas.
- [`postgres.md`](postgres.md): JDBC, Flyway y pruebas de integración.
