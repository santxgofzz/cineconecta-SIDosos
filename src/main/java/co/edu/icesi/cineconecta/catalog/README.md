# Módulo catalog

La API de clientes ofrece `GET /api/profiles/{profileId}/catalog` y
`GET /api/profiles/{profileId}/catalog/{contentId}`. Usa la identidad de
la sesión del titular. Antes de consultar Mongo verifica en Oracle
la pertenencia y actividad del perfil y la vigencia de la suscripción.

| Parámetro | Semántica |
| --- | --- |
| `q` | Coincidencia parcial literal por título, sin distinguir mayúsculas y respetando las tildes. Hasta 100 caracteres. |
| `type` | `movie`, `series` o `documentary`. |
| `genre` | Igualdad exacta con un género guardado, hasta 80 caracteres. |
| `language` | Igualdad exacta en audio o subtítulos, hasta 40 caracteres. |
| `year` | Año exacto entre 1888 y 2100. |
| `page` / `size` | Página desde cero, tamaño de 1 a 100. Por defecto 0 y 20. |

Por ejemplo, con una sesión válida solicita
`GET /api/profiles/31/catalog?type=movie&genre=Drama&language=en&year=2022&q=ruta&size=10`.
La respuesta incluye los filtros efectivos, los elementos de la página y
los totales. Ordena por título e ID. Listado y conteo usan los mismos
criterios. Una edición entre ambas lecturas puede actualizar el total
respecto de la página, pues son consultas separadas.

El filtro de estado y clasificación aplica también al detalle. La API
devuelve 404 para títulos inexistentes o fuera de la vista autorizada,
403 para perfiles ajenos/inactivos o cuentas sin suscripción vigente,
400 para filtros inválidos y 503 si falla Oracle o MongoDB. Las temporadas
y episodios se devuelven ordenados por número.

Para medir el filtro de visibilidad, ejecuta en una base de pruebas
`db.contents.find({status: "published", type: "movie", ageRating: {$gte: 0, $lte: 12}}).sort({title: 1, _id: 1}).limit(20).explain("executionStats")`.
Los índices de J1 pueden acotar estado, clasificación, tipo y año según
el plan elegido. El regex literal sin ancla inspecciona candidatos,
el índice text de la demo trabaja por términos y el sort puede ser
explícito. Género y los dos campos de idioma son arreglos independientes,
por lo que la consulta deja su evaluación al motor sin un único índice
compuesto que abarque los tres.

La vista Thymeleaf está en `/profiles/{profileId}/catalog` y requiere
la sesión de un titular. El detalle enlaza al reproductor existente
y actualiza Mi lista con sus endpoints y el token CSRF de la sesión.
