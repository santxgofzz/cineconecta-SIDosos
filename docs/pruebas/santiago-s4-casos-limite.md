# santiago - s4: pruebas avanzadas de facturacion

## objetivo

este commit completa las pruebas aisladas de suscripciones y pagos sin tocar la implementacion de los compañeros. el foco es comprobar casos limite que no alcanzamos a cubrir con las diez pruebas de s2 ni con las tres pruebas reales de concurrencia y rollback de s3.

el codigo de produccion, los endpoints, el esquema y los archivos de configuracion **no cambian** en s4.

## archivos y responsabilidad

| archivo | para que sirve |
|---|---|
| `BillingAdvancedRulesTest.java` | casos limite del servicio, ventanas de vigencia, historial y cupos |
| `BillingJdbcRepositoryContractTest.java` | comprueba sentencias, parametros y conversiones jdbc sin conectarse a oracle |
| `BillingHttpContractAdvancedTest.java` | respuestas http, validacion de entrada y uso del titular autenticado |
| `05_consultas_evidencia_s4.sql` | consultas de solo lectura para usar en sql developer durante la demo |
| `santiago-s4-casos-limite.md` | esta guia, matriz y evidencias |

## pruebas que quedan cubiertas

| grupo | casos a verificar | resultado esperado |
|---|---|---|
| solicitud invalida | cuenta sin id valido, plan inexistente o negativo, decision de pago faltante | respuesta de error; sin escrituras |
| pago | rechazo, precio negativo o nulo, plan sin cupos | no se genera recibo de compra |
| suscripcion | periodo superpuesto y plan con cupos inferiores al total de perfiles | se rechaza antes de reservar ids |
| limite exacto | numero de perfiles activos igual al maximo del plan | se permite la compra |
| fechas | 30 dias exactos, UTC, inicio incluido y fin excluido | comparacion correcta en el borde |
| historial | se muestran los planes historicos y se calcula vigencia por cada registro | no se reescribe el plan pasado |
| errores de almacenamiento | falla del insert de suscripcion o del insert de pago | no se responde como compra exitosa |
| jdbc | consultas con `FOR UPDATE WAIT 20`, intervalos `[inicio, fin)`, inserts y secuencias | se comprueba el contrato del repositorio |
| http | cliente autenticado, historial propio, validacion del body, pago rechazado | no se usa el id enviado por el navegador |

**alcance de la evidencia:** estas pruebas trabajan con dobles y mocks; no ejecutan un rollback en oracle ni prueban la cadena de seguridad completa de spring security. esos dos aspectos se demuestran con las pruebas de s3 y la demo autenticada. tampoco se consideran mediciones de rendimiento.

## como ejecutar

**importante:** usar java 21 o superior en `mvn -version`.

```powershell
# en feature/subscriptions-payments
mvn clean compile
mvn '-Dtest=BillingAdvancedRulesTest,BillingJdbcRepositoryContractTest,BillingHttpContractAdvancedTest' test

# verificar que s2 sigue pasando
mvn '-Dtest=BillingServiceTest,BillingControllerTest' test
```

las pruebas nuevas **no necesitan** `ORACLE_JDBC_URL`, `ORACLE_USERNAME`, `ORACLE_PASSWORD` ni `MONGO_URI`, porque no abren conexiones. solo usan los servicios y el repositorio simulados.

para repetir s3 sobre la base **de pruebas autorizada**, con la red y variables oracle reales:

```powershell
$env:RUN_ORACLE_IT = 'true'
mvn '-Dtest=BillingOracleIntegrationTest' test
Remove-Item Env:RUN_ORACLE_IT -ErrorAction SilentlyContinue
```

no confundas las pruebas omitidas por `RUN_ORACLE_IT` con pruebas exitosas. s3 solo se puede dar como aprobado si las tres se ejecutaron con `Failures: 0, Errors: 0, Skipped: 0`.

## consultas oracle para sustentacion

abre `database/oracle/subscriptions/05_consultas_evidencia_s4.sql` en sql developer, conectado al esquema donde el equipo registro las compras de la demo. ejecuta con **f5** e ingresa el `accountId` de una **cuenta de prueba**.

el script **no crea, actualiza ni borra** registros; muestra el plan historico de cada suscripcion, si esta vigente, la cantidad de perfiles activos frente al maximo y las dos verificaciones de integridad (sin pago / solapamiento). si no existen registros para la cuenta, el resultado vacio no demuestra que el flujo de compra funcione: primero realicen la demo con una cuenta autorizada.

la fecha actual de la consulta se calcula en utc para mantener la convencion de `TIMESTAMP` sin zona que utiliza el modulo.

## evidencias que debes guardar

1. captura de maven con `BillingAdvancedRulesTest`, `BillingJdbcRepositoryContractTest` y `BillingHttpContractAdvancedTest` ejecutados sin fallas.
2. captura de los diez tests s2 aprobados despues de integrar s4.
3. captura de las tres pruebas s3 **si realmente se ejecutaron contra oracle** (no incluir si se omitieron).
4. captura de sql developer con la cuenta de demo y su historial de planes, junto a los indicadores `suscripciones_sin_pago = 0` y `periodos_superpuestos = 0`.
5. estado de git y hash del commit s4. no incluir credenciales ni URI completas en las capturas.

## comandos de git

```powershell
git switch feature/subscriptions-payments
git status

git add src/test/java/co/edu/icesi/cineconecta/subscription/BillingAdvancedRulesTest.java
git add src/test/java/co/edu/icesi/cineconecta/subscription/BillingJdbcRepositoryContractTest.java
git add src/test/java/co/edu/icesi/cineconecta/subscription/BillingHttpContractAdvancedTest.java
git add database/oracle/subscriptions/05_consultas_evidencia_s4.sql
git add docs/pruebas/santiago-s4-casos-limite.md

git diff --cached --stat
git commit -m 'test(billing): cover purchase boundaries and oracle jdbc contracts'
git push origin feature/subscriptions-payments
```

**ojo:** hacer el commit solamente cuando las pruebas locales hayan terminado bien. si `git diff --cached --stat` muestra archivos de otros modulos, deshacer su preparacion antes de confirmar.

## que queda para s5

interfaz de contratacion, seleccion de plan e historial para el titular, con mensajes claros de rechazo y validacion en navegador. el backend del s2 sigue siendo la fuente de verdad para reglas y autorizacion.
