# Informe: consultas, índices y mediciones

## Propósito y alcance

Este ejercicio compara el costo de dos consultas representativas sobre datos sintéticos: el informe relacional de ingresos y suscripciones por plan, y el consumo agrupado de sesiones por suscripción y tipo de contenido. Se mide cada consulta antes y después de añadir índices secundarios.

La aplicación CineConecta usa Oracle para sus datos relacionales. PostgreSQL se usa aquí como un espejo de benchmark, en una base aislada; estos resultados no describen ni predicen el rendimiento de Oracle en el ambiente del curso. La base `cineconecta_benchmark` y la colección `benchmark_viewing_sessions` están separadas de los datos operativos.

## Datos y periodo

Los generadores reproducibles están en:

- [`01_prepare_volume.sql`](../postgresql/benchmark/01_prepare_volume.sql): 8 planes, 250 000 suscripciones y 500 000 pagos en el esquema `benchmark`.
- [`01_prepare_volume.js`](../mongodb/benchmark/01_prepare_volume.js): 500 000 sesiones en `cineconecta_benchmark.benchmark_viewing_sessions`.

Ambas consultas usan el intervalo semiabierto `[2026-11-01, 2026-12-01)`; el inicio se incluye y el final se excluye. Así se evita contar dos veces el límite cuando los periodos se ejecutan uno después del otro. La consulta MongoDB interpreta las fechas como UTC.

## Consulta PostgreSQL: ingresos y suscripciones por plan

La consulta devuelve una fila por plan, aunque no tenga actividad en el intervalo. Cuenta suscripciones cuya fecha de inicio cae en el periodo y suma pagos cuya fecha de pago cae en ese mismo periodo.

```sql
WITH subscriptions_by_plan AS (
    SELECT s.plan_id, COUNT(*) AS subscription_count
    FROM benchmark.subscriptions AS s
    WHERE s.starts_at >= TIMESTAMP '2026-11-01 00:00:00'
      AND s.starts_at <  TIMESTAMP '2026-12-01 00:00:00'
    GROUP BY s.plan_id
), payments_by_plan AS (
    SELECT s.plan_id, SUM(p.amount) AS revenue
    FROM benchmark.payments AS p
    JOIN benchmark.subscriptions AS s
      ON s.subscription_id = p.subscription_id
    WHERE p.paid_at >= TIMESTAMP '2026-11-01 00:00:00'
      AND p.paid_at <  TIMESTAMP '2026-12-01 00:00:00'
    GROUP BY s.plan_id
)
SELECT plan.plan_id,
       plan.name,
       COALESCE(payments.revenue, 0) AS revenue,
       COALESCE(subscriptions.subscription_count, 0) AS subscription_count
FROM benchmark.plans AS plan
LEFT JOIN payments_by_plan AS payments
  ON payments.plan_id = plan.plan_id
LEFT JOIN subscriptions_by_plan AS subscriptions
  ON subscriptions.plan_id = plan.plan_id
ORDER BY plan.plan_id;
```

Índices añadidos para la segunda ejecución:

```sql
CREATE INDEX ix_subscriptions_started_plan
    ON benchmark.subscriptions (starts_at, plan_id);

CREATE INDEX ix_payments_paid_subscription
    ON benchmark.payments (paid_at, subscription_id) INCLUDE (amount);
```

El orden de las columnas empieza por la fecha que filtra el rango. `plan_id` y `subscription_id` ayudan con agrupación y unión; `amount` queda cubierto por el índice de pagos.

## Consulta MongoDB: consumo de sesiones

El pipeline filtra sesiones por fecha y agrupa por suscripción y tipo de contenido. Calcula cantidad de sesiones, segundos vistos y perfiles distintos.

```javascript
[
  { $match: { startedAt: {
    $gte: ISODate("2026-11-01T00:00:00Z"),
    $lt: ISODate("2026-12-01T00:00:00Z")
  } } },
  { $group: {
    _id: {
      subscriptionId: "$subscriptionId",
      contentType: "$contentTypeSnapshot"
    },
    sessions: { $sum: 1 },
    watchedSeconds: { $sum: "$watchedSeconds" },
    profileIds: { $addToSet: "$profileId" }
  } }
]
```

Índice añadido para la segunda ejecución:

```javascript
db.benchmark_viewing_sessions.createIndex(
  { startedAt: 1, subscriptionId: 1, contentTypeSnapshot: 1 },
  { name: "ix_benchmark_period_subscription_type" }
);
```

`startedAt` permite acotar el rango. Las otras claves corresponden a campos de agrupación. El plan todavía debe leer documentos (`FETCH`) para sumar `watchedSeconds` y obtener `profileId`, que no están incluidos en el índice.

## Evidencia observada

Las mediciones capturadas el 25 de noviembre de 2026 están en [`measurements-local.md`](measurements-local.md). Los scripts de medición [`02_measure_before_after.sql`](../postgresql/benchmark/02_measure_before_after.sql) y [`02_measure_before_after.js`](../mongodb/benchmark/02_measure_before_after.js) imprimen los planes y métricas completos al ejecutarse.

| Motor y volumen | Antes | Después | Cambio observado |
| --- | --- | --- | --- |
| PostgreSQL 16.13, 250k suscripciones y 500k pagos | 43.211 ms; 6 893 bloques `hit`; escaneos secuenciales | 24.957 ms; 1 674 `hit` + 77 `read`; escaneos `Index Only Scan` para filtrar fechas | 42.3% menos tiempo en esta ejecución; 74.6% menos bloques accedidos |
| MongoDB 8.0.38, 500k sesiones | 173 ms en `executionStats`; 500 000 documentos examinados; 0 claves; mediana caliente 215 ms | 44 ms; 10 290 documentos y 10 290 claves; mediana caliente 70 ms | 97.9% menos documentos examinados; 67.4% menos mediana caliente |

El PostgreSQL posterior usó `ix_payments_paid_subscription` y `ix_subscriptions_started_plan` en `Index Only Scan`, con 0 `Heap Fetches` para esos nodos. El join de pagos con suscripciones todavía usó `Parallel Seq Scan` y hash join; los índices no eliminaron todo escaneo secuencial.

MongoDB pasó de `COLLSCAN -> GROUP` a `IXSCAN -> FETCH -> GROUP` (la impresión del explain puede presentar `GROUP` antes del cursor). La colección contenía 115 916 674 bytes de BSON y 29 130 752 bytes de almacenamiento comprimido. El nuevo índice ocupó 14 336 000 bytes. Ambos planes devolvieron 10 290 grupos.

Las cifras son evidencia de esta ejecución y este equipo. El tiempo de PostgreSQL corresponde a una ejecución de `EXPLAIN ANALYZE`; MongoDB además resume cinco ejecuciones calientes. No es una comparación directa entre motores ni un SLA. En particular, PostgreSQL reportó caché activa y solo se midió un periodo relativamente selectivo.

## Instrucciones de ejecución

Desde PowerShell en la raíz del repositorio, con Docker Desktop activo:

```powershell
docker compose up -d mongodb
docker compose --profile benchmark up -d --wait postgres-benchmark

docker compose exec -T postgres-benchmark psql -U benchmark -d cineconecta_benchmark -f /benchmark/01_prepare_volume.sql
docker compose exec -T postgres-benchmark psql -U benchmark -d cineconecta_benchmark -f /benchmark/02_measure_before_after.sql | Tee-Object -FilePath database/benchmark/postgresql-run.txt

docker cp .\database\mongodb\benchmark\01_prepare_volume.js cineconecta-mongodb:/tmp/01_prepare_benchmark_volume.js
docker cp .\database\mongodb\benchmark\02_measure_before_after.js cineconecta-mongodb:/tmp/02_benchmark_before_after.js
docker compose exec -T mongodb mongosh "mongodb://localhost:27017/cineconecta_benchmark" --file /tmp/01_prepare_benchmark_volume.js
docker compose exec -T mongodb mongosh "mongodb://localhost:27017/cineconecta_benchmark" --file /tmp/02_benchmark_before_after.js | Tee-Object -FilePath database/benchmark/mongodb-run.txt
```

La preparación de PostgreSQL elimina y vuelve a crear únicamente el esquema `benchmark`; la preparación de MongoDB reemplaza únicamente `benchmark_viewing_sessions` en la base de benchmark. Ejecutar cada preparación antes de su medición restaura un volumen conocido. La medición también reinicia los índices secundarios y luego los vuelve a crear. No apuntes estos scripts a una base distinta: incluyen guardas que comprueban el nombre de la base.

Al finalizar, PostgreSQL se puede detener sin borrar el volumen persistente:

```powershell
docker compose --profile benchmark stop postgres-benchmark
```

## Reflexión del equipo sobre el uso de IA

Reflexión del equipo sobre el uso de la inteligencia artificial

En el desarrollo de CineConecta usamos herramientas de inteligencia artificial, como Claude Code y Codex, para apoyar la programación, la escritura de pruebas, la creación de scripts para Oracle y MongoDB, la organización del trabajo en commits y la revisión de cambios antes de integrarlos. Estas herramientas nos permitieron avanzar más rápido en tareas repetitivas o extensas y nos ayudaron a entender temas que no dominábamos del todo, como el diseño de índices, la validación de documentos, el manejo de reintentos y el control de concurrencia.

Sin embargo, aprendimos que la IA comete errores con mucha seguridad. En varios momentos propuso soluciones basadas en supuestos incorrectos. Por ejemplo, usó nombres de columnas que no coincidían con nuestro esquema relacional y recomendó cambios que, al medirlos, resultaron innecesarios. Además, como cada integrante trabajaba en su propia rama con su propia asistencia, aparecieron diferencias entre módulos que la IA no detectó por sí sola: nombres distintos, archivos con la misma numeración y pruebas que dejaban de funcionar al integrar el trabajo de otro compañero. Estos problemas solo salieron a la luz cuando ejecutamos el sistema contra bases de datos reales, corrimos las pruebas completas y revisamos las ramas antes de fusionarlas.

Esto nos dejó claro que la responsabilidad del resultado es del equipo, no de la herramienta. Decidimos qué sugerencias aceptar, cuáles modificar y cuáles rechazar. Por ejemplo, no permitimos cambios que afectaran el trabajo de los demás sin acordarlo, como reescribir el historial compartido del repositorio. También entendimos que no basta con que el código compile o que las pruebas pasen con datos simulados: hay que verificarlo con datos reales, medir su comportamiento y comunicar los cambios al grupo para que la integración funcione.

En conclusión, la inteligencia artificial fue una herramienta valiosa para acelerar el desarrollo y aprender, pero no reemplaza el criterio de cada integrante, la verificación ni la coordinación del equipo. Usarla de forma responsable implica ser transparentes sobre su uso, comprobar todo lo que produce y asegurarnos de que cada integrante pueda explicar y defender las decisiones que quedaron en su parte del proyecto.