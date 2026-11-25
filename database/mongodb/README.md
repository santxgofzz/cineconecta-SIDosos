# MongoDB

## `demo/`: material original del equipo (sin modificaciones)

El ZIP recibido apuntaba a la BD `cineconecta_modelo_demo`. Sus scripts se
conservan bajo `demo/` para obtener el diagrama de Compass y reproducir datos
sintéticos **aislados**. Incluyen:

- `01_crear_colecciones_validadores_indices.js`
- `02_cargar_datos_sinteticos.js`
- `03_verificar_datos_y_consultas.js`
- `json/*.json`
- `LEEME_PASO_A_PASO.md`

**No conectes el backend real a esa base demo**: contiene actividad sintetica y
un administrador desactivado con hash marcador. Sus IDs de perfil/suscripcion
coinciden con `database/oracle/03_seed.sql`, pero MongoDB no valida esas
referencias ni confirma que la semilla se haya ejecutado en tu Oracle.

## `runtime/`: adaptación para la aplicación real

Contiene los validadores e índices de `cineconecta`, el catálogo y la actividad
de reproducción, así como la semilla local de sesiones del informe de consumo.
Los scripts no borran datos de producción; revisa el README de runtime antes de
ejecutarlos en una base compartida.

## `benchmark/`: volumen y planes de ejecución

Usa la base aislada `cineconecta_benchmark` y la colección
`benchmark_viewing_sessions` para comparar `COLLSCAN` e `IXSCAN` con 500000
sesiones. Las instrucciones comunes y la comparación con PostgreSQL están en
`database/benchmark/README.md`.
