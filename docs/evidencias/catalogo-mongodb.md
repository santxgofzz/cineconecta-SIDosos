# Evidencia de catálogo y administración

Ejecución del 24 de noviembre de 2026 en Windows con Corretto 21.0.6,
Maven 3.9.16, MongoDB local 9.0.8 y mongosh 2.14.2. La base principal
de esta ejecución aislada fue `cineconecta_test_j5_johan`.

## Scripts MongoDB

Desde la raíz, sobre una base nueva de prueba

```powershell
$env:CINE_MONGO_TARGET_DB = 'cineconecta_test_j5_johan'
$env:CINE_CATALOG_TEST_URI = 'mongodb://127.0.0.1:27017/cineconecta_test_j5_johan?directConnection=true'
1..2 | ForEach-Object {
    mongosh $env:CINE_CATALOG_TEST_URI --quiet --file database/mongodb/runtime/02_catalog_admin.js
    mongosh $env:CINE_CATALOG_TEST_URI --quiet --file database/mongodb/runtime/03_seed_catalog_dev.js
}
mongosh $env:CINE_CATALOG_TEST_URI --quiet --file database/mongodb/runtime/04_verify_catalog_test.js
```

| Control | Resultado observado |
| --- | --- |
| `node --check`, tres scripts Mongo y `catalog.js` | Salida 0 para cada archivo. |
| Migración J1, dos ejecuciones | Salida 0 en ambas, validadores de `contents` y `admin_users` listos. |
| Semilla inicial y repetición | Salida 0 en ambas, 8 títulos nuevos y luego 0. |
| Verificador Mongo | Salida 0, 8 títulos, 3 tipos, 3 estados, 7 escrituras inválidas rechazadas e índice único comprobado. |
| Documento legado incompatible en otra base aislada | La migración devolvió 1. La comprobación posterior confirmó que el documento seguía presente y que la configuración previa se conservó. |

Para repetir el rechazo de migración, crea una colección sin validador
en otra base local `cineconecta_test_*` e inserta solo
`{_id: "LEGACY-J5", type: "series"}`. Ejecuta el script `02` y
comprueba el documento y las opciones con `db.contents.findOne()` y
`db.getCollectionInfos({name: "contents"})`.

## Pruebas de integración del módulo

| Comando | Salida | Resultado |
| --- | --- | --- |
| `mvn -B "-Dtest=CatalogRetirementMongoTest,AdminLoginMongoTest" test` | 0 | 2 ejecutadas, 0 fallos, 0 errores, 0 omitidas. |
| `mvn -B test` | 0 | 34 ejecutadas, 0 fallos, 0 errores, 0 omitidas con MongoDB local habilitado. |
| `mvn -B -DskipTests package` | 0 | JAR ejecutable de Spring Boot generado. |

- `AdminLoginMongoTest` usó MongoDB y el proveedor Spring Security con
  hashes BCrypt generados en ejecución. MockMvc comprobó las respuestas de
  login para cuenta activa, contraseña incorrecta y cuenta inactiva.
- `CatalogRetirementMongoTest` leyó la proyección de episodios con el gateway
  de playback, agregó a Mi lista y comparó los tres documentos BSON de
  actividad antes y después de retirar. Mi lista marcó el título como
  retirado y una nueva reproducción se denegó. Los documentos de sesión y
  progreso eran fixtures y Oracle se simuló.
- Las pruebas del catálogo ejercitaron filtros, paginación y estados en
  MongoDB. Las pruebas de autorización y de fallos de conexión simularon
  respuestas de Oracle o Mongo. Las vistas se renderizaron con MockMvc.

Pendientes de integración del equipo

- Recorrido visual con sesión de titular contra Oracle real. El módulo
  cliente y el esquema de pruebas no estaban configurados en este entorno.
- Guardado automático de progreso y Continuar viendo, que están pendientes en
  el contrato de playback del que partió esta rama.
- Medición comparativa de ambos motores. El
  [anexo](../modelos/catalogo-mongodb.md) deja la consulta de MongoDB y los
  contadores que debe registrar el equipo.
