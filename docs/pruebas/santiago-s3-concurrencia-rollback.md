# santiago - s3: concurrencia y rollback en oracle

## que cambia este commit

- se mantiene `@Transactional(rollbackFor = Exception.class)` en `BillingService.purchase` del s2; no se crea una transaccion distribuida con mongo.
- el `SELECT accountId ... FOR UPDATE WAIT 20` bloquea las compras de **la misma cuenta** hasta que termina la transaccion. las compras de otras cuentas no se bloquean por esta regla.
- se agregan **tres pruebas de integracion real con oracle** y consultas SQL para las evidencias de la sustentacion.
- la prueba de fallo usa un repositorio *espia solo en el contexto de test*. no se agrega un endpoint de sabotaje, ni un parametro de fallo que pueda utilizar un cliente.
- este cambio no toca `pom.xml`, `application.yml` ni los modulos de los compañeros.

## requisitos antes de ejecutar

1. estar en la rama `feature/subscriptions-payments`, con s1 y s2 ya aplicados.
2. java 21 o superior en `mvn -version`.
3. tener creadas las tablas oracle `ACCOUNTS`, `PLANS`, `PROFILES`, `SUBSCRIPTIONS` y `PAYMENTS`.
4. haber ejecutado el s1 `01_reglas_facturacion.sql` para crear `CC_SUBSCRIPTIONS_SEQ`, `CC_PAYMENTS_SEQ` y las restricciones.
5. configurar en **la misma terminal** `ORACLE_JDBC_URL`, `ORACLE_USERNAME` y `ORACLE_PASSWORD` con los datos del usuario de oracle **autorizado para la prueba**. no pegar credenciales en capturas ni subirlas a git.
6. comprobar que el **usuario de pruebas/esquema** puede insertar y borrar **datos sintéticos temporales** en `ACCOUNTS`, `PLANS`, `SUBSCRIPTIONS` y `PAYMENTS`. no amplien los permisos del usuario de la aplicación para correr estas pruebas: la app debe usar una cuenta con privilegios mínimos; el test puede utilizar una cuenta separada autorizada para los datos sintéticos. las pruebas no modifican cuentas existentes.

la prueba utiliza `@JdbcTest` y no arranca los modulos mongo: **no requiere `MONGO_URI`**. no cambia configuraciones globales ni deshabilita seguridad de la aplicacion.

## comandos para las pruebas

```powershell
# en la misma terminal con las variables oracle ya configuradas
mvn clean compile
mvn '-Dtest=BillingServiceTest,BillingControllerTest' test

# esto habilita expresamente las pruebas con escrituras reales en ESTUD
$env:RUN_ORACLE_IT = 'true'
mvn '-Dtest=BillingOracleIntegrationTest' test
Remove-Item Env:RUN_ORACLE_IT -ErrorAction SilentlyContinue
```

**importante:** sin `RUN_ORACLE_IT=true`, las pruebas de integracion se omiten por seguridad. un `BUILD SUCCESS` que diga `Tests run: 0` o `Skipped: 3` **no** es una evidencia valida de concurrencia. para cerrar s3 deben pasar los tres metodos de `BillingOracleIntegrationTest`.

## que comprueba cada prueba

| prueba | lo que debe ocurrir |
|---|---|
| `dosContratacionesSimultaneasDejanUnaSolaSuscripcionYPago` | dos hilos inician juntos y compran para la misma cuenta. una compra sale bien, la otra responde `SUBSCRIPTION_OVERLAP`. al consultar oracle hay una suscripcion, un pago y cero suscripciones sin pago. |
| `falloAntesDeGuardarPagoRevierteLaSuscripcionEnOracle` | se inserta una suscripcion real, luego el repositorio de prueba lanza una excepcion al tratar de insertar el pago. spring revierte la transaccion y oracle queda con cero filas en ambas tablas para esa cuenta. |
| `pagoRechazadoNoEscribeEnOracle` | un pago simulado rechazado no crea ni suscripcion ni pago. |

cada test crea una cuenta y un plan **exclusivamente de prueba** (con correo en `example.invalid`). en `@AfterEach` borra lo que haya creado, por `accountId` y en orden de claves foraneas. si se cierra el proceso a la fuerza, esa limpieza podria no ejecutarse; en ese caso verificar los registros con prefijo `s3.` / `S3-` antes de eliminarlos **manualmente**. no usar `DELETE` masivos ni `TRUNCATE` en la base compartida.

las secuencias oracle pueden avanzar aunque haya rollback. los saltos en el consecutivo **no son registros parciales**.

## para mostrarlo en la sustentacion

1. sacar captura de Maven con `BillingOracleIntegrationTest`: **3 pruebas**, `Failures: 0`, `Errors: 0`, `Skipped: 0`.
2. mostrar la salida `s3 ok: 2 compras simultaneas; 1 confirmada, 1 rechazada...`.
3. mostrar la salida `s3 ok: fallo antes de pago; rollback real...`.
4. ejecutar en SQL Developer `database/oracle/subscriptions/04_consultas_evidencia_s3.sql` sobre una cuenta de **demo** y mostrar `periodos_superpuestos = 0` y `suscripciones_sin_pago = 0`.
5. si quieren mostrar **las filas temporales** de la prueba JUnit antes de limpiarlas, se debe agregar un mecanismo controlado de observacion en una version posterior. la prueba actual valida por consultas **antes** de borrar, pero los datos no quedan en ESTUD para evitar contaminar la BD del equipo.
6. para la demostracion de dos **solicitudes HTTP** en vivo, usar dos clientes autenticados sobre la misma cuenta y la ruta del s2 `POST /api/subscriptions/purchase`, manteniendo CSRF. s3 automatiza la carrera sobre el **mismo servicio transaccional** invocado por esa ruta, no simula una sesion HTTP real.

## limite importante para el informe

este mecanismo protege las compras realizadas por el servicio `BillingService` porque todas bloquean la misma fila de `ACCOUNTS`. cualquier otro codigo que inserte suscripciones sin ese bloqueo podria evadir la regla; los otros modulos deben usar el mismo servicio o una regla equivalente. de forma parecida, los cambios concurrentes de perfiles deben coordinarse con el modulo de aponza usando el mismo bloqueo de cuenta para que el limite se cumpla en todos los flujos.

no se afirma que hayamos probado la conexion del computador del usuario desde aqui. las medidas de rendimiento y los planes `EXPLAIN` de la rubrica se documentan por separado: s3 es la evidencia de **atomicidad y concurrencia**.
