# MongoDB de la aplicación (`cineconecta`)

## Hecho

- `01_playback_activity.js`: crea/actualiza `my_list`, `viewing_sessions` y
  `viewing_progress` con `$jsonSchema` estricto e índices (rama
  `feature/playback-activity`). Idempotente; no borra datos.
  La aplicación además verifica los mismos índices al arrancar
  (`MongoActivityIndexes`, desactivable con `cineconecta.mongo.ensure-indexes=false`).

```powershell
mongosh "mongodb://localhost:27017" --file database/mongodb/runtime/01_playback_activity.js
```

Para probar el informe de consumo con las suscripciones de
`database/oracle/03_seed.sql`, el fixture local
`05_seed_consumption_demo.js` agrega diez sesiones a `cineconecta`. Inserta
solo IDs de sesión que falten; no reemplaza ni elimina documentos.

Si `mongosh` no está instalado en Windows, ejecuta el fixture desde el
contenedor Mongo local:

```powershell
docker cp .\database\mongodb\runtime\05_seed_consumption_demo.js cineconecta-mongodb:/tmp/05_seed_consumption_demo.js
docker exec cineconecta-mongodb mongosh "mongodb://localhost:27017/cineconecta" --file /tmp/05_seed_consumption_demo.js
```

Usa el periodo `2026-11-01` a `2026-12-01` en
`/admin/reportes/consumo-por-plan` para incluir las sesiones de ejemplo.

## Catálogo y administradores

En una instalación de desarrollo de la aplicación, ejecuta primero
`01_playback_activity.js` para la actividad y luego `02_catalog_admin.js`
para catálogo y administración. El script de actividad selecciona
`cineconecta` explícitamente. El ejemplo aislado de catálogo que sigue
ejecuta únicamente el script de catálogo y sus semillas. Evita ejecutar
el script de actividad en un servidor que contenga una base de uso
compartido durante las pruebas.

`02_catalog_admin.js` prepara `contents` y `admin_users`. Ejecuta la migración
con un usuario de despliegue autorizado a listar colecciones, leer los
documentos para verificar compatibilidad, crear colecciones e índices y
ejecutar `collMod`. Si detecta documentos incompatibles o nombres de usuario
duplicados, se detiene antes de modificar los validadores. Haz la migración
sin otros escritores activos. La cuenta habitual de la aplicación solo
necesita permisos para sus operaciones de datos.

Ejemplo desde la raíz del repositorio para verificar en una base local nueva.
Usa la URI de `cineconecta` y el mismo nombre en `CINE_MONGO_TARGET_DB` para
instalar los validadores en la base de la aplicación.

```powershell
$env:CINE_MONGO_TARGET_DB = "cineconecta_test_catalog"
$catalogUri = "mongodb://127.0.0.1:27017/cineconecta_test_catalog?directConnection=true"
1..2 | ForEach-Object {
    mongosh $catalogUri --file database/mongodb/runtime/02_catalog_admin.js
    if ($LASTEXITCODE) { throw "Falló la migración" }
    mongosh $catalogUri --file database/mongodb/runtime/03_seed_catalog_dev.js
    if ($LASTEXITCODE) { throw "Falló la semilla" }
}
mongosh $catalogUri --file database/mongodb/runtime/04_verify_catalog_test.js
$env:CINE_CATALOG_TEST_URI = $catalogUri
mvn -B "-Dtest=CatalogPersistenceTest,CatalogMongoIntegrationTest" test
```

En mongosh, usa `db.getCollectionInfos({name: "contents"})`,
`db.contents.getIndexes()` y `db.admin_users.getIndexes()`. La semilla local
reutiliza únicamente los ocho títulos sintéticos con los IDs de la demo
referenciados por playback. Inserta por ID y conserva ediciones existentes.
La verificación espera esos ocho títulos e inserta un administrador ficticio
deshabilitado para comprobar que el índice rechaza duplicados. Conserva las
semillas de actividad y los datos de medición en sus propios conjuntos.

- `ix_contents_visibility` permite acotar por estado y clasificación,
  con `_id` como último componente para un desempate estable. Puede requerir
  ordenamiento adicional cuando la clasificación es un rango.
- `ix_contents_filters` usa el prefijo estado/tipo para explorar años y
  aplicar clasificación. Sin tipo pierde eficacia para consultar por año.
- J3 concretará la búsqueda de título. `ix_contents_title_text` de la demo
  funciona por términos de texto y no optimiza coincidencias parciales
  arbitrarias. El runtime deja pendiente ese índice hasta decidir la semántica.
  Componer un índice con varios arreglos independientes produce claves
  multikey paralelas incompatibles. Un índice único multikey tampoco
  valida números de temporadas y episodios dentro del mismo documento.
- `uq_admin_username` protege la unicidad del nombre normalizado.

El validador admite extensiones opcionales, exige los campos de cada tipo y
rechaza campos específicos de otros tipos. Usa años 1888–2100 y edades 0–18.
Se permiten listas vacías de subtítulos y reparto, y las fuentes y
entrevistas son opcionales.

### Administrador local opcional

Configura `MONGO_URI` apuntando a la base local migrada y las variables Oracle
que requiera el arranque de la aplicación. Activa temporalmente el perfil
`dev-admin-bootstrap` en `SPRING_PROFILES_ACTIVE`. Proporciona
`CINE_ADMIN_USERNAME` y `CINE_ADMIN_PASSWORD` mediante el entorno del proceso
o un gestor de secretos, sin escribir la contraseña en archivos ni en el
historial. El arranque usa el `PasswordEncoder` BCrypt de Spring y crea
la cuenta solo si el nombre normalizado es nuevo. Arrancar sin ambas
credenciales omite el bootstrap. Desactiva el perfil y retira las variables
cuando termine la preparación local. La autenticación de esta cuenta
se hace desde `/admin/login`. Para iniciar la aplicación,
configura `MONGO_URI`, `ORACLE_JDBC_URL`, `ORACLE_USERNAME` y
`ORACLE_PASSWORD` en el entorno de ejecución y usa `mvn spring-boot:run`.

Las referencias del fixture Mongo coinciden con los IDs de
`database/oracle/03_seed.sql`. Si cambias esos IDs, actualízalos en ambos lados
antes de cargar actividad en MongoDB. La semilla Oracle no carga documentos
MongoDB ni valida que el fixture se haya ejecutado en esa base.
