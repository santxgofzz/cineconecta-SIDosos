# Catálogo y administración en MongoDB

Este anexo describe el catálogo implementado. Complementa el modelo documental
entregado, el [enunciado](../enunciado/EnunciadoPlataformaCineConecta.pdf) y
la [rúbrica](../rubrica/proyecto-Eval3.pdf).

## Responsabilidades de los dos motores

Oracle conserva cuentas, perfiles, planes, suscripciones y pagos. La compra
requiere integridad relacional y una transacción que abarque suscripción y
pago. MongoDB conserva el catálogo de campos variables, la actividad de
reproducción y las cuentas administrativas independientes. Embeber temporadas
y episodios permite leer el detalle de una serie en una consulta y validar su
estructura al actualizar el documento. La serie puede crecer hasta el límite
de 16 MiB por documento BSON. Si el volumen exige otro diseño, se deberá
planificar una migración que conserve los identificadores.

| Campo de referencia | Origen | Uso |
| --- | --- | --- |
| `contentId` | `contents._id`, string | Vincula Mi lista, sesiones y progreso a un título. |
| `episodeId` | `contents.seasons[].episodes[].id`, string | Vincula sesión y progreso a un episodio. Para películas y documentales la unidad es el propio título. |
| `profileId` | `PROFILES.PROFILE_ID` en Oracle, BSON long en MongoDB | Identifica el titular de la actividad a través de su perfil. La aplicación verifica pertenencia y actividad usando Oracle. |
| `subscriptionId` | `SUBSCRIPTIONS.SUBSCRIPTION_ID` en Oracle, BSON long en MongoDB | Registra la suscripción al inicio de la sesión para atribuir el plan histórico. |
| `lastSessionId` | `viewing_sessions._id`, string | Relaciona un progreso con la sesión de la que procede. |

Son referencias lógicas. Cada motor confirma sus operaciones de forma
independiente y las comprobaciones entre motores se hacen en la aplicación.
La actividad registra el tipo de contenido al iniciar una sesión para el
informe histórico. Ese valor describe el momento de inicio y permanece
estable aunque cambie el catálogo. La edad máxima y la vigencia se consultan
en Oracle para cada solicitud de catálogo. Si falla alguno de los motores,
el servicio responde con un error de disponibilidad y deniega el acceso.

## Diccionario BSON

### `contents`

| Campos | BSON | Condición |
| --- | --- | --- |
| `_id` | string | Obligatorio y estable. La API genera `CNT-<UUID>`. |
| `type` | string | Obligatorio. `movie`, `series` o `documentary`. |
| `title`, `synopsis`, `coverUrl` | string | Obligatorios. Título y portada no vacíos. La API exige URL HTTP(S). |
| `releaseYear`, `ageRating` | int | Obligatorios, 1888–2100 y 0–18 respectivamente. |
| `genres`, `countries`, `audioLanguages`, `subtitleLanguages` | array de string | Obligatorios. Subtítulos admite lista vacía. |
| `status` | string | Obligatorio. `draft`, `published` o `retired`. |
| `createdAt`, `updatedAt` | date | Obligatorios, en UTC. La API fija ambos al crear y avanza `updatedAt` en cada cambio. |
| `durationSeconds`, `director` | int positivo, string | Obligatorios en película y documental. |
| `cast` | array de objetos | Obligatorio en película, puede estar vacío. Cada elemento exige `actorName`, con `characterName` opcional. |
| `seasons` | array de objetos | Obligatorio y no vacío en serie. Cada temporada exige `number`, `description` y `episodes` no vacío. |
| `seasons[].episodes` | array de objetos | Cada episodio exige `id`, `number`, `title`, `synopsis` y `durationSeconds`. Números y duración son int positivos. El ID se guarda como string en BSON `id`. |
| `topics`, `interviewees`, `sources` | arrays | Documental exige temas no vacíos. Entrevistados son strings y las fuentes son objetos con `name` y `url` opcional. |

### `admin_users`

| Campo | BSON | Condición |
| --- | --- | --- |
| `_id` | ObjectId | Identificador obligatorio. |
| `usernameNormalized` | string | Obligatorio y único. Se quitan espacios exteriores y se normaliza a minúsculas. |
| `passwordHash` | string | Forma BCrypt válida. El bootstrap usa el codificador de Spring Security. |
| `role` | string | `ADMIN`. |
| `active` | bool | Debe ser verdadero para autenticar al administrador. |
| `createdAt` | date | Fecha de creación obligatoria en UTC. |

## Validación, historial e índices

La migración instala `$jsonSchema` con validación `strict/error`. Las ramas
del esquema requieren los campos de la variante y excluyen campos que
pertenecen a otras variantes. Se admiten extensiones opcionales. Frente al
modelo entregado, el runtime fija el límite superior del año en 2100, exige
series con al menos una temporada y episodio, y valida la forma de BCrypt.
Los marcadores de hash de la demo no se cargan. El mapeo conserva
`contents._id` como string incluso si tiene 24 caracteres hexadecimales.

El servicio exige unicidad de números de temporada en cada serie y de
episodio dentro de su temporada. Los IDs de episodio existentes deben
mantener su posición, incluidos los de borrador. El servidor genera IDs
para los episodios nuevos. El índice único multikey no garantiza unicidad
entre elementos del mismo arreglo embebido. El retiro cambia el estado y
conserva el título, la lista, las sesiones y el progreso. Se puede republicar
con el mismo ID. Para editar, `If-Match` y una comparación condicional de
`updatedAt` impiden grabar cambios de una revisión desactualizada.

| Índice runtime | Consulta o regla |
| --- | --- |
| `contents._id_` | Detalle por ID y referencias desde actividad. |
| `ix_contents_visibility` en `status, ageRating, _id` | Acota títulos por estado y clasificación. El orden por título puede requerir un sort. |
| `ix_contents_filters` en `status, type, releaseYear desc, ageRating` | Prefijo de estado y tipo con filtro de año. Conserva campos escalares. |
| `uq_admin_username` | Igualdad y unicidad del usuario normalizado. |

La búsqueda de título usa coincidencia parcial literal que ignora
mayúsculas. El índice text de la demo fue diseñado para buscar términos,
por lo que el runtime evita crearlo para esta consulta. Se filtra por
igualdad de género y de idioma. El idioma puede aparecer en audio o
subtítulos. La combinación de varios arreglos independientes impediría
crear un solo índice compuesto multikey para esos filtros. El listado
ordena por título e ID y cuenta con los mismos criterios.

## Ejemplos

Documentos de catálogo independientes que cumplen el esquema, en sintaxis
mongosh. Las [semillas completas](../../database/mongodb/demo/json/contents.json)
usan sus propios IDs.

```javascript
const common = {
  synopsis: "Descripción de prueba", releaseYear: NumberInt(2025),
  genres: ["Drama"], countries: ["CO"], audioLanguages: ["es"], subtitleLanguages: [],
  ageRating: NumberInt(12), coverUrl: "https://example.org/cover.jpg", status: "draft",
  createdAt: ISODate("2026-01-01"), updatedAt: ISODate("2026-01-01")
};
const movie = {
  ...common, _id: "DOC-MOVIE", type: "movie", title: "Camino",
  director: "Dirección ficticia", durationSeconds: NumberInt(5400),
  cast: [{actorName: "Intérprete ficticio", characterName: "Ana"}]
};
const series = {
  ...common, _id: "DOC-SERIES", type: "series", title: "Temporadas",
  seasons: [{number: NumberInt(1), description: "Primera", episodes: [
    {id: "DOC-EP-01", number: NumberInt(1), title: "Inicio",
     synopsis: "Capítulo inicial", durationSeconds: NumberInt(1500)}
  ]}]
};
const documentary = {
  ...common, _id: "DOC-DOCUMENTARY", type: "documentary", title: "Memoria",
  director: "Dirección ficticia", durationSeconds: NumberInt(2700),
  topics: ["Historia"], sources: [{name: "Archivo de prueba"}]
};
```

Ejemplo administrativo redactado, para lectura. Obtén cuentas reales de
desarrollo mediante el [bootstrap](../../database/mongodb/runtime/README.md).

```javascript
{
  _id: ObjectId("000000000000000000000001"),
  usernameNormalized: "admin.documentacion",
  passwordHash: "<redactado>", role: "ADMIN", active: false,
  createdAt: ISODate("2026-01-01")
}
```

```javascript
// Temporadas y episodios de un solo título en una lectura.
db.contents.findOne({_id: "CNT-002", type: "series"}, {title: 1, seasons: 1});
// El historial conserva el contentId y, para series, episodeId.
db.viewing_sessions.find({contentId: "CNT-002"}, {profileId: 1, episodeId: 1, subscriptionId: 1});
// Elegir una base de medición propia y registrar tamaño, plan y contadores.
db.contents.find({status: "published", type: "movie", ageRating: {$gte: 0, $lte: 12}})
  .sort({title: 1, _id: 1}).limit(20).explain("executionStats");
```

Para una comparación, ejecuta la misma consulta sobre el conjunto sintético
de medición antes y después de crear el índice de catálogo. Registra por
separado el volumen, el plan, `totalKeysExamined`, `totalDocsExamined` y la
latencia, usando las mismas condiciones. Evita quitar índices de una BD
compartida. La comparación formal de ambos motores corresponde al módulo
de informes y rendimiento.

## Operación y sustentación

- [Preparación y prueba de MongoDB](../../database/mongodb/runtime/README.md).
- [Rutas y permisos de administración](../../src/main/java/co/edu/icesi/cineconecta/admin/README.md).
- [Rutas y filtros del catálogo](../../src/main/java/co/edu/icesi/cineconecta/catalog/README.md).
- [Trazabilidad y comandos de prueba](../pruebas/catalogo-mongodb.md).

Preguntas para la sustentación de Johan

1. ¿Qué campos comparte el esquema polimórfico y qué exige para cada variante?
2. ¿Qué lectura aprovecha el embebido de temporadas y qué sucede al acercarse a 16 MiB?
3. ¿Qué valida el esquema, qué valida el servicio y qué garantiza un índice único?
4. ¿Cómo conserva el editor los IDs de episodios cuando dos administradores editan a la vez?
5. ¿Cómo funciona la autenticación BCrypt de administración y cómo se aísla de Oracle?
6. ¿De dónde sale el límite de edad y qué responde el catálogo cuando falla Oracle?
7. ¿Qué garantiza una referencia lógica entre bases y cómo se demuestra que retirar preserva el historial?
