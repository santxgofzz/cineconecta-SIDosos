# Módulo watchlist — Mi lista (RF-15)

| Método | Ruta | Resultado |
|---|---|---|
| GET    | `/api/profiles/{profileId}/my-list` | Lista con `available` / `availability` (`AVAILABLE`, `RETIRED`, `UNAVAILABLE`) |
| PUT    | `/api/profiles/{profileId}/my-list/{contentId}` | 201 si se agregó, 200 si ya estaba (idempotente) |
| DELETE | `/api/profiles/{profileId}/my-list/{contentId}` | 204 (idempotente; funciona con títulos retirados) |

- Sin duplicados: consulta previa + índice único `uq_my_list_profile_content` (cubre concurrencia).
- Solo se agregan contenidos publicados y permitidos por la clasificación del perfil.
- Los títulos retirados permanecen en la lista y se marcan como no disponibles.
- Perfil propio y activo verificado en Oracle (`integration.ProfileGuard`).
