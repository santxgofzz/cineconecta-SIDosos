# Benchmark relacional y MongoDB

La aplicación usa Oracle para el modelo relacional. Este benchmark es una
comparación aislada en **PostgreSQL y MongoDB**; no cambia la configuración ni
las consultas operativas de la aplicación. El modelo PostgreSQL replica las
tablas `PLANS`, `SUBSCRIPTIONS` y `PAYMENTS` necesarias para el informe por plan.

## Volumen

- PostgreSQL: 8 planes, 250 000 suscripciones y 500 000 pagos sintéticos.
- MongoDB: 500 000 sesiones sintéticas en `cineconecta_benchmark`.
- Ambos consultan el intervalo `[2026-11-01, 2026-12-01)`.
- PostgreSQL corre en el perfil opcional `benchmark`, expuesto solo en
  `127.0.0.1:5433`. MongoDB usa la instancia local, pero en una base y colección
  separadas de los datos de la aplicación.

La preparación de PostgreSQL reconstruye solo el esquema `benchmark` dentro de
la base dedicada `cineconecta_benchmark`; la preparación Mongo reemplaza solo la
colección `benchmark_viewing_sessions` dentro de `cineconecta_benchmark`. Los
dos scripts abortan si se conectan a otra base.

## Ejecutar en PowerShell

Desde la raíz del repositorio, con Docker Desktop activo:

```powershell
docker compose up -d mongodb
docker compose --profile benchmark up -d --wait postgres-benchmark

docker compose exec -T postgres-benchmark psql -U benchmark -d cineconecta_benchmark -f /benchmark/01_prepare_volume.sql
docker compose exec -T postgres-benchmark psql -U benchmark -d cineconecta_benchmark -f /benchmark/02_measure_before_after.sql

docker cp .\database\mongodb\benchmark\01_prepare_volume.js cineconecta-mongodb:/tmp/01_prepare_benchmark_volume.js
docker cp .\database\mongodb\benchmark\02_measure_before_after.js cineconecta-mongodb:/tmp/02_benchmark_before_after.js
docker compose exec -T mongodb mongosh "mongodb://localhost:27017/cineconecta_benchmark" --file /tmp/01_prepare_benchmark_volume.js
docker compose exec -T mongodb mongosh "mongodb://localhost:27017/cineconecta_benchmark" --file /tmp/02_benchmark_before_after.js
```

Para guardar las salidas, agrega `| Tee-Object -FilePath database/benchmark/postgresql-run.txt`
o `| Tee-Object -FilePath database/benchmark/mongodb-run.txt` al comando que
quieras registrar. `EXPLAIN ANALYZE` en PostgreSQL y `explain("executionStats")`
en MongoDB muestran el plan real, filas/documentos examinados y buffers/tiempo.
El script Mongo también compara la mediana de cinco ejecuciones calientes.

## Antes y después

Antes, PostgreSQL conserva las claves primarias y únicas; MongoDB conserva el
índice `_id_`. Los scripts miden el mismo rango antes de crear índices
secundarios. Después crean:

- PostgreSQL `subscriptions(starts_at, plan_id)` para el rango de suscripciones.
- PostgreSQL `payments(paid_at, subscription_id) INCLUDE (amount)` para filtrar
  pagos y cubrir importe/llave de unión.
- MongoDB `{startedAt: 1, subscriptionId: 1, contentTypeSnapshot: 1}` para
  reducir sesiones del periodo y alinear las claves iniciales de agrupación.

Los tiempos dependen de CPU, memoria, almacenamiento y caché. Compara también
tipo de plan, filas/documentos examinados, `shared read/hit blocks`,
`totalKeysExamined` y el resultado de la agregación; no infieras rendimiento de
una sola cifra de milisegundos.

Una ejecución local registrada está en `measurements-local.md`; es evidencia
de esta máquina y estos contenedores, no una promesa de rendimiento en producción.

Para detener PostgreSQL sin borrar el volumen:

```powershell
docker compose --profile benchmark stop postgres-benchmark
```

Las mediciones no son aún resultados certificados del runtime Oracle: son una
línea base local de comparación para PostgreSQL y MongoDB.
