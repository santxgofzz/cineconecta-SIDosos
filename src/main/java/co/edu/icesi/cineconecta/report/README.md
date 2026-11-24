# Módulo report

El informe de ingresos y cantidad de suscripciones por plan está disponible en
`GET /reportes/ingresos-por-plan`. La página recibe fechas inclusivas; el
servicio convierte la fecha final al límite exclusivo del día siguiente antes
de consultar Oracle con `NamedParameterJdbcTemplate`.

La consulta SQL ejecutable directamente en Oracle está en
`database/oracle/06_report_ingresos_y_suscripciones_por_plan.sql`.
La conexión se configura con `ORACLE_JDBC_URL`, `ORACLE_USERNAME` y
`ORACLE_PASSWORD`, según `src/main/resources/application.yml`. También se
requiere `MONGO_URI` para iniciar la aplicación. Spring no carga `.env`
automáticamente; define esas variables en el entorno o en la configuración de
ejecución del IDE. El proyecto aún no incluye el script de creación de las
tablas Oracle ni datos semilla.
