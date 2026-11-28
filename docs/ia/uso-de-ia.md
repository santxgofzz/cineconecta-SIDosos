# Reflexión sobre el uso de IA (completar como equipo)

- Herramientas utilizadas y tareas en que ayudaron.
- Qué sugerencias se aceptaron, modificaron o rechazaron, y por qué.
- Qué verificamos ejecutando consultas, pruebas y revisiones manuales.
- Límites: código no probado, referencias Oracle ficticias, riesgos de seguridad.
- Qué decisiones puede explicar individualmente cada integrante.


## Catálogo y administración

Codex asistió la implementación, la revisión de cambios y la ejecución de
pruebas de la rama de Johan. Contrastó el esquema y la proyección de
episodios con los ejemplos BSON y contratos de playback. Una prueba detectó
que la proyección interpretaba el ID de episodio como un ID documental,
por lo que se fijó el mapeo al campo BSON `id`. La verificación separó los
resultados de MongoDB real, los de MockMvc y los escenarios con Oracle
simulado. El recorrido completo en navegador con Oracle y las mediciones
comparativas quedan pendientes.

Las decisiones que Johan debe poder explicar están en el
[anexo del catálogo](../modelos/catalogo-mongodb.md). La revisión de
integración por el equipo sigue el flujo del repositorio.


## Cuentas, perfiles, seguridad y ajustes de modelo (Juan José Aponza)

Se utilizó asistencia de IA en los módulos de cuentas y perfiles (`account`,
`profile`, `SecurityConfig`, `02_constraints.sql`, `07_least_privilege_grants.sql`),
la interfaz React opcional en `frontend/`, el dataset extendido
`08_seed_movies_dataset.js` y la resolución de las observaciones del modelo tras
la revisión del monitor.

- **Sugerencias aceptadas/modificadas:** se mantuvo `maxAgeRating` e `isActive`
  como únicas columnas en `PROFILES` apuntando directamente por clave foránea a
  `DM_AGE_RATINGS` y `DM_PROFILE_STATES`, eliminando las columnas duplicadas
  `DM_AGE_RATINGS_ageValue` y `DM_PROFILE_STATES_stateCode` junto con sus
  `CHECK` de sincronización; se eliminó `durationSeconds` de `viewing_progress`
  para conservarlo únicamente en `contents` y leerlo en `ActivityService`; y se
  preservó `stateCode` solo en el DTO `ProfileResponse` para mantener compatibilidad
  con la UI y las pruebas existentes.
- **Verificación ejecutada:** se validaron los scripts MongoDB contra un
  contenedor `mongo:8.0` local (`01`..`08`, `05_verificar_playback_activity.js`
  y 10 pruebas reales de integración MongoDB con Maven), la sintaxis JS/JSON con
  `node --check` y las pruebas unitarias/MockMvc con `mvn test`, incluyendo el
  ajuste de `'A'` en `JdbcBillingRepository.SQL_ACTIVE_PROFILES`.
- **Pendiente de verificación externa:** la ejecución de las pruebas JPA y DDL
  actualizados contra una instancia real de Oracle (`ddl-auto: validate` y
  `BillingOracleIntegrationTest` con `RUN_ORACLE_IT=true`) y la exportación
  definitiva de los archivos visuales (`diagrama-mongo.png` y `modelo-relacional.pdf`).
