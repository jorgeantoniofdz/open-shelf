# Tests del dominio

El objetivo es que cada validación tenga al menos un test que falle si la validación desaparece. Un test que siempre pasa no protege nada.

---

## Anatomía de un test en este proyecto

```java
// Convención de nombres: método_escenario_resultadoEsperado
void isValidIsbn13_null_returnsFalse() { ... }
void constructor_blankName_throwsIllegalArgumentException() { ... }
```

### Anotaciones principales

| Anotación                                      | Para qué                                               |
| ---------------------------------------------- | ------------------------------------------------------ |
| `@Test`                                        | Caso único y concreto                                  |
| `@ParameterizedTest` + `@ValueSource`          | Múltiples inputs que deben producir el mismo resultado |
| `assertThrows(ExceptionType.class, () -> ...)` | Verificar que el constructor lanza excepción           |
| `assertTrue` / `assertFalse`                   | Verificar booleanos                                    |
| `assertEquals`                                 | Verificar valores concretos                            |

### Patrón para constructores que validan

```java
@Test
void constructor_blankId_throwsIllegalArgumentException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Member("  ", "Ana López", "ana@example.com"));
}
```

> **Nota:** `User` es abstracta — testear a través de `Member` (el subtipo más simple).

---

## Orden sugerido

| Prioridad | Clase             | Por qué                                   |
| --------- | ----------------- | ----------------------------------------- |
| 1         | `IsbnValidator`   | ✓ Ya implementado — sirve como referencia |
| 2         | `EmailValidator`  | Misma estructura, un solo método estático |
| 3         | `Preconditions`   | Base de todas las demás validaciones      |
| 4         | `Author`          | Primer constructor con múltiples campos   |
| 5         | `Member` / `User` | Herencia — testear por `Member`           |
| 6         | `LoanStatus`      | Máquina de estados con `canTransitionTo`  |
| 7         | `Loan`            | Constructor + métodos de mutación         |
| 8         | `Book.Builder`    | Más complejo: errores acumulados en lista |

---

## `EmailValidator`

Paquete: `src/test/java/com/openshelf/domain/shared/`

| Escenario                   | Entrada                   | Resultado |
| --------------------------- | ------------------------- | --------- |
| Email válido                | `"user@example.com"`      | `true`    |
| Email válido con subdominio | `"user@mail.example.com"` | `true`    |
| Null                        | `null`                    | `false`   |
| Sin arroba                  | `"userexample.com"`       | `false`   |
| Sin dominio                 | `"user@"`                 | `false`   |
| Sin TLD                     | `"user@example"`          | `false`   |
| Solo arroba                 | `"@"`                     | `false`   |

---

## `Preconditions`

Paquete: `src/test/java/com/openshelf/domain/shared/`

Cada método necesita dos grupos: uno que no lanza y otro que sí.

### `checkNotNull`

- Objeto no nulo → no lanza
- Null → `IllegalArgumentException` con el mensaje correcto

### `checkArgument`

- Condición `true` → no lanza
- Condición `false` → `IllegalArgumentException` con el mensaje correcto

### `requireNotBlank`

- String válido → devuelve el mismo string (verificar con `assertEquals`)
- Null → `IllegalArgumentException`
- String vacío `""` → `IllegalArgumentException`
- String en blanco `"   "` → `IllegalArgumentException`

### `requireNotWhitespace`

- String sin espacios → devuelve el mismo string
- Null → `IllegalArgumentException`
- String con espacio en el medio `"ab cd"` → `IllegalArgumentException`
- String con tabulación `"ab\tcd"` → `IllegalArgumentException`

---

## `Author`

Paquete: `src/test/java/com/openshelf/domain/book/`

> **Tip:** crea un método auxiliar `validAuthor()` que devuelva un `Author` con datos correctos. Cada test de error toma ese caso base y cambia solo el campo que quiere romper.

### Happy path

- Constructor con todos los campos válidos → objeto creado sin excepción

### `firstName`

- Null → `IllegalArgumentException`
- Blank → `IllegalArgumentException`
- Exactamente 50 chars → válido (boundary)
- 51 chars → `IllegalArgumentException`

### `lastName`

- Mismos casos que `firstName`

### `birthDate`

- Null → `IllegalArgumentException`
- Fecha futura (`LocalDate.now().plusDays(1)`) → `IllegalArgumentException`
- Fecha pasada (`LocalDate.of(1980, 1, 1)`) → válido
- Fecha de hoy (`LocalDate.now()`) → `IllegalArgumentException` (hoy no es "en el pasado")

### `nationality`

- Null → `IllegalArgumentException`
- Blank → `IllegalArgumentException`

### `biography`

- Null → `IllegalArgumentException`
- String vacío `""` → válido (biografía aún no cargada)

---

## `Member` / `User`

Paquete: `src/test/java/com/openshelf/domain/user/`

Testear a través de `Member` — es el subtipo más simple y hereda todas las validaciones de `User`.

### Happy path

- Constructor con id, name, email válidos → `getRole()` devuelve `UserRole.MEMBER`

### `id`

- Null → `IllegalArgumentException`
- Blank → `IllegalArgumentException`
- Con espacio `"ab cd"` → `IllegalArgumentException`

### `name`

- Null → `IllegalArgumentException`
- Blank → `IllegalArgumentException`
- Exactamente 100 chars → válido
- 101 chars → `IllegalArgumentException`

### `email`

- Email inválido `"not-an-email"` → `IllegalArgumentException`
- Email válido → objeto creado

---

## `LoanStatus`

Paquete: `src/test/java/com/openshelf/domain/loan/`

La máquina de estados tiene transiciones definidas — testear todas las combinaciones.

| Desde      | Hacia      | Resultado |
| ---------- | ---------- | --------- |
| `ACTIVE`   | `RETURNED` | `true`    |
| `ACTIVE`   | `OVERDUE`  | `true`    |
| `ACTIVE`   | `ACTIVE`   | `false`   |
| `RETURNED` | `ACTIVE`   | `false`   |
| `RETURNED` | `OVERDUE`  | `false`   |
| `OVERDUE`  | `ACTIVE`   | `false`   |
| `OVERDUE`  | `RETURNED` | `false`   |

> Usa `@ParameterizedTest` con `@MethodSource` o `@CsvSource` para cubrir la tabla completa sin repetir código.

---

## `Loan`

Paquete: `src/test/java/com/openshelf/domain/loan/`

> **Tip:** crea métodos auxiliares `validMember()`, `validBook()`, y `validAuthor()` para no repetir setup en cada test.

### Constructor

- Todos los campos válidos → `getStatus()` es `ACTIVE`, `getDueDate()` es `loanDate + 14`
- `id` null o blank → `IllegalArgumentException`
- `book` null → `IllegalArgumentException`
- `member` null → `IllegalArgumentException`
- `loanDate` null → `IllegalArgumentException`
- `loanDate` futura → `IllegalArgumentException`
- `loanDate` hoy → válido

### `markReturned`

- Desde `ACTIVE` → status cambia a `RETURNED`
- Desde `RETURNED` → `IllegalStateException`
- Desde `OVERDUE` → `IllegalStateException`

### `refreshOverdueStatus`

- Préstamo activo con fecha pasada la `dueDate` → status cambia a `OVERDUE`
- Préstamo activo con fecha antes de `dueDate` → status permanece `ACTIVE`
- Préstamo ya devuelto → status permanece `RETURNED` (no regresa a OVERDUE)

---

## `Book.Builder`

Paquete: `src/test/java/com/openshelf/domain/book/`

> `Book.Builder` acumula todos los errores antes de lanzar. El mensaje de excepción contiene todos los campos inválidos separados por `"; "`. Esto permite testear que el mensaje **contiene** la descripción del error con `assertThrows` + `getMessage()`.

### Happy path

- Builder con todos los campos válidos → objeto creado

### Errores individuales (verificar mensaje)

- Sin `title` → excepción con "Title cannot be null or blank"
- `title` vacío → excepción con "Title cannot be null or blank"
- `title` > 255 chars → excepción con "Title cannot exceed 255 characters"
- Sin `isbn` → excepción con "ISBN cannot be null or blank"
- ISBN con formato incorrecto → excepción con "ISBN must be a valid ISBN-13 format"
- `publicationDate` nula → excepción
- `publicationDate` futura → excepción
- `pageCount` = 0 → excepción
- `pageCount` negativo → excepción
- Sin `language` → excepción
- Sin `author` → excepción

### Errores múltiples (característica distintiva del Builder)

- Sin `title` ni `isbn` → una sola excepción con ambos mensajes
- Verificar que el mensaje contiene `";"` (hay más de un error)
