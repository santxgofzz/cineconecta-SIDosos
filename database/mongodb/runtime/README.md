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

## Pendiente

1. Script de `contents` y `admin_users` para la BD `cineconecta` (catálogo/admin).
2. Las referencias de la demo usan los IDs de `database/oracle/03_seed.sql`;
   si se cambian, actualizar ambos lados antes de cargar actividad en MongoDB.
3. Crear administrador mediante hash BCrypt real generado por el servidor.
4. Separar carga de demo/semillas de benchmarking; NO restaurar datos de demo
   sobre una BD compartida sin revisarlos.
