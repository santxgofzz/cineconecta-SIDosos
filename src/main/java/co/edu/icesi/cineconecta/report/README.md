# Módulo report

El centro administrativo de informes está en `GET /admin/reportes` y requiere
un administrador autenticado (`ROLE_ADMIN`). Desde allí se accede a cada informe.
El informe de ingresos y cantidad de suscripciones por plan está en
`GET /admin/reportes/ingresos-por-plan`; recibe fechas inclusivas y el servicio
convierte la fecha final al límite exclusivo del día siguiente antes de consultar
Oracle con `NamedParameterJdbcTemplate`. La ruta anterior
`GET /reportes/ingresos-por-plan` solo redirige al destino protegido.

Su exportación CSV está en `GET /admin/reportes/ingresos-por-plan.csv` y exige
los mismos parámetros `desde` y `hasta` que la pantalla. La descarga conserva
ese rango y agrega una fila de totales.

El informe de consumo está en `GET /admin/reportes/consumo-por-plan` y requiere
un administrador autenticado. Filtra sesiones MongoDB por `startedAt` en el
rango inclusivo de fechas locales (`cineconecta.report.time-zone`, por defecto
`America/Bogota`), y las agrega primero por `subscriptionId` y tipo de contenido.
Después resuelve cada suscripción en Oracle (`SUBSCRIPTIONS` → `PLANS`) y agrupa
por el plan histórico, sin consultar el plan actual de la cuenta. Muestra sesiones,
tiempo acumulado en segundos/hh:mm:ss y perfiles distintos por plan y tipo.
Si una sesión refiere a una suscripción inexistente en Oracle, conserva su consumo
en una fila identificada como `Sin plan histórico`.

La descarga CSV está en `GET /admin/reportes/consumo-por-plan.csv` y recibe los
mismos parámetros `desde` y `hasta`. La página y la descarga usan el mismo servicio
de agregación y validación del rango. Ambas rutas de informe y sus exportaciones
están dentro del espacio `/admin/**`, protegido por la cadena administrativa.

La consulta SQL ejecutable directamente en Oracle está en
`database/oracle/06_report_ingresos_y_suscripciones_por_plan.sql`.
La conexión se configura con `ORACLE_JDBC_URL`, `ORACLE_USERNAME` y
`ORACLE_PASSWORD`, según `src/main/resources/application.yml`. También se
requiere `MONGO_URI` para iniciar la aplicación. Spring no carga `.env`
automáticamente; define esas variables en el entorno o en la configuración de
ejecución del IDE. El esquema y sus datos sintéticos de desarrollo están en
`database/oracle/01_schema.sql` y `database/oracle/03_seed.sql`.
