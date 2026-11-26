# Módulo playback — reproducción simulada, progreso e historial

Rama `feature/playback-activity` (F1–F5). Requisitos: RF-16 a RF-21, RNF-02, RNF-06, RNF-07.

## API (cuenta tomada de la sesión, nunca del cliente)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/profiles/{profileId}/playback/sessions` `{contentId, episodeId?}` | Autoriza (Oracle + Mongo) y crea una sesión nueva |
| GET  | `/api/profiles/{profileId}/playback/sessions/{sessionId}` | Estado de la sesión |
| POST | `/api/profiles/{profileId}/playback/sessions/{sessionId}/events` `{seq, type, seconds?, positionSeconds?}` | `WATCH`, `SEEK`, `PAUSE`, `FINISH` |
| GET  | `/api/profiles/{profileId}/continue-watching?limit=12` | Pendientes por actividad reciente |
| GET  | `/api/profiles/{profileId}/history?page=0&size=20` | Sesiones históricas |

Páginas: `/profiles/{profileId}/player?contentId=…&episodeId=…` y `/profiles/{profileId}/activity`.

## Reglas implementadas

- **Autorización** (`PlaybackAuthorizationService`): primero Oracle (perfil propio y activo,
  suscripción vigente `[STARTS_AT, ENDS_AT)`), luego Mongo (publicado, `ageRating <= maxAgeRating`,
  series solo por episodio existente). Si una BD no responde → 503, nunca se autoriza.
- **Tiempo visto ≠ posición**: `WATCH` suma a ambos; `SEEK` solo mueve la posición. El tiempo
  declarado se limita al tiempo real transcurrido + tolerancia (`cineconecta.playback.watch-tolerance-seconds`).
- **Sesiones**: una por inicio, nunca se borran. `PAUSE`/`FINISH` la cierran; reanudar crea otra.
  Una sesión abandonada se cierra como `interrupted` al volver a iniciar esa unidad.
- **Progreso**: único por `(profileId, unitKey)`; completado al finalizar o con posición ≥ 90 %.
  Ver de nuevo algo completado empieza en 0 y restablece el progreso, sin tocar el historial.
- **Reintentos**: cada evento lleva `seq`; si ya se aplicó, se ignora (y se re-sincroniza el progreso).
  Las escrituras en Mongo son condicionales/idempotentes y se reintentan ante fallos transitorios
  (`cineconecta.retry.max-attempts`, `cineconecta.retry.backoff-millis`).

## Supuestos a alinear con el equipo

- `JdbcAccountDataGateway` usa `PROFILES(PROFILEID, ACCOUNTS_ACCOUNTID, MAXAGERATING, ISACTIVE)` y
  `SUBSCRIPTIONS(SUBSCRIPTIONID, ACCOUNTS_ACCOUNTID, STARTSAT, ENDSAT)`, según
  `database/oracle/01_schema.sql`. Un perfil está activo con `ISACTIVE = 'A'`
  (`DM_PROFILE_STATES`). Si el esquema cambia, solo se ajustan esas dos sentencias.
- La autenticación real de titulares debe devolver un principal que implemente
  `common.security.AccountPrincipal`. Mientras tanto existe `DevAccountUserConfig` (solo perfil `dev`).
- El catálogo se lee por proyección de `contents` (`MongoCatalogGateway`); no se definió un documento
  propio para no chocar con `feature/mongo-catalog`.

## Probar

```powershell
mvn test   # 84 pruebas unitarias, sin necesidad de BDs
```

Las pruebas con Oracle/Mongo reales (evidencia para `docs/evidencias/`) siguen pendientes.
