# CineConecta - Sistemas Intensivos de Datos II

## Integrantes
- Santiago Florez Solarte
- Johan David Mora Guzman
- Juan José Aponza
- Juan Felipe Zuñiga
- Juan David Fajardo

**Estado:** aplicación modular implementada con persistencia políglota (Oracle + MongoDB). Quedan pendientes las evidencias finales sobre Oracle real (`04_indexes.sql`, `05_benchmark.sql`, `BillingOracleIntegrationTest`), la actualización visual de `docs/modelos/diagrama-mongo.png` / `modelo-relacional.pdf` y completar `docs/ia/uso-de-ia.md`.
Stack: Java 21, Spring Boot 3.5.16, Spring MVC/Thymeleaf, Spring Security,
Spring Data JPA + JDBC (Oracle) y Spring Data MongoDB (más cliente React + Vite
en `frontend/` para registro, login y gestión de perfiles).

## Qué tiene hoy?

- `docs/`: enunciado, rúbrica, MR, modelo documental, diagrama Mongo, reflexión IA y evidencias (`docs/evidencias/` y `docs/pruebas/`).
- `database/mongodb/demo/`: scripts demostrativos (`cineconecta_modelo_demo`), validadores, índices y JSON para generar el diagrama en MongoDB Compass.
- `database/mongodb/runtime/`: scripts idempotentes para crear/verificar la BD real `cineconecta` (`01_playback_activity.js` .. `08_seed_movies_dataset.js`).
- `database/oracle/`: esquema relacional (`01_schema.sql`), restricciones (`02_constraints.sql`), datos sintéticos (`03_seed.sql`), permisos mínimos (`07_least_privilege_grants.sql`), reglas de facturación (`subscriptions/`) e informe SQL (`06_report_ingresos_y_suscripciones_por_plan.sql`).
- `src/`: módulos `account`, `profile`, `subscription`, `catalog`, `watchlist`, `playback`, `admin`, `report`, `integration` y su suite de pruebas en `src/test/`.
- `frontend/`: interfaz React + Vite para inicio, login, registro y administración/selección de perfiles consumiendo la API de Spring Boot.

## Primeros pasos

1. Instalar JDK 21, Maven 3.6.3+ y un motor MongoDB. Tener acceso a Oracle XE
   o al servidor Oracle autorizado por el curso.
2. (Opcional) Levantar Mongo local con `docker compose up -d mongodb`.
3. Configurar en el sistema/IDE las variables del archivo `.env.example`.
   **Spring no lee `.env` automáticamente:** hay que definir las variables en
   el entorno o en la configuración de ejecución del IDE.
4. En SQL Developer, conecta al esquema dueño en Oracle y ejecuta con **Run Script (F5)**
   `database/oracle/01_schema.sql`, `database/oracle/02_constraints.sql`,
   `database/oracle/subscriptions/01_reglas_facturacion.sql` y una sola vez
   `database/oracle/03_seed.sql` (opcional `07_least_privilege_grants.sql` para `CINECONECTA_APP`).
5. Preparar MongoDB con los [scripts runtime](database/mongodb/runtime/README.md).
   Las referencias de actividad deben coincidir con Oracle antes de cargarlas.
   Mantén la demo en su base propia.
6. En la raíz del repo: `mvn test`, `mvn spring-boot:run`.
7. Acceder a `http://localhost:8080/`. La app necesita conexiones Oracle y
   Mongo válidas para arrancar, aun cuando la página inicial sea básica.

### Catálogo y administración

- `/admin/login` abre el acceso administrativo independiente de MongoDB.
- `/profiles/{profileId}/catalog` exige la sesión del titular, el perfil
  propio activo y una suscripción vigente.
- Consulta el [modelo y decisiones MongoDB](docs/modelos/catalogo-mongodb.md)
  y las [pruebas del módulo](docs/pruebas/catalogo-mongodb.md).

### Solo demostrar el Mongo del diseño

Si necesitas reproducir el diagrama inicial, usa un MongoDB de prueba y:

```powershell
mongosh "mongodb://localhost:27017" --file database/mongodb/demo/01_crear_colecciones_validadores_indices.js
mongosh "mongodb://localhost:27017" --file database/mongodb/demo/02_cargar_datos_sinteticos.js
mongosh "mongodb://localhost:27017" --file database/mongodb/demo/03_verificar_datos_y_consultas.js
```

**Atención:** la demo no prueba referencias Oracle ni autorización real y el
admin de demo no permite iniciar sesión. No cargarla en la base productiva.

## Carpetas Java

- `config`: configuración de conexiones, repositorios y seguridad.
- `persistence/oracle`: entidades JPA y repositorios Oracle.
- `persistence/mongo`: documentos y repositorios MongoDB.
- `account`, `profile`, `subscription`: operaciones del titular y contratación.
- `catalog`, `watchlist`, `playback`: catálogo, Mi lista y actividad.
- `admin`, `report`: administración e informes.
- `integration`: reglas que requieren consultar ambos motores.
- `common`: errores, respuestas y validaciones compartidas.

Los módulos implementan controladores REST/MVC, servicios transaccionales,
repositorios Oracle/MongoDB y validaciones de autorización en el servidor.

## Flujo de trabajo Git

- `main`: versión estable y demostrable.
- `develop`: integración del equipo.
- Ramas `feature/*` / `fix/*`: trabajo por módulo y Pull Requests hacia `develop`.

Cada miembro trabaja desde `develop`, hace commits de un cambio entendible,
abre Pull Request, solicita revisión y evita merges directos a `main`.

## Definición de terminado

Una historia está terminada SOLO cuando: tiene endpoints/pantallas funcionales,
restricciones del servidor, pruebas, y evidencia reproducible. Los criterios
obligatorios están en `docs/pruebas/plan.md`.

## Seguridad

No incluir credenciales en Git. `SecurityConfig` (`@Order(2)`) autentica a los
titulares contra `ACCOUNTS` en Oracle (`AccountUserDetailsService` + BCrypt) y
`AdminSecurityConfig` (`@Order(1)`) aísla el acceso administrativo en MongoDB
(`/admin/**`, `/api/admin/**`) con su propio contexto de sesión, roles y CSRF.
