# Mediciones locales: PostgreSQL y MongoDB

Fecha de ejecución: 2026-11-25. Datos sintéticos en contenedores locales;
periodo medido `[2026-11-01, 2026-12-01)`. Son mediciones de esta máquina, no
un SLA ni una comparación de bases en hardware equivalente.

## PostgreSQL 16.13

Volumen: 8 planes, 250000 suscripciones, 500000 pagos; base de datos 72 MB.
Medición `EXPLAIN ANALYZE` del mismo informe antes y después de crear los índices:

| Medición | Antes | Después |
| --- | ---: | ---: |
| Execution Time | 43.211 ms | 24.957 ms |
| Buffers leídos/tocados por el plan | 6893 hit | 1674 hit + 77 read |

El plan inicial usa `Parallel Seq Scan` sobre `payments` y `subscriptions` para
filtrar el rango. Después usa `Index Only Scan` con `ix_payments_paid_subscription`
y `ix_subscriptions_started_plan`; ambos reportan 0 heap fetches. El join de pagos
a suscripciones aún usa un `Parallel Seq Scan` en `subscriptions` y hash join,
lo cual queda visible para una siguiente iteración de índices.

Índices agregados:

- `ix_subscriptions_started_plan (starts_at, plan_id)`: 7712 kB.
- `ix_payments_paid_subscription (paid_at, subscription_id) INCLUDE (amount)`: 19 MB.

Esta ejecución bajó el tiempo indicado por el plan en aproximadamente 42.3% y
el número de bloques accedidos en aproximadamente 74.6%. La caché estaba activa.

## MongoDB 8.0.38

Volumen: 500000 sesiones; BSON de documentos 115916674 bytes, almacenamiento
comprimido 29130752 bytes. Rango de fechas y pipeline iguales antes/después.

| Medición | Antes | Después |
| --- | ---: | ---: |
| `executionStats.executionTimeMillis` | 173 ms | 44 ms |
| Documentos examinados | 500000 | 10290 |
| Claves examinadas | 0 | 10290 |
| Mediana de 5 ejecuciones calientes | 215 ms | 70 ms |

El plan inicial es `GROUP -> COLLSCAN`; después es `GROUP -> FETCH -> IXSCAN`
con `ix_benchmark_period_subscription_type`. La mediana caliente bajó cerca de
67.4% y los documentos examinados bajaron 97.9%. El índice compuesto mide
14336000 bytes (aprox. 13.7 MiB); `_id_` mide 6717440 bytes.

## Lectura de resultados

Los dos motores redujeron el trabajo para el periodo selectivo. Los tiempos no
son directamente comparables entre motores: PostgreSQL usa un solo
`EXPLAIN ANALYZE`, mientras que MongoDB además registra cinco ejecuciones
calientes. Usa los planes y filas/documentos examinados como evidencia principal;
la latencia depende del almacenamiento, CPU, memoria y estado de caché.
