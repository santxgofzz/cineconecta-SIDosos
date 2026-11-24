# Scripts Oracle

Fuente: `docs/modelos/modelo-relacional.pdf` (diagrama entregado por el equipo).
`01_schema.sql` crea las siete tablas del modelo, con sus claves primarias,
foráneas y restricciones únicas de correo y nombre del plan. Ejecutarlo conectado
al esquema Oracle que será usado por la aplicación. Solo crea estructura; no
inserta datos ni cambia objetos existentes.

Pendiente de implementar:

1. `02_constraints.sql` - restricciones CHECK y reglas que no representa el MR.
2. `04_indexes.sql` - índices por consultas y explicación.
3. `05_benchmark.sql` - comparar una consulta, EXPLAIN PLAN, volumen y mediciones.

`03_seed.sql` agrega datos sinteticos para desarrollo. Ejecutarlo una vez, despues
de `01_schema.sql`, en un esquema vacio/de prueba. Inserta planes, cuentas, perfiles,
suscripciones de 30 dias y pagos; no borra ni actualiza filas. Los IDs de perfiles
(15, 16, 21) y suscripciones (8, 9, 10) coinciden con el fixture de
`database/mongodb/demo/02_cargar_datos_sinteticos.js`.

Para comprobar el informe, el periodo `2026-10-01` a `2026-12-01` incluye 3
suscripciones y `$149700` de ingresos. El periodo `2026-11-01` a `2026-12-01`
incluye 2 suscripciones y `$99800` de ingresos. La fecha final no se incluye.
Las cuentas usan correos reservados `.invalid` y un hash BCrypt de una clave
solo de demostracion; no son credenciales para la aplicacion ni para Oracle.

**Reglas críticas:** 30 días, inicio inclusivo/fin exclusivo, pago+suscripción
atómicos, bloqueo de la cuenta en contratación concurrente, capacidad de perfiles,
ningún período superpuesto. Las tablas DM_AGE_RATINGS y DM_PROFILE_STATES del
modelo de equipo deben documentarse y evitar campos que contradigan sus catálogos.

Si cambias esos IDs, actualiza las referencias en el fixture Mongo. La demo Mongo
continua siendo una base aislada y no se transforma por ejecutar la semilla Oracle.

## Informe SQL implementado

`06_report_ingresos_y_suscripciones_por_plan.sql` recibe `:fecha_inicio` y
`:fecha_fin` como parametros `TIMESTAMP` y usa el intervalo `[inicio, fin)`.
Suma `PAYMENTS.amount` por `PAYMENTS.paidAt` y cuenta suscripciones por
`SUBSCRIPTIONS.startsAt`, agrupando ambas metricas por el plan de cada
suscripcion. Devuelve todos los planes, incluidos los que tienen cero
actividad en el periodo.
