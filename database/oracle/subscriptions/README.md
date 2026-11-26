# santiago - commit s1: reglas de suscripciones y pagos

Este commit complementa las tablas **PLANS**, **SUBSCRIPTIONS** y **PAYMENTS** del modelo relacional que ya definió el equipo. No vuelve a crear tablas ni modifica lo que hicieron los demás.

## por qué está separado

En la copia de `develop` del 25 de noviembre todavía no aparece `database/oracle/01_schema.sql`, pero el historial incluido en el ZIP sí contiene la rama `origin/feature/informe-ingresos-planes`, donde Felipe ya preparó **el esquema de todas las tablas** y los datos sintéticos en `01_schema.sql` y `03_seed.sql`.

Si creamos otra versión de las mismas tablas habría conflictos al integrar. Por eso S1 se enfoca en agregar reglas de integridad al modelo existente, sin cambiar columnas ni nombres.

## orden para ejecutar con sql developer

1. Tener listas las tablas `ACCOUNTS`, `PLANS`, `SUBSCRIPTIONS`, `PAYMENTS` (creadas con el SQL general del equipo, no se crean acá).
2. Ejecutar `01_reglas_facturacion.sql` con **F5**. Agrega las restricciones y las secuencias si no existen.
3. Ejecutar `02_verificar_estructura.sql` con **F5**. No requiere datos de prueba.
4. Ejecutar `03_prueba_facturacion.sql` con **F5**. Es independiente de `03_seed.sql`: crea cuenta y plan temporales, prueba reglas y revierte todo con `ROLLBACK`.
5. Ejecutar otra vez `02_verificar_estructura.sql` para comprobar que los datos existentes siguen bien.

No hay que correr un `DROP TABLE`, ni modificar archivos de `catalog`, `playback` o `report`.

## qué agrega s1

| tabla | regla |
|---|---|
| `PLANS` | precio mayor o igual a cero; máximo de perfiles mínimo 1 |
| `SUBSCRIPTIONS` | fecha final posterior a la inicial; duración exacta de 30 días |
| `PAYMENTS` | monto no negativo; un solo pago por suscripción; referencia única |
| secuencias | `CC_SUBSCRIPTIONS_SEQ` y `CC_PAYMENTS_SEQ` con inicio por encima de los IDs existentes |

Los nombres físicos son los del SQL del equipo: `subscriptionId`, `startsAt`, `endsAt`, `ACCOUNTS_accountId`, `PLANS_planId`, `SUBSCRIPTIONS_subscriptionId`. En Oracle, al no llevar comillas, se guardan internamente en mayúsculas.

## lo que queda para s2 y s3

- **s2:** implementar la compra simulada en Spring Boot usando transacciones y las secuencias para generar IDs.
- **s3:** bloquear la cuenta durante la contratación; revisar la suscripción vigente y el límite de perfiles, impedir períodos superpuestos y asegurar el rollback del pago y la suscripción juntos.
- Este commit todavía **no impide solapamientos entre filas** ni garantiza por sí solo que cada suscripción tenga un pago: son reglas entre operaciones que se implementarán en S3. `02_verificar_estructura.sql` sí detecta registros históricos que las incumplan.

## precauciones

- Ejecutar los scripts sobre un esquema de **desarrollo autorizado**, nunca sobre una BD compartida sin coordinar al equipo. Las sentencias `ALTER TABLE` hacen confirmación implícita en Oracle.
- Si la base tiene datos que violan las nuevas reglas, Oracle rechazará el `ALTER TABLE` y mostrará el error. No los borra para forzar el paso.
- El script de prueba crea una cuenta y un plan temporales; no necesita datos sinteticos de otros modulos y no deja filas, pero las secuencias avanzan aunque se haga rollback, lo cual es normal.
- Si vas a ejecutar la semilla de Felipe (`database/oracle/03_seed.sql`) para una demo integrada, hacelo aparte y solo una vez sobre un esquema de desarrollo preparado; no hace falta para validar este commit.
- `02_verificar_estructura.sql` exige que las suscripciones históricas tengan un pago y que no se superpongan. Eso sirve como control de integridad, no como sustituto de la lógica de compra concurrente.
- Si al ejecutar falla la sentencia del intervalo `INTERVAL '30' DAY`, anotar el código Oracle y revisar la versión del servidor antes de cambiar la regla.

## evidencias para guardar

- captura de las restricciones nuevas en SQL Developer (`USER_CONSTRAINTS`).
- captura de `02_verificar_estructura.sql` terminando con `ok`.
- captura de `03_prueba_facturacion.sql` terminando con `ok`.
- captura de las tablas `PLANS`, `SUBSCRIPTIONS` y `PAYMENTS` en el navegador SQL.
