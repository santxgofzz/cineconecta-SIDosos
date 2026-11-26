# CineConecta - Sistemas Intensivos de Datos II

## Integrantes
- Santiago Florez Solarte
- Johan David Mora Guzman
- Juan José Aponza
- Juan Felipe Zuñiga
- Juan David Fajardo

**Estado:** estructura inicial del repositorio, NO aplicación terminada.
Stack propuesto: Java 21, Spring Boot 3.5.16, Spring MVC/Thymeleaf, Spring
Security, Spring Data JPA (Oracle) y Spring Data MongoDB.

## Qué tiene hoy?

- `docs/`: enunciado, rúbrica, MR, modelo documental y diagrama Mongo.
- `database/mongodb/demo/`: scripts **originales**, validadores, índices y JSON
  enviados por el equipo; usan `cineconecta_modelo_demo` con IDs SQL ficticios.
- `database/mongodb/runtime/`: tareas para crear BD real `cineconecta`.
- `database/oracle/`: plan de scripts SQL todavía no implementados.
- `src/`: base mínima de Spring Boot y un ejemplo unitario de vigencia de suscripción.

## Primeros pasos

1. Instalar JDK 21, Maven 3.6.3+ y un motor MongoDB. Tener acceso a Oracle XE
   o al servidor Oracle autorizado por el curso.
2. (Opcional) Levantar Mongo local con `docker compose up -d mongodb`.
3. Configurar en el sistema/IDE las variables del archivo `.env.example`.
   **Spring no lee `.env` automáticamente.**
4. Crear el esquema Oracle con los scripts del equipo (PENDIENTES).
5. Preparar MongoDB con los [scripts runtime](database/mongodb/runtime/README.md).
   Alinear las referencias de actividad con Oracle antes de cargar esos datos.
   Mantener la demo en su base propia.
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

Los módulos contienen marcadores README, NO controladores/servicios completos.

## Flujo de trabajo Git

- `main`: versión estable y demostrable.
- `develop`: integración del equipo.
- `feature/oracle-subscriptions`
- `feature/mongo-catalog`
- `feature/playback-integration`
- `feature/auth-profiles-ui`
- `feature/reports-performance`

Cada miembro trabaja desde `develop`, hace commits de un cambio entendible,
abre Pull Request, solicita revisión y evita merges directos a `main`.

## Definición de terminado

Una historia está terminada SOLO cuando: tiene endpoints/pantallas funcionales,
restricciones del servidor, pruebas, y evidencia reproducible. Los criterios
obligatorios están en `docs/pruebas/plan.md`.

## Seguridad

No incluir credenciales en Git. La cadena general de `SecurityConfig`
mantiene pendiente la integración con la autenticación de titulares Oracle.
`AdminSecurityConfig` usa el acceso MongoDB independiente, con BCrypt,
roles y CSRF para las operaciones de administración.

## Limitaciones de este ZIP

Es un scaffold para iniciar el repositorio, no un sistema completo ni un modelo
Oracle implementado. El POM y Java no se han compilado en este entorno: verificar
`mvn test` en la máquina del equipo antes de integrar nuevas funcionalidades.
