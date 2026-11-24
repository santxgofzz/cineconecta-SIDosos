# Módulo admin

La API administrativa usa una sesión y un proveedor de autenticación
independientes de los clientes. Para una prueba REST, pide el token con
`GET /admin/login` y conserva la cookie de sesión. Envía un formulario con
`username`, `password` y el parámetro CSRF a `POST /admin/login`.
El login devuelve 204 si las credenciales son válidas y 401 si se rechazan.
Después de iniciar sesión, obtén un token CSRF nuevo con la misma sesión y
envíalo en las solicitudes de escritura. `POST /admin/logout` cierra la
sesión administrativa.

`/admin/**` y `/api/admin/**` son espacios de administración, incluidos los
futuros informes. La cadena administrativa exige `ROLE_ADMIN` para estas
rutas, con acceso público a `/admin/login`. La autenticación de clientes
sigue en la cadena general y las operaciones de perfiles mantienen
`AccountPrincipal` como contrato.

La API usa `/api/admin/contents`. Permite listar por página, leer por ID,
crear con POST, editar con PUT y usar POST en `/{id}/publish` y
`/{id}/retire`. Crea contenidos en borrador. Repetir el estado vigente
conserva la revisión. Republicar conserva el mismo ID. Los contenidos se
retiran sin borrar documentos ni actividad histórica.

Para el PUT, usa el `ETag` de la lectura en `If-Match`. Mongo compara la
fecha de actualización antes de aplicar el cambio y devuelve 409 si otra
escritura se anticipó. Las revisiones avanzan en milisegundos incluso si el
reloj no ha avanzado. El tipo de contenido y la fecha de creación son
inmutables. Al editar series, conserva cada ID de episodio en su número
de temporada y episodio. Para agregar episodios, omite su ID para que el
servidor lo genere. Esta política conserva también episodios de borrador,
pues el documento no registra una fecha de primera publicación.
