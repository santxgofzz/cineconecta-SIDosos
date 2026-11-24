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

- `02_verificar_playback_activity.js`: verificación **de solo lectura** de lo anterior.
  Comprueba validadores e índices, que las consultas del backend (Mi lista, Continuar
  viendo, historial, sesiones abiertas, informes por periodo) usen su índice sin
  `COLLSCAN` ni `SORT` en memoria, y avisa de datos inconsistentes (progreso ≥ 90 %
  sin completar, progreso huérfano, sesiones cerradas a medias). Sale con código 1 si
  algo falla; su salida sirve como evidencia para `docs/evidencias/`.

```powershell
mongosh "mongodb://localhost:27017" --file database/mongodb/runtime/02_verificar_playback_activity.js
# con un perfil concreto para los planes de consulta:
mongosh "mongodb://localhost:27017" --eval "var profileId = 7" --file database/mongodb/runtime/02_verificar_playback_activity.js
```

## Pendiente

1. Script de `contents` y `admin_users` para la BD `cineconecta` (catálogo/admin).
2. Alinear referencias `profileId` / `subscriptionId` con semillas Oracle reales.
3. Crear administrador mediante hash BCrypt real generado por el servidor.
4. Separar carga de demo/semillas de benchmarking; NO restaurar datos de demo
   sobre una BD compartida sin revisarlos.
