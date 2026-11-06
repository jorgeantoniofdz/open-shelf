# OpenShelf — Roadmap de aprendizaje Java y OOP

Este documento describe el camino de aprendizaje del proyecto, desde el modelo de dominio inicial
hasta una arquitectura hexagonal completa. Cada etapa tiene objetivos de aprendizaje concretos y
tareas específicas sobre este mismo código.

---

## Estado actual (lo que ya existe)

El código se organiza bajo `src/main/java/com/openshelf/domain/`; los servicios de aplicación y adaptadores se describen en la Etapa 5:

| Clase / Enum                           | Paquete | Concepto que ilustra                |
| -------------------------------------- | ------- | ----------------------------------- |
| `Category`, `Language`                 | `domain.book` | Enum como SSOT de valores válidos   |
| `Author`                               | `domain.book` | Objeto de datos inmutable           |
| `Book` + `Book.Builder`                | `domain.book` | Inmutabilidad + patrón Builder      |
| `UserRole`                             | `domain.user` | Enum como SSOT de roles             |
| `User`                                 | `domain.user` | Clase abstracta, herencia           |
| `Member`, `Librarian`, `Administrator` | `domain.user` | Polimorfismo, subtipos concretos    |
| `LoanStatus`                           | `domain.loan` | Enum con lógica: máquina de estados |
| `Loan`                                 | `domain.loan` | Reglas de negocio en el dominio     |
| `Reservation`, `ReservationStatus`     | `domain.reservation` | Transiciones de estado y encapsulación |
| `Fine`                                 | `domain.fine` | Cálculo monetario con `BigDecimal` |

**Herramientas configuradas:**

```bash
docker compose run --rm app mvn test            # compila + tests (sin linter)
docker compose run --rm app mvn verify          # pipeline completo
docker compose run --rm app mvn fmt:format      # auto-formatea todos los archivos
```

---

## Etapa 1 — Enriquecer el dominio (en memoria)

Objetivo: aprender a escribir objetos de dominio con comportamiento real, no solo datos.

### 1.1 Agregar validación a las entidades — completado

Las entidades validan sus parámetros mediante precondiciones y `Book.Builder.build()` acumula errores antes de fallar. Esta tarea de la etapa quedó completada.

```java
// En Author.java
public Author(String firstName, ...) {
    Objects.requireNonNull(firstName, "firstName must not be null");
    if (firstName.isBlank()) throw new IllegalArgumentException("firstName must not be blank");
    // ...
}
```

Conceptos: diseño por contrato, `Objects.requireNonNull`, `IllegalArgumentException`.

> La validación está implementada en `Author`, `Book.Builder.build()`, `Loan` y `User`.

### 1.2 Escribir los primeros tests con JUnit 5 — completado

Cada regla de negocio debe tener un test. `LoanTest` ya cubre las transiciones y validaciones principales; un ejemplo del estilo de test es:

```java
@Test
void markReturnedTransitionsStatusToReturned() {
    Loan loan = new Loan("1", book, member, LocalDate.now());
    loan.markReturned();
    assertEquals(LoanStatus.RETURNED, loan.getStatus());
}

@Test
void markReturnedThrowsWhenLoanIsAlreadyReturned() {
    Loan loan = new Loan("1", book, member, LocalDate.now());
    loan.markReturned();
    assertThrows(IllegalStateException.class, loan::markReturned);
}
```

Conceptos: `@Test`, `assertEquals`, `assertThrows`, tests como documentación ejecutable.

> Tarea: escribir tests para `LoanStatus.canTransitionTo()`, `Loan.refreshOverdueStatus()`,
> y la validación del Builder de `Book`.

### 1.3 Implementar `equals` y `hashCode` en Author

**Completado:** `Author` implementa `equals`/`hashCode` por nombre, apellido y fecha de
nacimiento. Nacionalidad y biografía no participan de la identidad. `AuthorTest` cubre
el contrato de igualdad, diferencias en cada campo de identidad y el uso en `HashSet`.

La implementación sigue este criterio:

```java
@Override
public boolean equals(Object obj) {
    if (!(obj instanceof Author other)) return false;
    return Objects.equals(firstName, other.firstName)
        && Objects.equals(lastName, other.lastName)
        && Objects.equals(birthDate, other.birthDate);
}

@Override
public int hashCode() {
    return Objects.hash(firstName, lastName, birthDate);
}
```

> Nota: Checkstyle tiene la regla `EqualsHashCode` — si implementás uno, el linter exige el otro.

### 1.4 Agregar `Reservation` y `Fine`

**Completado:** entidades y tests implementados en los paquetes `domain.reservation` y `domain.fine`.

**Reservation** — estados: `PENDING → FULFILLED | CANCELLED`, con `markFulfilled()`
y `cancel()`. Ambos estados finales rechazan nuevas transiciones. El constructor valida
ID, libro, miembro y fecha de reserva. La comprobación de disponibilidad corresponde a
un futuro servicio, cuando exista inventario de ejemplares.

**Fine** — `new Fine(id, loan, calculationDate)` calcula 1 USD por cada día posterior
al vencimiento mediante `BigDecimal` y la constante `DAILY_RATE`. En el vencimiento o
antes, el monto es `0.00`; una fecha anterior al inicio del préstamo es inválida.
Guarda una instantánea inmutable del ID del préstamo, la fecha de cálculo, los días de
retraso y el monto. No modifica el préstamo ni acumula cargos automáticamente.
Para un préstamo devuelto, quien crea la multa debe proporcionar la fecha de devolución;
`Loan` todavía no almacena esa fecha. La selección de estrategias se implementó en
la etapa 3.1; el constructor original sigue usando la tarifa estándar.

Los tests cubren todas las transiciones, parámetros inválidos, límites del vencimiento,
años bisiestos y estabilidad del monto ante cambios en el préstamo.

---

## Etapa 2 — Repositorios e inyección de dependencias (sin framework)

Objetivo: aprender el patrón Repository y la inversión de dependencias.

### 2.1 Definir interfaces de repositorio

**Completado:** `BookRepository`, `UserRepository` y `LoanRepository` están definidos
junto a sus entidades. Sus contratos documentan reemplazo por clave, resultados con
`Optional`, listas no modificables y búsquedas de préstamos por ID del miembro.
`findActiveByMember` filtra exclusivamente el estado `ACTIVE`; el futuro servicio debe
contar también los préstamos `OVERDUE` para aplicar el límite de libros sin devolver.

```java
// src/main/java/com/openshelf/domain/book/BookRepository.java
public interface BookRepository {
    void save(Book book);
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByAuthor(Author author);
    List<Book> findAll();
    void delete(String isbn);
}
```

Mismo patrón para `UserRepository` y `LoanRepository`.

Conceptos: `interface`, `Optional<T>`, Dependency Inversion Principle.

### 2.2 Implementar InMemoryBookRepository

**Completado:** `InMemoryBookRepository`, `InMemoryUserRepository` e
`InMemoryLoanRepository` implementan los contratos en `infrastructure.inmemory`.
Usan `HashMap`, validan argumentos y devuelven listas no modificables. Las pruebas
cubren reemplazos por clave, búsquedas por autor y miembro, filtros por estado,
eliminación idempotente de libros e independencia de las listas respecto del mapa.
Estas implementaciones guardan referencias a las entidades y son para uso en un solo hilo.

```java
public final class InMemoryBookRepository implements BookRepository {
    private final Map<String, Book> store = new HashMap<>();

    @Override
    public void save(Book book) {
        store.put(book.getIsbn(), book);
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return Optional.ofNullable(store.get(isbn));
    }
    // ...
}
```

Conceptos: `HashMap`, `Optional`, implementación de interfaz.

### 2.3 Crear LoanService con reglas de negocio

**Completado:** `application.service.LoanService` recibe `LoanRepository` y `BookRepository` por
constructor. `checkout` exige un libro existente, cuenta préstamos `ACTIVE` y `OVERDUE`
por ID del miembro y guarda un préstamo con UUID y vencimiento a 14 días.
`returnBook` permite devolver préstamos activos o vencidos y persiste el nuevo estado.
La transición `OVERDUE → RETURNED` está habilitada y probada.

`LoanServiceTest` verifica estos casos con repositorios en memoria, incluidos errores,
límites, miembros distintos y recuperación de cupo. El servicio sigue siendo para uso
en un solo hilo; disponibilidad de ejemplares, multas y notificaciones quedan fuera
de esta etapa.

```java
public final class LoanService {
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;

    public LoanService(LoanRepository loans, BookRepository books) {
        this.loanRepository = loans;
        this.bookRepository = books;
    }

    public Loan checkout(Member member, String isbn, LocalDate today) {
        // Regla: máximo 5 préstamos activos
        // Regla: el libro debe existir
        // Regla: crear préstamo y persistir
    }
}
```

Conceptos: constructor injection, SRP (el servicio orquesta, no persiste).

---

## Etapa 3 — Patrones de diseño

Los patrones están ordenados por dificultad. Implementar sobre el código existente.

### 3.1 Strategy — cálculo de multas

**Completado:** `FineCalculationStrategy`, `StandardFineStrategy`, `PremiumFineStrategy`
y `StudentFineStrategy` viven en `domain.fine`. `application.service.FineService` recibe la política por
constructor y `calculate(loan, calculationDate)` devuelve una nueva multa con UUID.
No guarda ni cobra multas, y no selecciona una política según el rol del miembro.

Tarifas diarias en USD:

| Estrategia | Tarifa | Origen |
| ---------- | ------ | ------ |
| Standard | 1,00 | Regla del README |
| Premium | 0,50 | Valor elegido para el ejercicio; no estaba especificado |
| Student | 0,25 | Valor elegido para el ejercicio; no estaba especificado |

Cada estrategia rechaza días negativos. `Fine` calcula los días desde el vencimiento,
evalúa la estrategia una sola vez y guarda el monto como instantánea inmutable. Valida
que el resultado no sea nulo ni negativo, tenga precisión de centavos y sea cero sin
retraso. Normaliza a dos decimales sin redondear. El constructor de tres argumentos y
`Fine.DAILY_RATE` conservan la compatibilidad con el cálculo estándar.

Las pruebas cubren las tres tarifas, cero días, días negativos, valores grandes,
políticas personalizadas, resultados inválidos e inmutabilidad del monto.

El README define tres estrategias de multa. Diseño:

```java
public interface FineCalculationStrategy {
    BigDecimal calculate(long daysOverdue);
}

public final class StandardFineStrategy implements FineCalculationStrategy {
    private static final BigDecimal DAILY_RATE = new BigDecimal("1.00");

    @Override
    public BigDecimal calculate(long daysOverdue) {
        return DAILY_RATE.multiply(BigDecimal.valueOf(daysOverdue));
    }
}
```

Agregar `PremiumFineStrategy` y `StudentFineStrategy` con tasas distintas.
Inyectar la estrategia en `FineService` vía constructor.

Conceptos: Open/Closed Principle, polimorfismo, `BigDecimal` para dinero.

### 3.2 Observer — notificación al devolver un libro

**Completado:** `LoanService.returnBook()` cambia el estado, guarda el préstamo y luego
invoca los `LoanEventListener` inyectados en el constructor. El constructor original
de dos argumentos sigue funcionando sin observadores. La lista de observadores se
copia y se recorre en el orden recibido.

`ReservationNotifier` consulta `ReservationRepository.findPendingByIsbn()` y utiliza
`NotificationService` para avisar a cada miembro pendiente una vez por ID y devolución.
No notifica reservas canceladas, cumplidas ni de otro ISBN. Notificar no cambia una
reserva a `FULFILLED`. `InMemoryReservationRepository` permite guardar, reemplazar y
consultar reservas; sus listas son instantáneas no modificables de referencias.

La publicación ocurre en el servicio, después de persistir, para evitar avisos antes
de guardar la devolución. Llamar directamente a `Loan.markReturned()` no publica
eventos. `Loan` continúa sin conocer reservas ni canales de notificación.

Los observadores se ejecutan sincrónicamente. Un fallo detiene los envíos restantes y
se propaga al llamador; la devolución ya guardada no se revierte. No hay reintentos ni
garantía de entrega. Repetir `returnBook` sobre ese préstamo es inválido y no reenvía
avisos. Una implementación durable requerirá una cola o un outbox.

La interfaz `NotificationService` se adelantó como dependencia mínima del observador;
los canales concretos y la fábrica se implementaron en 3.3. Las pruebas registran los
avisos en memoria, sin enviar mensajes externos.

```java
public interface LoanEventListener {
    void onBookReturned(Loan loan);
}

public final class ReservationNotifier implements LoanEventListener {
    @Override
    public void onBookReturned(Loan loan) {
        // buscar reservas PENDING para este libro y notificar
    }
}
```

Practicar: el Observer desacopla `LoanService` de `ReservationNotifier`; el servicio
solo conoce la interfaz del observador.

### 3.3 Factory — creación de notificaciones

**Completado para la etapa en memoria:** `NotificationFactory.email()`, `sms()` y
`push()` devuelven implementaciones de `NotificationService`. Por defecto escriben
un log con el prefijo `SIMULATED`; no realizan envíos externos.

Cada método tiene una sobrecarga que recibe `NotificationSender`, la dependencia que
entrega o registra el aviso. `EmailNotificationService`, `SmsNotificationService` y
`PushNotificationService` crean una `Notification` inmutable con canal, ID del usuario
y mensaje. Validan argumentos, mantienen el texto original y propagan fallos del
receptor sin reintentos.

El destinatario es un ID lógico en los tres canales. Un adaptador real deberá resolver
el email, teléfono o token; `User` todavía no modela teléfonos ni dispositivos. No se
simula que el ID sea una dirección física. Las integraciones con proveedores externos
quedan fuera de esta etapa.

Ejemplo de conexión en memoria, sin depender de una implementación concreta del canal:

```java
List<Notification> sent = new ArrayList<>();
NotificationService channel = NotificationFactory.email(sent::add);
LoanEventListener observer = new ReservationNotifier(reservations, channel);
LoanService service = new LoanService(loans, books, List.of(observer));
service.returnBook(loanId);
// sent contiene los avisos destinados a los miembros con reservas pendientes.
```

Las pruebas cubren los tres canales, todos los roles de usuario, validaciones, errores
del receptor e integración completa de devolución → observador → fábrica → registro.

```java
public interface NotificationService {
    void notify(User user, String message);
}

public final class NotificationFactory {
    public static NotificationService email() { ... }
    public static NotificationService sms() { ... }
    public static NotificationService push() { ... }
}
```

Conceptos: `interface` para polimorfismo, Factory para desacoplar creación.

---

## Etapa 4 — Persistencia con PostgreSQL

### 4.1 Agregar PostgresBookRepository

**Completado:** `infrastructure.postgres.PostgresBookRepository` implementa todas las
operaciones de `BookRepository` con JDBC y un `DataSource` inyectado. Guarda el libro
completo mediante un upsert atómico, preserva los datos del autor y las categorías,
y cierra los recursos adquiridos. `PersistenceException` conserva la causa JDBC.

El esquema inicial está en `src/main/resources/db/migration/V1__create_books.sql`. Las pruebas con una
base PostgreSQL temporal se ejecutan con el perfil Maven `postgres-it` y el servicio
Compose `integration-tests`. Ver [guía de PostgreSQL](postgres.md) para inicialización,
composición y comandos. Flyway se incorporó en 4.2 y Testcontainers en 4.3.

Misma interfaz `BookRepository`, nueva implementación usando JDBC puro:

```java
public final class PostgresBookRepository implements BookRepository {
    private final DataSource dataSource;

    @Override
    public void save(Book book) {
        // INSERT INTO books (isbn, title, ...) VALUES (?, ?, ...)
    }
}
```

El `LoanService` no cambia en absoluto — solo se inyecta la implementación diferente.
Esto demuestra el Dependency Inversion Principle en acción.

### 4.2 Migraciones con Flyway

**Completado para el catálogo persistido:** Flyway gestiona `V1__create_books.sql`.
`DatabaseMigrator.migrate(dataSource)` valida y aplica migraciones desde Java; el plugin
Maven permite ejecutar `flyway:migrate`, `flyway:info` y `flyway:validate` explícitamente.
Las pruebas JDBC crean sus tablas mediante Flyway y verifican repetición, historial,
checksums y rechazo de esquemas existentes sin versionar. Ver [guía](postgres.md).

La carpeta contiene actualmente:

```
src/main/resources/db/migration/
  V1__create_books.sql
```

Las migraciones de usuarios y préstamos se incorporarán junto con sus repositorios
PostgreSQL; esas entidades todavía se almacenan en memoria.

### 4.3 Testcontainers para tests de integración

**Completado:** `PostgresBookRepositoryIT` usa la extensión JUnit y el módulo PostgreSQL
de Testcontainers. El contenedor se inicia por clase y sus datos de conexión se obtienen
dinámicamente. Cada prueba conserva su propio esquema y ejecuta las migraciones Flyway.
La extensión elimina PostgreSQL al terminar; Ryuk permanece habilitado.

El perfil Maven `postgres-it` sigue ejecutando los tests con Failsafe. Desde Docker:
`docker compose --profile integration run --rm --build integration-tests`.
No hace falta levantar una base manualmente ni configurar credenciales de pruebas.
El build habitual continúa sin requerir acceso a Docker desde Maven.

```java
@Testcontainers
class PostgresBookRepositoryIT {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // Tests que corren contra una DB real en Docker
}
```

---

## Etapa 5 (Bonus) — Arquitectura Hexagonal

**Completado:** el código quedó organizado por capas y puertos/adaptadores. Las entidades,
value objects y contratos de repositorio viven bajo `com.openshelf.domain`; los casos de uso
están en `com.openshelf.application`; las implementaciones en memoria, PostgreSQL y
notificaciones están en `com.openshelf.infrastructure`. `LoanEventListener` pertenece a
application porque es el puerto de salida usado por el caso de uso de préstamos.

```text
com.openshelf/
  domain/          entidades, value objects y contratos
  application/     servicios de aplicación y puertos de salida
  infrastructure/ adaptadores en memoria, PostgreSQL y notificaciones
```

La etapa cubre la separación de capas. La extracción de `ISBN`, `Email` y `Money`, junto
con los eventos de dominio enumerados abajo, queda como trabajo posterior.

Value Objects a extraer:

| Clase actual                  | Value Object |
| ----------------------------- | ------------ |
| `String isbn` en `Book`       | `ISBN`       |
| `String email` en `User`      | `Email`      |
| `BigDecimal amount` en `Fine` | `Money`      |

Eventos de dominio a modelar:

- `BookBorrowedEvent`
- `BookReturnedEvent`
- `FineGeneratedEvent`
- `ReservationCreatedEvent`

---

## Checklist de conceptos OOP cubiertos

| Concepto             | Dónde se practica                             |
| -------------------- | --------------------------------------------- |
| Clases e instancias  | `Book`, `Author`, `Loan`                      |
| Herencia             | `User → Member / Librarian / Administrator`   |
| Polimorfismo         | `User.getRole()`, `FineCalculationStrategy`   |
| Encapsulación        | campos `private final` en todas las entidades |
| Abstracción          | `User` abstracta, interfaces de repositorio   |
| Composición          | `Loan` tiene `Book` y `Member`                |
| Enum como SSOT       | `LoanStatus`, `Category`, `UserRole`          |
| Builder              | `Book.Builder`                                |
| Strategy             | `FineCalculationStrategy`                     |
| Observer             | `LoanEventListener`                           |
| Factory              | `NotificationFactory`                         |
| Repository           | `BookRepository` + implementaciones           |
| Dependency Injection | constructores de servicio                     |

---

## Referencias

- _Effective Java_ (Joshua Bloch) — el estándar de facto para buenas prácticas Java
- _Clean Code_ (Robert Martin) — métodos cortos, nombres expresivos
- _Head First Design Patterns_ — patrones con ejemplos visuales, ideal para primer contacto
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) — el formatter ya la aplica
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/) — referencia de tests
