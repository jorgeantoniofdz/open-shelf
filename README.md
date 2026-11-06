# OpenShelf

OpenShelf es un sistema de gestión de bibliotecas pensado para practicar modelado de dominio, programación orientada a objetos y patrones de diseño en Java.

## Descripción

El sistema permite administrar:

- Catálogo de libros.
- Usuarios.
- Préstamos.
- Reservas.
- Multas.
- Notificaciones.
- Historial de actividad.

El objetivo principal es modelar correctamente el dominio aplicando OOP.

## MVP: versión 1

### Gestión de libros

El sistema debe permitir:

- Crear libros.
- Actualizar libros.
- Eliminar libros.
- Buscar libros.

Cada libro debe contener:

- Título.
- ISBN.
- Fecha de publicación.
- Cantidad de páginas.
- Idioma.

La búsqueda de libros debe poder realizarse por:

- Título.
- Autor.
- ISBN.
- Categoría.

### Gestión de autores

El sistema debe permitir crear autores con los siguientes datos:

- Nombre.
- Apellido.
- Fecha de nacimiento.
- Nacionalidad.
- Biografía.

Relación esperada:

```text
Author
|
+---- Book
```

Un autor puede tener muchos libros.

### Gestión de categorías

Categorías iniciales:

- Programming.
- Science.
- History.
- Fantasy.
- Finance.

Un libro puede pertenecer a varias categorías.

### Gestión de usuarios

Tipos de usuario:

- Member.
- Librarian.
- Administrator.

Aquí se puede practicar herencia o modelado basado en roles.

Ejemplo:

```java
abstract class User
class Member extends User
class Librarian extends User
class Administrator extends User
```

### Sistema de préstamos

Reglas:

- Un usuario puede tener como máximo 5 libros prestados.
- La duración de un préstamo es de 14 días.

Entidad principal:

- Loan.

Campos:

- id.
- book.
- member.
- loanDate.
- dueDate.
- status.

Estados:

- ACTIVE.
- RETURNED.
- OVERDUE.

Cuando un libro se devuelve, el préstamo pasa de `ACTIVE` a `RETURNED`.

Cuando `today > dueDate`, el préstamo pasa a `OVERDUE`.

### Sistema de reservas

Las reservas se crean cuando no hay copias disponibles de un libro.

Entidad principal:

- Reservation.

Estados:

- PENDING.
- FULFILLED.
- CANCELLED.

### Sistema de multas

Entidad principal:

- Fine.

Regla inicial:

- 1 USD por cada día de retraso.

Ejemplo:

```text
5 días tarde = 5 USD
```

### Sistema de notificaciones

Interfaz:

```java
interface NotificationService
```

Implementaciones:

- EmailNotificationService.
- SmsNotificationService.
- PushNotificationService.

Este módulo permite practicar:

- Polimorfismo.
- Dependency Injection.
- Open/Closed Principle.

### Reportes

Reportes esperados:

- Top 10 de libros más prestados.
- Usuarios con multas.
- Libros vencidos.
- Libros disponibles.

## Persistencia

### Primera etapa

La primera etapa debe funcionar en memoria.

Colecciones esperadas:

```java
List<Book>
List<User>
List<Loan>
```

### Segunda etapa

Incorporar repositorios.

Interfaz de ejemplo:

```java
interface BookRepository
```

Implementaciones esperadas:

- InMemoryBookRepository.
- PostgresBookRepository.

Aquí se practican:

- Dependency Inversion.
- Repository Pattern.

## Patrones de diseño implementados

### Builder

- BookBuilder.

### Factory

- NotificationFactory.

### Strategy

Aplicado al cálculo de multas.

Interfaz o abstracción esperada:

- FineCalculationStrategy.

Estrategias posibles:

- StandardFineStrategy.
- PremiumFineStrategy.
- StudentFineStrategy.

### Observer

Cuando un libro se devuelve, se dispara una notificación a los usuarios en espera.

## Arquitectura actual

El código está separado en tres capas:

- `com.openshelf.domain`: entidades, reglas de negocio y contratos de repositorio.
- `com.openshelf.application`: servicios de aplicación y puertos de salida.
- `com.openshelf.infrastructure`: adaptadores en memoria, PostgreSQL y notificaciones.

La composición de dependencias es manual y `Main` todavía no inicia un flujo de negocio.
Actualmente PostgreSQL persiste el catálogo; usuarios, préstamos y reservas se manejan
en memoria. Consulta la [documentación técnica](doc/architecture.md) para límites,
migraciones y detalles de la estructura.

La extracción de `ISBN`, `Email` y `Money` como value objects, y el modelado de eventos
de dominio independientes, quedan como extensiones posteriores.

## Stack actual

- Java 21.
- JUnit 5.
- Maven.
- PostgreSQL.
- Docker.
- Flyway para migraciones.
- Testcontainers para pruebas de integración.

Como trabajo posterior quedan los value objects `ISBN`, `Email` y `Money`, además de
eventos de dominio independientes.

## Entorno de desarrollo

El proyecto puede compilarse y probarse sin instalar Java ni Maven localmente usando
Docker Compose. Los comandos y los límites de cada servicio están descritos en
[`doc/architecture.md`](doc/architecture.md).

### Comandos con Docker Compose

Levantar PostgreSQL:

```bash
docker compose up -d postgres
```

Ejecutar Maven dentro del contenedor:

```bash
docker compose run --rm app mvn test
```

Compilar sin ejecutar pruebas y ejecutar el pipeline de verificaciones:

```bash
docker compose run --rm app mvn -DskipTests compile
docker compose run --rm app mvn verify
```

Ejecutar las pruebas de integración PostgreSQL con Testcontainers:

```bash
docker compose --profile integration run --rm --build integration-tests
```

Consultar [doc/postgres.md](doc/postgres.md) para la configuración de la base local,
migraciones Flyway y requisitos del perfil de integración.

Ejecutar cualquier comando Maven:

```bash
docker compose run --rm app mvn package
```

Abrir una shell dentro del contenedor con Java 21 y Maven disponibles:

```bash
docker compose run --rm app bash
```

Detener los servicios:

```bash
docker compose down
```
