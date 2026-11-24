# Pruebas y trazabilidad de catálogo

Evidencia de la rama de Johan Mora. Los estados describen qué se ejecutó y
en qué entorno. Oracle simulado significa que la prueba no abrió una
conexión a la base relacional.

| Requisito | Implementación | Prueba | Evidencia | Estado |
| --- | --- | --- | --- | --- |
| RF-09, autenticación admin | Sesión admin aislada, proveedor Mongo y BCrypt | `AdminLoginMongoTest`, `AdminCatalogSecurityTest` | Login activo, rechazo de clave errónea e inactivo, permisos y CSRF | Mongo verificado, MVC controlado |
| RF-10, alta y edición de contenidos | `AdminCatalogService`, `contents`, Bean Validation y ETag | `AdminCatalogMongoTest`, `AdminCatalogServiceTest` y script `04_verify_catalog_test.js` | Tres variantes, revisiones, rechazo BSON y validación de campos | Mongo verificado |
| RF-11, publicar y retirar | Estado dentro del documento, sin borrado físico | `AdminCatalogMongoTest`, `CatalogRetirementMongoTest` | Transiciones y documentos de actividad iguales antes y después del retiro | Mongo verificado, Oracle simulado |
| RF-12, series | Temporadas y episodios embebidos, números e IDs estables | `CatalogPersistenceTest`, `AdminCatalogServiceTest`, `AdminCatalogMongoTest` | Mapeo BSON, proyección playback y rechazo de duplicados o pérdida de IDs | Verificado |
| RF-13, búsqueda y paginación | `CatalogService`, criterios Mongo y validación de perfil/suscripción | `CatalogQueryMongoTest`, `CatalogServiceTest`, `CatalogPagesTest` | Filtros combinados, conteo, paginación, estado, edad, perfil y suscripción | Mongo verificado, Oracle simulado |
| RF-14, detalle | API y vista autorizadas, temporadas ordenadas | `CatalogQueryMongoTest`, `CatalogPagesTest` | Acceso directo, episodios, renderizado, enlaces y escape HTML | Mongo verificado, MVC controlado, Oracle simulado |
| RNF-01/02, credenciales y autorización | BCrypt, exclusión del hash, roles y CSRF | `CatalogPersistenceTest`, `CatalogMongoIntegrationTest`, `AdminLoginMongoTest`, `AdminCatalogSecurityTest` | Serialización, bootstrap, login y rutas protegidas | Verificado en el entorno de prueba |
| RNF-05/06, referencias y fallos | `CatalogGateway`, `ProfileGuard`, errores explícitos y retiro lógico | `CatalogRetirementMongoTest`, `CatalogServiceTest` | Actividad preservada, gateway de playback, fallos Oracle/Mongo | Mongo verificado, fallos simulados |
| RNF-07 a RNF-11, aspectos aplicables al catálogo | Paginación, índices, validación documental, setup reproducible y vistas | `CatalogQueryMongoTest`, `CatalogPagesTest`, scripts Mongo | Plan reproducible y controles; sin mediciones comparativas ni recorrido visual en navegador con ambas BD | Parcial, integración y benchmarking pendientes |

Los resultados y comandos de la ejecución se registran en
[evidencia de catálogo](../evidencias/catalogo-mongodb.md).

## Repetir la verificación

Desde la raíz del repositorio y con Java 21, Maven y mongosh disponibles,
selecciona una base nueva y local con prefijo `cineconecta_test_`. Ejecuta
la [migración y semilla dos veces](../../database/mongodb/runtime/README.md)
antes de correr las pruebas Java. El verificador espera ocho títulos de
semilla y las pruebas Java agregan sus propios documentos ficticios.

```powershell
$env:CINE_CATALOG_TEST_URI = $catalogUri
mvn -B "-Dtest=CatalogRetirementMongoTest,AdminLoginMongoTest" test
mvn -B test
mvn -B -DskipTests package
```

Las pruebas Mongo se omiten si `CINE_CATALOG_TEST_URI` no corresponde a una
URI directa de loopback con nombre de base de pruebas. Las pruebas web
usan MockMvc. El recorrido con sesión de titular y Oracle real requiere
un esquema de pruebas y la integración del módulo de cuentas. La prueba
de retiro comprueba autorización de playback y preservación de progreso
preexistente con un fixture. El guardado automático de progreso y
Continuar viendo están pendientes en la base de playback de esta rama.
