# santiago - s2: contratacion simulada con oracle

este commit implementa la compra desde spring boot sin depender de las pantallas del equipo. la compra y su pago quedan en oracle, y **ningun dato de facturacion se escribe en mongodb**.

## 1. antes de arrancar

- java 21 o superior para compilar a release 21, y maven.
- oracle `ESTUD` accesible con las variables `ORACLE_JDBC_URL`, `ORACLE_USERNAME` y `ORACLE_PASSWORD`.
- tablas `ACCOUNTS`, `PLANS`, `PROFILES`, `SUBSCRIPTIONS`, `PAYMENTS` creadas con el esquema del equipo.
- ejecutar `database/oracle/subscriptions/01_reglas_facturacion.sql` del s1 para crear `CC_SUBSCRIPTIONS_SEQ`, `CC_PAYMENTS_SEQ` y las restricciones.
- tener al menos una cuenta y un plan reales en oracle; se puede usar la carga sintetica acordada con el equipo.
- mongo atlas tambien tiene que estar configurado con `MONGO_URI`, porque el proyecto completo inicia los modulos mongo existentes.
- para probar sin esperar el login real de clientes, activar el perfil `dev` que ya existe en `DevAccountUserConfig`: `DEV_ACCOUNT_ID` tiene que corresponder a un `ACCOUNTS.accountId` existente.

## 2. endpoints

| metodo | ruta | uso |
|---|---|---|
| GET | `/api/subscriptions/plans` | listar planes de oracle |
| GET | `/api/subscriptions/history` | historial del titular autenticado, con estado de vigencia |
| POST | `/api/subscriptions/purchase` | contratar un plan con pago simulado |

cuerpo de ejemplo:

```json
{"planId": 1, "approvePayment": true}
```

`approvePayment=false` responde `PAYMENT_REJECTED` (400) y **no escribe** registros. una compra exitosa devuelve 201 con el identificador de suscripcion, monto real leido del plan, referencia, inicio y fin. no se acepta `amount`, `accountId` ni `price` del navegador.

el endpoint POST necesita una sesion cliente autenticada y un token CSRF valido. en el perfil `dev` se puede entrar desde `/login` con el usuario y clave configurados. sin login no debe poderse comprar. no desactivar csrf para facilitar las pruebas.

## 3. operaciones de la transaccion

1. se rechaza inmediatamente un pago simulado no aprobado.
2. se bloquea la fila de `ACCOUNTS` con `SELECT ... FOR UPDATE WAIT 20` durante la transaccion.
3. se consulta el plan, evitando aceptar precio enviado por el cliente.
4. se revisa si existe cualquier suscripcion superpuesta con el periodo nuevo `[inicio, fin)`.
5. se compara el numero de perfiles activos con `PLANS.maxProfiles`.
6. se obtienen los ids de las secuencias creadas en s1.
7. se insertan `SUBSCRIPTIONS` y `PAYMENTS` dentro de la misma transaccion oracle.
8. si falla cualquiera de las escrituras, spring hace rollback de ambas.

las fechas se representan en utc sobre columnas oracle `TIMESTAMP` sin zona. la suscripcion dura exactamente 30 dias, y se considera vigente desde el inicio inclusive hasta el fin exclusivo.

**ojo:** el bloqueo protege las contrataciones que pasan por este servicio. para demostrar la concurrencia y el rollback con dos conexiones reales, hacemos la prueba de integracion s3; las pruebas unitarias s2 no reemplazan esa evidencia.

## 4. pruebas automaticas

```powershell
mvn -Dtest=BillingServiceTest,BillingControllerTest test
mvn clean test
```

estas pruebas no necesitan conectarse a oracle y comprueban pagos rechazados, limites, cruces de periodos, referencia de pago, no confiar en el `accountId` del cliente y comportamiento del servicio ante errores. `BillingServiceTest` verifica la declaracion transaccional, pero no un rollback real; ese queda en s3.

## 5. como probar con oracle (con el login listo)

1. ejecutar `mvn spring-boot:run` con todas las variables de entorno necesarias.
2. ingresar desde el navegador con un cliente valido (o perfil `dev` con cuenta existente).
3. consultar `GET /api/subscriptions/plans` y comprobar que aparecen los planes de oracle.
4. llamar a `POST /api/subscriptions/purchase` incluyendo cookies de login y token csrf.
5. comprobar 201 y consultar `GET /api/subscriptions/history`.
6. verificar en sql developer con la consulta de abajo, cambiando `:id_cuenta` por la cuenta de prueba.
7. repetir compra durante la vigencia: debe devolver `SUBSCRIPTION_OVERLAP` (409).
8. probar `approvePayment=false`: debe devolver `PAYMENT_REJECTED` (400) sin registrar filas.

```sql
SELECT s.subscriptionId, s.ACCOUNTS_accountId, s.startsAt, s.endsAt,
       p.paymentId, p.amount, p.reference
  FROM SUBSCRIPTIONS s
  LEFT JOIN PAYMENTS p ON p.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
 WHERE s.ACCOUNTS_accountId = :id_cuenta
 ORDER BY s.startsAt DESC;
```

## 6. evidencias que hay que guardar

- consola maven con las pruebas unitarias del modulo pasando.
- respuesta 201 de la compra y respuesta 400 del pago rechazado.
- respuesta 409 al intentar comprar con una suscripcion vigente.
- sql developer mostrando `SUBSCRIPTIONS` y `PAYMENTS` relacionados para esa cuenta, sin filas parciales.
- para la sustentacion s3: dos peticiones concurrentes y rollback forzado medidos en oracle.

## 7. decisiones de integracion

- el servicio lee las tablas con jdbc en vez de duplicar entidades que otro integrante aun esta preparando.
- el repositorio `BillingRepository` es una interfaz; las pruebas lo pueden simular sin depender de la base de otro modulo.
- no se cambian `pom.xml`, `application.yml`, configuraciones de seguridad, catalogo ni reproduccion.
- `@Transactional` usa el gestor relacional por defecto de spring boot. si el equipo crea otro transaction manager para mongo, hay que calificar expresamente el de oracle para no mezclar transacciones.
- el estado `approvePayment` es una simulacion academica; no es una integracion de cobro real y no debe exponerse como pago real en produccion.

> actualizacion s3: el tiempo de espera del bloqueo de cuenta paso a 20 segundos. las pruebas de concurrencia y rollback estan en `docs/pruebas/santiago-s3-concurrencia-rollback.md`.
