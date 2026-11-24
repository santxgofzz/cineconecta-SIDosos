// Use mongosh with an explicit database in the URI and CINE_MONGO_TARGET_DB set.
(function () {
  const name = process.env.CINE_MONGO_TARGET_DB;
  if (!name || db.getName() !== name || !/^cineconecta(?:_test_[a-z0-9_]+)?$/.test(name)) {
    throw Error("Selecciona cineconecta o cineconecta_test_* en la URI y CINE_MONGO_TARGET_DB.");
  }

  const text = { bsonType: "string", minLength: 1 };
  const string = { bsonType: "string" };
  const positive = { bsonType: "int", minimum: 1 };
  const date = { bsonType: "date" };
  const array = (items, minItems = 0) => ({ bsonType: "array", items, minItems });
  const object = (required, properties) => ({ bsonType: "object", required, properties });
  const variant = (type, required, forbidden) => ({
    required,
    properties: { type: { enum: [type] } },
    not: { anyOf: forbidden.map(field => ({ required: [field] })) }
  });

  const castMember = object(["actorName"], { actorName: text, characterName: string });
  const episode = object(["id", "number", "title", "synopsis", "durationSeconds"], {
    id: text, number: positive, title: text, synopsis: string, durationSeconds: positive
  });
  const season = object(["number", "description", "episodes"], {
    number: positive, description: string, episodes: array(episode, 1)
  });
  const source = object(["name"], { name: text, url: string });

  const contents = object([
    "_id", "type", "title", "synopsis", "releaseYear", "genres", "countries",
    "audioLanguages", "subtitleLanguages", "ageRating", "coverUrl", "status",
    "createdAt", "updatedAt"
  ], {
    _id: text, type: { enum: ["movie", "series", "documentary"] },
    title: text, synopsis: string, releaseYear: { bsonType: "int", minimum: 1888, maximum: 2100 },
    genres: array(text, 1), countries: array(text, 1), audioLanguages: array(text, 1),
    subtitleLanguages: array(text), ageRating: { bsonType: "int", minimum: 0, maximum: 18 },
    coverUrl: text, status: { enum: ["draft", "published", "retired"] },
    createdAt: date, updatedAt: date, durationSeconds: positive, director: text,
    cast: array(castMember), seasons: array(season, 1), topics: array(text, 1),
    interviewees: array(text), sources: array(source)
  });
  contents.oneOf = [
    variant("movie", ["durationSeconds", "director", "cast"], ["seasons", "topics", "interviewees", "sources"]),
    variant("series", ["seasons"], ["durationSeconds", "director", "cast", "topics", "interviewees", "sources"]),
    variant("documentary", ["durationSeconds", "director", "topics"], ["cast", "seasons"])
  ];

  const admins = object(["_id", "usernameNormalized", "passwordHash", "role", "active", "createdAt"], {
    _id: { bsonType: "objectId" },
    usernameNormalized: { bsonType: "string", minLength: 1, maxLength: 64, pattern: "^[a-z0-9][a-z0-9._-]*$" },
    passwordHash: {
      bsonType: "string",
      pattern: "^\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}$"
    },
    role: { enum: ["ADMIN"] }, active: { bsonType: "bool" }, createdAt: date
  });

  const schemas = { contents, admin_users: admins };
  const existing = db.getCollectionNames();
  for (const [collection, schema] of Object.entries(schemas)) {
    if (existing.includes(collection) && db.getCollection(collection).findOne(
      { $nor: [{ $jsonSchema: schema }] }, { _id: 1 }
    )) {
      throw Error(collection + " contiene documentos incompatibles; no se modificaron validadores.");
    }
  }
  if (existing.includes("admin_users") && db.admin_users.aggregate([
    { $group: { _id: "$usernameNormalized", count: { $sum: 1 } } },
    { $match: { count: { $gt: 1 } } },
    { $limit: 1 }
  ]).hasNext()) {
    throw Error("admin_users contiene usuarios duplicados; no se modificaron validadores.");
  }

  for (const [collection, schema] of Object.entries(schemas)) {
    const options = { validator: { $jsonSchema: schema }, validationLevel: "strict", validationAction: "error" };
    const result = existing.includes(collection)
      ? db.runCommand({ collMod: collection, ...options })
      : db.createCollection(collection, options);
    if (result.ok !== 1) throw Error("No se pudo configurar la colección " + collection);
    print("Validador listo: " + collection);
  }

  db.admin_users.createIndex({ usernameNormalized: 1 }, { name: "uq_admin_username", unique: true });
  db.contents.createIndex({ status: 1, ageRating: 1, _id: 1 }, { name: "ix_contents_visibility" });
  db.contents.createIndex({ status: 1, type: 1, releaseYear: -1, ageRating: 1 },
    { name: "ix_contents_filters" });
}());
