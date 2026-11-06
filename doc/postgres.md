# Etapa 4 — JDBC, migraciones Flyway y Testcontainers

`PostgresBookRepository` implementa el mismo contrato que `InMemoryBookRepository`.
Recibe un `javax.sql.DataSource`, abre una conexión por operación y cierra conexiones,
sentencias, resultados y arrays JDBC. El dueño del `DataSource` administra su ciclo de
vida. Las conexiones deben estar configuradas con auto-commit.

## Modelo de almacenamiento

El esquema inicial está en `src/main/resources/db/migration/V1__create_books.sql`. La tabla `books` usa
el ISBN exacto como clave, sin normalizarlo. Guarda todos los campos del libro y una
instantánea de su autor. Así, cambiar la biografía de un autor al guardar un libro no
modifica los otros libros, igual que en memoria.

Las categorías se guardan como `TEXT[]`: se conservan su orden, duplicados y lista
vacía. Los nombres de los enums se almacenan como texto. La búsqueda por autor usa
nombre, apellido y fecha de nacimiento, con un índice sobre esos tres campos.

`save` ejecuta un único `INSERT ... ON CONFLICT DO UPDATE`, por lo que el reemplazo
del libro completo es atómico. Las consultas usan parámetros JDBC; los datos no se
concatenan en SQL. `delete` es idempotente y las consultas devuelven listas no
modificables. Un error JDBC se propaga como `PersistenceException` conservando su causa.

## Preparar una base de desarrollo vacía

El repositorio no crea tablas automáticamente. Flyway aplica las migraciones pendientes
y registra versión, checksum y resultado en `flyway_schema_history`:

```bash
docker compose up -d --wait postgres
docker compose run --rm app mvn flyway:migrate
docker compose run --rm app mvn flyway:info
docker compose run --rm app mvn flyway:validate
```

Compose configura `FLYWAY_URL`, `FLYWAY_USER` y `FLYWAY_PASSWORD` desde la configuración
de desarrollo. Los mismos nombres pueden usarse como variables de entorno al ejecutar
Maven localmente. El plugin no está asociado al ciclo de compilación: `mvn verify`
no migra la base de desarrollo. Los tests migran únicamente sus esquemas temporales.

Repetir `migrate` valida el historial y aplica solo las versiones pendientes. No editar
una migración aplicada; agregar un nuevo archivo `V2__descripcion.sql`, etc. La ejecución
Java usa recursos del classpath; el plugin Maven usa la misma carpeta en el código fuente.
El comando `clean` de Flyway está deshabilitado y `baselineOnMigrate` está en `false`.

### Si se aplicó el antiguo `db/schema.sql`

Una base no vacía sin historial se rechaza. Antes de adoptarla, revisar que sus tablas
e índices coincidan con `V1__create_books.sql` y contar con un respaldo. Solo si coincide,
registrar explícitamente esa versión como punto de partida:

```bash
docker compose run --rm app mvn flyway:baseline -Dflyway.baselineVersion=1
docker compose run --rm app mvn flyway:migrate
```

El baseline no compara el esquema ni ejecuta V1: esa comprobación es responsabilidad
de quien adopta la base. No se ejecutó este procedimiento sobre la base de desarrollo.

Ejemplo de composición desde Java, con la configuración del entorno de Compose:

```java
PGSimpleDataSource dataSource = new PGSimpleDataSource();
dataSource.setServerNames(new String[] {System.getenv("DB_HOST")});
dataSource.setPortNumbers(new int[] {Integer.parseInt(System.getenv("DB_PORT"))});
dataSource.setDatabaseName(System.getenv("DB_NAME"));
dataSource.setUser(System.getenv("DB_USER"));
dataSource.setPassword(System.getenv("DB_PASSWORD"));
DatabaseMigrator.migrate(dataSource);
BookRepository books = new PostgresBookRepository(dataSource);
LoanService service = new LoanService(new InMemoryLoanRepository(), books);
```

Solo el catálogo se persiste en PostgreSQL por ahora; préstamos, usuarios y reservas
conservan sus implementaciones en memoria.

## Pruebas de integración

La suite habitual sigue sin requerir una base:

```bash
docker compose run --rm app mvn verify
```

Para probar JDBC y migraciones contra PostgreSQL 16 real, sin instalar Java localmente:

```bash
docker compose --profile integration run --rm --build integration-tests
```

`integration-tests` ejecuta Maven con el perfil `postgres-it` y monta el socket Docker
del host para que Testcontainers cree contenedores hermanos. Este acceso se limita al
servicio de integración; `app` no monta el socket. `host.docker.internal` se resuelve
mediante `host-gateway` y `TESTCONTAINERS_HOST_OVERRIDE` permite alcanzar los puertos
temporales de PostgreSQL y del contenedor de limpieza Ryuk.

Con Java 21, Maven y acceso a Docker instalados localmente:

```bash
mvn -Ppostgres-it verify
```

`@Testcontainers` inicia un PostgreSQL `postgres:16-alpine` por clase y lo elimina al
terminar. La URL, el puerto y las credenciales salen de ese contenedor; ya no se usan
`PG_TEST_URL`, `PG_TEST_USER` ni `PG_TEST_PASSWORD`, ni una base externa. Cada test sigue
creando un esquema aleatorio y aplicando Flyway para aislar sus datos. No se habilita
reutilización de contenedores ni se desactiva Ryuk.

La primera ejecución puede descargar imágenes y dependencias. El perfil requiere un
daemon Docker accesible y falla si no está disponible; no omite silenciosamente las
pruebas. El servicio de desarrollo `postgres` y su volumen no participan en estos tests.
Para Docker remoto o rootless, adaptar el socket y la dirección del host al entorno;
el servicio Compose incluido está configurado para el socket local estándar.

Failsafe ejecuta `PostgresBookRepositoryIT` durante `integration-test` y comprueba
los resultados en `verify`. Se prueban persistencia entre instancias, todos los campos,
reemplazos, filtros, eliminación, entradas inválidas, caracteres SQL como datos,
fallo atómico de un reemplazo e integración con `LoanService`. La preparación de cada
esquema usa `DatabaseMigrator`; también se comprueban repetición sin pérdida de datos,
historial, rechazo de checksums alterados y rechazo de esquemas no versionados.

Compose solo proporciona el entorno Maven. El ciclo de vida de PostgreSQL lo controla
la extensión JUnit de Testcontainers, incluido el cierre al finalizar la clase.
