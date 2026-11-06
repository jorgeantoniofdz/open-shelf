# Etapa 2 — Repositorios e inyección de dependencias

El objetivo de esta etapa es separar _qué hace el dominio_ de _dónde se guardan los datos_. Al terminar, el `LoanService` no sabrá si los datos viven en memoria, en Postgres o en un archivo — solo sabrá que existe algo que puede guardar y recuperar préstamos.

---

## Concepto clave antes de empezar

Un **repositorio** es una interfaz que el dominio define para persistir sus objetos. No sabe cómo se implementa — solo declara qué operaciones necesita.

```
Dominio                    Infraestructura
─────────────────          ───────────────────────────
BookRepository (interfaz)  InMemoryBookRepository (impl)
                           PostgresBookRepository (impl, Etapa 4)
```

El `LoanService` depende de `BookRepository` (la interfaz), nunca de `InMemoryBookRepository` (la implementación). Si mañana cambiás a Postgres, el servicio no cambia.

Esto es el **Dependency Inversion Principle** — los módulos de alto nivel no dependen de los de bajo nivel; ambos dependen de abstracciones.

---

## Orden sugerido

| Paso | Qué crear                      | Por qué en ese orden                         |
| ---- | ------------------------------ | -------------------------------------------- |
| 1    | `BookRepository` (interfaz)    | La más simple, solo tiene ISBN como clave    |
| 2    | `InMemoryBookRepository`       | Primera implementación concreta              |
| 3    | `InMemoryBookRepositoryTest`   | Verificar que la implementación funciona     |
| 4    | `UserRepository` + impl + test | Mismo patrón, practicar la repetición        |
| 5    | `LoanRepository` + impl + test | Agrega queries más complejas                 |
| 6    | `LoanService`                  | Orquesta dominio + repositorios              |
| 7    | `LoanServiceTest`              | Los tests más importantes: reglas de negocio |

---

## 2.1 Interfaces de repositorio

**Estado: completado.** Las tres interfaces y sus implementaciones en memoria ya existen.

Contratos comunes: los argumentos nulos y las claves en blanco se rechazan con
`IllegalArgumentException`; `save` reemplaza por clave; las listas son instantáneas no
modificables, sin orden garantizado. En préstamos, esto no implica copiar las entidades
mutables: los cambios deben persistirse mediante `save`. Las búsquedas no actualizan
automáticamente el estado de vencimiento.

### Dónde viven

Las interfaces van junto a la entidad que representan — son parte del dominio, no de la infraestructura:

```
com.openshelf.domain.book/
  BookRepository.java       ← interfaz
com.openshelf.domain.user/
  UserRepository.java       ← interfaz
com.openshelf.domain.loan/
  LoanRepository.java       ← interfaz
```

### `BookRepository`

```java
public interface BookRepository {
    void save(Book book);
    Optional<Book> findByIsbn(String isbn);
    List<Book> findByAuthor(Author author);
    List<Book> findAll();
    void delete(String isbn);
}
```

### `UserRepository`

```java
public interface UserRepository {
    void save(User user);
    Optional<User> findById(String id);
    List<User> findAll();
}
```

### `LoanRepository`

```java
public interface LoanRepository {
    void save(Loan loan);
    Optional<Loan> findById(String id);
    List<Loan> findByMember(Member member);
    List<Loan> findActiveByMember(Member member);
    List<Loan> findAll();
}
```

> **Sobre `Optional<T>`:** es el reemplazo explícito del `null`. Fuerza al caller a manejar el caso en que no existe el objeto — no puede ignorarlo accidentalmente. Regla de uso: devolvé `Optional` cuando el resultado puede no existir (buscar por ID), nunca cuando siempre existe (guardar).

---

## 2.2 Implementaciones en memoria

**Estado: completado.** Los tres repositorios y sus pruebas están implementados.
Se valida el contrato de cada interfaz, incluidos los filtros por autor y por ID del
miembro, todos los roles de usuario y la distinción entre préstamos activos, vencidos
y devueltos. Las implementaciones guardan referencias y están pensadas para un solo
hilo; las listas devueltas no permiten modificaciones y no reflejan altas o bajas
posteriores del mapa. Sus elementos no se copian en profundidad.

Los siguientes fragmentos son ejemplos didácticos; el código implementado también
valida argumentos mediante `Preconditions`.

### Dónde viven

Las implementaciones son adaptadores de infraestructura:

```
com.openshelf.infrastructure.inmemory/
  InMemoryBookRepository.java
  InMemoryUserRepository.java
  InMemoryLoanRepository.java
```

> La separación por capas ya está aplicada; consulta [architecture.md](architecture.md) para el mapa actual de dependencias.

### `InMemoryBookRepository`

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

    @Override
    public List<Book> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(String isbn) {
        store.remove(isbn);
    }
}
```

Puntos a notar:

- `List.copyOf(...)` devuelve una lista inmutable — el caller no puede modificar el estado interno del repositorio.
- `Optional.ofNullable(...)` convierte el null de `HashMap.get()` en un `Optional`.
- El `Map<String, Book>` usa el ISBN como clave porque es el identificador natural de `Book`.

### `InMemoryLoanRepository`

`findActiveByMember` requiere filtrar — primera oportunidad de usar Streams:

```java
@Override
public List<Loan> findActiveByMember(Member member) {
    return store.values().stream()
        .filter(loan -> loan.getMember().getId().equals(member.getId()) && loan.isActive())
        .toList();
}
```

> **Sobre Streams:** `stream()` crea un pipeline de operaciones sobre una colección. `filter()` conserva solo los elementos que cumplen el predicado. `toList()` materializa el resultado. Es el equivalente Java de `Array.filter()` en JavaScript/TypeScript.

---

## 2.3 LoanService

**Estado: completado.** `LoanService` implementa `checkout` y `returnBook` con inyección
por constructor, validación de argumentos y generación de IDs mediante UUID.
El préstamo conserva la validación de su fecha y el cálculo del vencimiento a 14 días.
El servicio guarda tanto los préstamos nuevos como las devoluciones.

Alcance actual: ejecución en un solo hilo, sin inventario de ejemplares ni generación
automática de multas. Desde la etapa 3.2, las devoluciones guardadas notifican a los
observadores inyectados. El miembro se recibe como entidad; no se exige
su registro previo en `UserRepository`.

### Dónde vive

```
com.openshelf.application.service/
  LoanService.java
```

El servicio vive en `com.openshelf.application.service`, separado del dominio y de los adaptadores de infraestructura.

### Responsabilidades

`LoanService` orquesta — no persiste ni valida campos individuales. Solo implementa reglas de negocio que involucran múltiples objetos:

| Regla                                   | Dónde verificar                                    |
| --------------------------------------- | -------------------------------------------------- |
| El libro existe                         | `bookRepository.findByIsbn(isbn)`                  |
| El miembro no supera `MAX_ACTIVE_LOANS` | contar préstamos no devueltos en `findByMember(member)` |
| El préstamo existe al devolver          | `loanRepository.findById(loanId)`                  |

### Esqueleto

```java
public final class LoanService {
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
    }

    public Loan checkout(Member member, String isbn, LocalDate today) {
        Book book = bookRepository.findByIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Book not found: " + isbn));

        long outstandingLoans = loanRepository.findByMember(member).stream()
            .filter(loan -> loan.getStatus() != LoanStatus.RETURNED)
            .count();
        if (outstandingLoans >= Member.MAX_ACTIVE_LOANS) {
            throw new IllegalStateException("Member has reached the active loan limit");
        }

        Loan loan = new Loan(generateId(), book, member, today);
        loanRepository.save(loan);
        return loan;
    }

    public void returnBook(String loanId) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
        loan.markReturned();
        loanRepository.save(loan);
    }
}
```

> **Sobre `Optional.orElseThrow`:** es la forma idiomática de "si no existe, lanzar excepción". Evitá el patrón `if (result.isPresent()) { ... } else { throw ... }` — es más verbose sin agregar claridad.

> **Sobre `generateId()`:** en Etapa 2 puede ser `UUID.randomUUID().toString()`. En Etapa 4 será el ID autogenerado por la DB.

> **Préstamos vencidos:** `findActiveByMember` devuelve solamente el estado `ACTIVE`.
> Los `OVERDUE` también cuentan para el límite porque el libro aún no fue devuelto.
> La transición `OVERDUE → RETURNED` está habilitada y probada. Una segunda devolución
> se rechaza con `IllegalStateException`.

---

## 2.4 Tests

### `InMemoryBookRepositoryTest`

```
src/test/java/com/openshelf/infrastructure/inmemory/InMemoryBookRepositoryTest.java
```

Qué testear:

| Escenario                               | Verificar                                 |
| --------------------------------------- | ----------------------------------------- |
| `save` + `findByIsbn` → libro existente | `Optional` presente con el libro correcto |
| `findByIsbn` → libro no existente       | `Optional.isEmpty()`                      |
| `save` dos veces el mismo ISBN          | el segundo sobreescribe al primero        |
| `findAll` con repositorio vacío         | lista vacía (no null)                     |
| `findAll` con N libros guardados        | lista de tamaño N                         |
| `delete` → libro existente              | `findByIsbn` devuelve `Optional.empty()`  |
| `delete` → ISBN no existente            | no lanza (operación idempotente)          |

> **El test de `findAll` vacío** es importante: `List.copyOf(store.values())` con un map vacío devuelve `[]`, no `null`. Testear esto previene regresiones si alguien cambia la implementación por `return null`.

### `LoanServiceTest`

**Estado: completado.** Incluye los escenarios de la tabla y pruebas de préstamos
vencidos, combinación de estados para el límite, identidad del miembro por ID,
argumentos inválidos, IDs distintos y recuperación de cupo al devolver un libro.

```
src/test/java/com/openshelf/application/service/LoanServiceTest.java
```

Qué testear:

| Escenario                                           | Verificar                                         |
| --------------------------------------------------- | ------------------------------------------------- |
| `checkout` exitoso                                  | devuelve un `Loan` con status `ACTIVE`            |
| `checkout` con ISBN inexistente                     | `IllegalArgumentException`                        |
| `checkout` cuando miembro llegó al límite           | `IllegalStateException`                           |
| `checkout` con 4 préstamos activos (bajo el límite) | funciona correctamente                            |
| `returnBook` exitoso                                | el loan tiene status `RETURNED`                   |
| `returnBook` con ID inexistente                     | `IllegalArgumentException`                        |
| `returnBook` sobre un loan ya devuelto              | `IllegalStateException` (viene de `markReturned`) |

> **Recomendación sobre el setup:** en `LoanServiceTest`, crear los repositorios directamente con las implementaciones en memoria — no mockear. Los mocks ocultan si los repositorios y el servicio interactúan correctamente. Las implementaciones en memoria son suficientemente simples para usarlas directamente.

```java
class LoanServiceTest {
    private BookRepository bookRepository;
    private LoanRepository loanRepository;
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        bookRepository = new InMemoryBookRepository();
        loanRepository = new InMemoryLoanRepository();
        loanService = new LoanService(loanRepository, bookRepository);
    }
}
```

> **`@BeforeEach`:** se ejecuta antes de cada test, garantizando que cada test empiece con repositorios vacíos y sin estado compartido entre tests.

---

## Conceptos nuevos en esta etapa

| Concepto                                     | Dónde aparece                                             |
| -------------------------------------------- | --------------------------------------------------------- |
| `interface`                                  | `BookRepository`, `UserRepository`, `LoanRepository`      |
| `Optional<T>`                                | retorno de `findBy*` en todas las interfaces              |
| `Map<K, V>` / `HashMap`                      | backing store de las implementaciones en memoria          |
| `List.copyOf`                                | devolver listas inmutables desde los repositorios         |
| Streams (`stream()`, `filter()`, `toList()`) | `findActiveByMember`                                      |
| Constructor injection (DI manual)            | `LoanService` recibe sus dependencias por constructor     |
| `@BeforeEach`                                | setup de tests en `LoanServiceTest`                       |
| `UUID.randomUUID()`                          | generación de IDs en `LoanService`                        |
| Dependency Inversion Principle               | el servicio depende de interfaces, no de implementaciones |
