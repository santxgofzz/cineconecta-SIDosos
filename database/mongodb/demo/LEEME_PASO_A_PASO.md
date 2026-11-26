# CineConecta — MongoDB Compass (PRUEBA DE MODELO)

Este paquete sirve **solo para construir un esquema demostrativo y obtener el diagrama** de la primera entrega. No incluye usuarios reales, Oracle, API, seguridad funcional ni una aplicación de streaming.

## Contenido

- `01_crear_colecciones_validadores_indices.js`: define las cinco colecciones (`contents`, `my_list`, `viewing_sessions`, `viewing_progress`, `admin_users`), validaciones `$jsonSchema` e índices.
- `02_cargar_datos_sinteticos.js`: agrega ejemplos coherentes con el documento de diseño (repetible por `_id`).
- `03_verificar_datos_y_consultas.js`: consultas de comprobación, de solo lectura.
- `json/*.json`: datos equivalentes en **Extended JSON canónico**, para importar manualmente desde Compass (alternativa al script 02).

## Base de destino

Los scripts solo trabajan con `cineconecta_modelo_demo`. No eliminan bases ni colecciones. Si ya tienes una base con este nombre, revisa antes de ejecutar.

## Opción A — Scripts con mongosh (recomendada)

1. Descomprime el ZIP, por ejemplo en `C:\CineConectaMongo\`.
2. Conecta MongoDB Compass a MongoDB local o Atlas.
3. En la conexión de Compass, abre el shell integrado con el icono `>_`.
4. Ejecuta estos tres comandos **en este orden**, reemplazando la ruta por la real de tu equipo:

```javascript
load("C:/CineConectaMongo/01_crear_colecciones_validadores_indices.js")
load("C:/CineConectaMongo/02_cargar_datos_sinteticos.js")
load("C:/CineConectaMongo/03_verificar_datos_y_consultas.js")
```

En Windows, en los comandos `load` usa `/` en la ruta o `\\` en vez de `\`.

Si `load()` no funciona en tu shell integrado, abre **mongosh instalado en Windows**, conectado al mismo MongoDB que Compass, y ejecuta allí los mismos comandos. Otra opción es `mongosh --file 01_crear_colecciones_validadores_indices.js` desde la terminal conectado al despliegue correcto.

**No uses `use` en los scripts:** todos se dirigen explícitamente a la BD `cineconecta_modelo_demo`.

## Opción B — Importar JSON directamente con Compass (sin mongosh)

1. Crea la base de datos `cineconecta_modelo_demo` en Compass y la primera colección `contents`.
2. Crea también `my_list`, `viewing_sessions`, `viewing_progress` y `admin_users` desde la vista de base de datos.
3. Entra a cada colección, selecciona `Add Data` → `Import JSON or CSV file`.
4. Elige el archivo `json/<nombre_coleccion>.json`, opción JSON, e importa.
5. Repite para las cinco colecciones y pulsa Refresh.

Esta opción crea y llena documentos. **No instala los validadores ni los índices personalizados** del script 01; si los necesitas, ejecuta el script 01 antes de importar. Para conservar correctamente `Date`, `Int32`, `Int64` y `ObjectId`, los archivos JSON usan Extended JSON canónico.

## Generar el diagrama en Compass

1. Selecciona `Data Modeling` en la barra lateral.
2. Clic en `Generate diagram` o `Generate new diagram`.
3. Elige tu conexión y la base `cineconecta_modelo_demo`, con nombre `CineConecta - Modelo documental`.
4. Selecciona las cinco colecciones. Usa muestreo `All documents` (son pocos registros) y la inferencia automática si aparece.
5. Clic `Generate`. Revisa `contents.seasons[].episodes[]` y que todas las colecciones tengan los tipos apropiados.
6. Ajusta **manualmente** las relaciones:
   - `my_list.contentId` → `contents._id` (muchos a uno).
   - `viewing_sessions.contentId` → `contents._id` (muchos a uno).
   - `viewing_progress.contentId` → `contents._id` (muchos a uno).
   - Opcional: `viewing_progress.lastSessionId` → `viewing_sessions._id` (referencia a la última sesión, que puede cambiar con el tiempo).
7. Las referencias `profileId` y `subscriptionId` apuntan a **Oracle, que no aparece en ese diagrama MongoDB**. Pon una nota lateral explicativa; NO inventes colecciones MongoDB para perfiles o suscripciones.
8. Exporta a PNG para el Word y a `.mdm` para compartir el diagrama editable.

**Nota:** Compass infiere relaciones de forma heurística y puede omitir alguna. Las relaciones del diagrama no son claves foráneas que MongoDB aplique automáticamente.

## Datos de prueba: IDs de Oracle de la demo

Los `profileId` 15, 16 y 21 y `subscriptionId` 8, 9 y 10 son referencias sinteticas. `database/oracle/03_seed.sql` crea los registros Oracle correspondientes y sus fechas cubren las sesiones de noviembre de este fixture. Ejecuta esa semilla en un esquema Oracle vacio/de prueba si quieres que las referencias coincidan entre ambos almacenes. Este script Mongo sigue apuntando a `cineconecta_modelo_demo` y no valida que la semilla se haya ejecutado en tu Oracle.

`admin_users` contiene una cuenta de demostración **desactivada** con un marcador de hash NO válido. No permite autenticarse. En el backend real creen un hash seguro y mantengan cuentas admin independientes.

## Notas adicionales

- Los campos `durationSeconds`, `watchedSeconds` y `lastPositionSeconds` se guardan en segundos.
- `contents.status` vale `draft`, `published` o `retired`.
- Series: temporadas y episodios **embebidos** en el documento de `contents`.
- `viewing_sessions`: historial de sesiones, no se sobrescribe.
- `viewing_progress`: último progreso por unidad reproducible.
- Para demostrar rendimiento en la entrega final se necesitará un volumen mayor; este conjunto pequeño solo es para el diseño y el diagrama.
