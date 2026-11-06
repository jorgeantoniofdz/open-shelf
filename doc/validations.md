# Validaciones del dominio

Lista de validaciones necesarias por entidad. El objetivo es que ningún objeto del dominio pueda existir en un estado inválido — si un constructor o método acepta datos incorrectos sin quejarse, el error se descubre tarde y lejos del origen.

---

## Dónde vive la validación

Antes de ver las listas: todas las validaciones del dominio van **en el constructor o en
`Builder.build()`**, no en un servicio externo. Un objeto inválido nunca debe poder
construirse.

### Paquete sugerido: `com.openshelf.domain.shared`

Para evitar duplicar lógica de validación en cada entidad, conviene extraer una clase
utilitaria reutilizable. Opciones posibles:

| Opción                                      | Cuándo usarla                                                                   |
| ------------------------------------------- | ------------------------------------------------------------------------------- |
| `com.openshelf.domain.shared.Preconditions`        | Validaciones genéricas: null, blank, rango numérico, tamaño de colección        |
| `com.openshelf.domain.shared.isbn.IsbnValidator`   | Lógica específica de ISBN (checksum) — suficientemente compleja para vivir sola |
| `com.openshelf.domain.shared.EmailValidator` | Ídem para email                                                                 |

`IsbnValidator` vive en `com.openshelf.domain.shared.isbn` y `EmailValidator` en `com.openshelf.domain.shared`. La extracción de `ISBN` y `Email` como value objects queda como trabajo posterior.

---

## `Author`

| Campo         | Validaciones necesarias                                                          |
| ------------- | -------------------------------------------------------------------------------- |
| `firstName`   | No nulo, no vacío/blank, longitud máxima razonable (ej. 100 caracteres)          |
| `lastName`    | No nulo, no vacío/blank, longitud máxima razonable                               |
| `birthDate`   | No nula, debe ser una fecha en el pasado (no puede nacer hoy ni en el futuro)    |
| `nationality` | No nula, no vacía/blank                                                          |
| `biography`   | No nula (puede ser cadena vacía — un autor puede no tener biografía cargada aún) |

---

## `Book.Builder` (validación en `build()`)

La validación va en `build()`, no en cada setter individual, para poder reportar todos los
errores juntos en lugar de uno por uno.

| Campo             | Validaciones necesarias                                                         |
| ----------------- | ------------------------------------------------------------------------------- |
| `title`           | No nulo, no vacío/blank, longitud máxima razonable (ej. 500 caracteres)         |
| `isbn`            | No nulo, no vacío/blank, formato ISBN-13 válido (13 dígitos, checksum correcto) |
| `publicationDate` | No nula, no puede ser una fecha futura                                          |
| `pageCount`       | Mayor que cero                                                                  |
| `language`        | No nulo                                                                         |
| `categories`      | La lista puede estar vacía, pero no puede ser nula                              |
| `author`          | No nulo                                                                         |

> **Nota sobre ISBN:** el formato ISBN-13 tiene un dígito de control calculable
> matemáticamente. Validarlo aquí sería la primera oportunidad de escribir lógica
> no trivial. Conviene extraerlo a `IsbnValidator` para poder testearlo de forma
> independiente.

---

## `User` (se propaga a `Member`, `Librarian`, `Administrator`)

La validación en la clase base `User` protege a todas las subclases sin repetir código.

| Campo   | Validaciones necesarias                                                   |
| ------- | ------------------------------------------------------------------------- |
| `id`    | No nulo, no vacío/blank                                                   |
| `name`  | No nulo, no vacío/blank, longitud máxima razonable                        |
| `email` | No nulo, no vacío/blank, formato de email válido (contiene `@` y dominio) |

> **Nota sobre email:** la validación de formato puede empezar simple (`contains("@")`)
> y evolucionar hacia un regex o un `EmailValidator` dedicado. Buen ejercicio para ver
> cómo una validación crece en complejidad.

---

## `Loan`

| Campo      | Validaciones necesarias                |
| ---------- | -------------------------------------- |
| `id`       | No nulo, no vacío/blank                |
| `book`     | No nulo                                |
| `member`   | No nulo                                |
| `loanDate` | No nula, no puede ser una fecha futura |

> **Regla de negocio adicional (no es validación de campo, sino invariante del dominio):**
> el constructor de `Loan` podría verificar que el `member` no superó su límite de
> préstamos activos (`Member.MAX_ACTIVE_LOANS`). Depende de si se decide poner esa
> responsabilidad en el dominio o en el servicio.

---

## Orden sugerido para implementar

1. Crear `com.openshelf.domain.shared.Preconditions` — es lo que más se va a reutilizar.
2. Validar `User` — el más simple (3 campos de texto).
3. Validar `Author` — agrega la validación de fecha en el pasado.
4. Validar `Loan` — agrega validación de objeto no nulo.
5. Validar `Book.Builder.build()` — el más complejo; incluye ISBN y reporte de múltiples errores.

Escribir un test por cada validación antes de implementarla (TDD): el test falla, se
implementa la validación, el test pasa.
