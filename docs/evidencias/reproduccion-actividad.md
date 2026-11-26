# Evidencia de reproducción, Mi lista y actividad

Ejecución del 25 de noviembre de 2026 en Windows con Temurin 21.0.11 y Maven 3.9.16.
Bases en contenedores locales: MongoDB 8.0.38 (`mongo:8.0`) y Oracle Database 23 Free
(`gvenzl/oracle-free:23-slim-faststart`). Oracle se cargó con `01_schema.sql` y `03_seed.sql`
de la rama `feature/informe-ingresos-planes`. Mongo se cargó con `01_playback_activity.js`,
`02_catalog_admin.js` y `03_seed_catalog_dev.js`.

La aplicación se ejecutó con `mvn spring-boot:run`, perfil `dev` y la cuenta 1 de la semilla
Oracle (perfiles 15 y 16, suscripción 10 vigente hasta el 8 de diciembre de 2026).
Las consultas de `JdbcAccountDataGateway` se ejecutaron con los nombres de columna de ese
`01_schema.sql` (`profileId`, `ACCOUNTS_accountId`, `maxAgeRating`, `isActive`, `startsAt`,
`endsAt`) y con `isActive = 'A'` como perfil activo. Ese ajuste se integra cuando el esquema
Oracle esté en `develop`.

## Flujos contra ambas bases

| Caso | Petición | Resultado observado |
| --- | --- | --- |
| Agregar a Mi lista | `PUT /api/profiles/15/my-list/CNT-001` | 201, disponible |
| Agregar el mismo título otra vez | igual | 200, un solo documento |
| Título 18+ en perfil infantil (máx. 7) | `PUT /api/profiles/16/my-list/CNT-004` | 403 `AGE_RATING_EXCEEDED` |
| Título en borrador | `PUT …/my-list/CNT-006` | 409 `CONTENT_NOT_AVAILABLE` |
| Título retirado | `PUT …/my-list/CNT-005` | 409 `CONTENT_NOT_AVAILABLE` |
| Perfil de otra cuenta | `PUT /api/profiles/21/my-list/CNT-001` | 403 `PROFILE_NOT_ALLOWED` |
| Título de Mi lista que se retira después | `GET /api/profiles/15/my-list` | Se conserva con `availability: RETIRED` |
| Reproducir 12+ en perfil infantil | `POST /api/profiles/16/playback/sessions` | 403 `AGE_RATING_EXCEEDED` |
| Reproducir con perfil ajeno | `POST /api/profiles/21/playback/sessions` | 403 `PROFILE_NOT_ALLOWED` |
| Serie sin episodio | `{"contentId":"CNT-002"}` | 400 `EPISODE_REQUIRED` |
| Serie con episodio | `{"contentId":"CNT-002","episodeId":"EP-002-01"}` | 201, `unitKey = EP-002-01` |
| Suscripción vencida (endsAt movido al pasado en Oracle) | iniciar | 403 `SUBSCRIPTION_REQUIRED` |
| Suscripción restaurada | iniciar | 201 |
| `WATCH` repetido con el mismo `seq` | evento | Se ignora: tiempo visto sigue en 3 s |
| `WATCH` de 500 s declarado 3 s después de iniciar | evento | Solo suma el tiempo real (6 s en total) |
| `SEEK` a 1200 s | evento | Posición 1200, tiempo visto sin cambios |
| Pausa | evento | Sesión cerrada con `endReason: paused` |
| Continuar viendo tras pausar al 36 % | `GET /api/profiles/15/continue-watching` | Aparece con `percent: 36` |
| Reanudar | iniciar | Sesión nueva que arranca en 1200 s |
| Avanzar a 2970 de 3300 s (90 %) | evento | `completed: true` |
| Continuar viendo tras el 90 % | consulta | Ya no aparece |
| Historial | `GET /api/profiles/15/history` | Una sesión por inicio, la más reciente primero |
| Página de actividad | `GET /profiles/15/activity` | 200 |

## Caída de una base al iniciar la reproducción

Contenedor detenido (`docker stop`) o congelado (`docker pause`) durante
`POST /api/profiles/15/playback/sessions`. En ningún caso se autorizó la reproducción.

| Caso | Antes del ajuste de tiempos de espera | Después |
| --- | --- | --- |
| Oracle detenido | 503 `ORACLE_UNAVAILABLE` en 30,0 s | 503 en 3,0 s |
| Oracle congelado | 503 `ORACLE_UNAVAILABLE` en 30,0 s | 503 en 3,0 s |
| Mongo detenido | 503 `MONGO_UNAVAILABLE` en 34,5 s | 503 en 3,0 s |
| Mongo congelado | 503 `MONGO_UNAVAILABLE` en 30,0 s | 503 en 3,7 s |
| Bases recuperadas | 201 | 201 |

Los 30 s eran los valores por defecto de HikariCP y del driver de MongoDB. Se fijaron en
`application.yml` (Oracle) y `MongoTimeoutConfig` (Mongo); se pueden cambiar con
`ORACLE_CONNECTION_TIMEOUT_MS`, `ORACLE_CONNECT_TIMEOUT_MS`, `ORACLE_READ_TIMEOUT_MS` y
`cineconecta.mongo.*-timeout-ms`.

## Verificación de colecciones e índices

`05_verificar_playback_activity.js` con `profileId = 15`, después de los flujos anteriores:

```text
[1-2] 3 colecciones con $jsonSchema estricto y sus 5 índices: OK
[3]   Mi lista del perfil:          SORT <- FETCH <- IXSCAN(uq_my_list_profile_content)
      Continuar viendo:             LIMIT <- FETCH <- IXSCAN(ix_progress_continue)
      Progreso de una unidad:       FETCH <- IXSCAN(uq_progress_profile_unit)
      Historial de sesiones:        LIMIT <- FETCH <- IXSCAN(ix_sessions_profile_history)
      Sesiones abiertas de unidad:  FETCH <- IXSCAN(ix_sessions_profile_history)
      Sesiones por periodo:         FETCH <- IXSCAN(ix_sessions_period_sub)  (filtro del informe de consumo)
[4]   Inconsistencias: 0
RESULTADO: OK, 0 aviso(s)
```

## Datos de actividad de desarrollo

`07_seed_activity_dev.js` sobre `cineconecta` recién creada con `01`, `02` y `03`:

| Control | Resultado observado |
| --- | --- |
| Sin colecciones de actividad | Se detiene: «Primero ejecuta 01_playback_activity.js.» |
| Sin catálogo | Se detiene: «Primero ejecuta 02_catalog_admin.js y 03_seed_catalog_dev.js.» |
| Primera carga | 10 sesiones, 9 progresos y 6 elementos de Mi lista |
| Segunda carga | 0 nuevos en las tres colecciones |
| Sobre una base con actividad previa | Conserva el progreso y Mi lista existentes (8 de 9 y 5 de 6 nuevos) |
| Actividad por perfil | 15: películas y series; 16: documental; 21: película, serie y documental |
| `05_verificar_playback_activity.js` después de la carga | `RESULTADO: OK, 0 aviso(s)` |

## Índices: antes y después

`06_benchmark_playback_activity.js` sobre la base aislada `cineconecta_benchmark`
(se crea y se borra; nunca toca `cineconecta`). Volumen: 200 000 sesiones y 80 000
progresos de 2000 perfiles. Tiempo: mediana de 15 ejecuciones.

| Consulta | Antes: plan | Docs examinados | ms | Después: plan | Docs examinados | ms |
| --- | --- | --- | --- | --- | --- | --- |
| Continuar viendo | `SORT <- COLLSCAN` | 80 000 | 24 | `LIMIT <- FETCH <- IXSCAN(ix_progress_continue)` | 12 | 1 |
| Historial de sesiones | `SORT <- COLLSCAN` | 200 000 | 42 | `LIMIT <- FETCH <- IXSCAN(ix_sessions_profile_history)` | 20 | 1 |
| Informe de consumo por periodo | `GROUP <- COLLSCAN` | 200 000 | 44 | `GROUP <- FETCH <- IXSCAN(ix_sessions_period_sub)` | 15 596 | 18 |

Interpretación:

- Continuar viendo e historial: el índice tiene el perfil como igualdad y el orden de la
  consulta (`lastActivityAt` o `startedAt` descendente) como último campo. Mongo lee solo
  los 12 o 20 documentos que devuelve y no ordena en memoria. El costo deja de depender del
  total de sesiones y pasa a depender del tamaño de la página.
- Informe de consumo: se midió la agregación de `MongoConsumptionReportRepository`
  (rama `feature/informe-ingresos-planes`), que filtra por rango de `startedAt` y agrupa por
  suscripción y tipo de contenido. Como el índice empieza por `startedAt`, Mongo examina
  exactamente las 15 596 sesiones de noviembre (claves = documentos) en lugar de las 200 000.
  El costo pasa a depender del volumen del periodo, no del histórico completo. Un índice que
  empezara por `subscriptionId` no serviría aquí, porque el informe no filtra por suscripción.

Para repetirlo:

```powershell
mongosh "mongodb://localhost:27017" --file database/mongodb/runtime/06_benchmark_playback_activity.js
```
