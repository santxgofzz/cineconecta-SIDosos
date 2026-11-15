# Implementación Oracle: PENDIENTE

Fuente: `docs/modelos/modelo-relacional.pdf` (diagrama entregado por el equipo).
Conservar el modelo base del enunciado y justificar tablas/restricciones adicionales.

Crear en orden (archivos por implementar, NO incluidos como si estuvieran resueltos):

1. `01_schema.sql` - tablas, PK, FK y restricciones del MR.
2. `02_constraints.sql` - unicidad de correo/nombre por cuenta, CHECK y relación pago-suscripción.
3. `03_seed.sql` - cuentas, planes, perfiles, suscripciones históricas y pagos coherentes.
4. `04_indexes.sql` - índices por consultas y explicación.
5. `05_benchmark.sql` - comparar una consulta, EXPLAIN PLAN, volumen y mediciones.

**Reglas críticas:** 30 días, inicio inclusivo/fin exclusivo, pago+suscripción
atómicos, bloqueo de la cuenta en contratación concurrente, capacidad de perfiles,
ningún período superpuesto. Las tablas DM_AGE_RATINGS y DM_PROFILE_STATES del
modelo de equipo deben documentarse y evitar campos que contradigan sus catálogos.

Las semillas de Oracle deben coordinarse con los `profileId` y `subscriptionId`
de MongoDB. No asumir que los IDs ficticios 15/16/21 y 8/9/10 ya existen.
