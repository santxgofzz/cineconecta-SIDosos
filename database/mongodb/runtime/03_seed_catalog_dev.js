// Catálogo sintético de desarrollo. Ejecutar desde la raíz del repositorio.
(function () {
  const name = process.env.CINE_MONGO_TARGET_DB;
  const uri = db.getMongo().getURI();
  if (!name || db.getName() !== name || !/^cineconecta(?:_test_[a-z0-9_]+)?$/.test(name)
      || !/^mongodb:\/\/(?:[^/@]+@)?(?:127\.0\.0\.1|localhost):\d+\//.test(uri)) {
    throw Error("La carga requiere una conexión local y la BD explícita en CINE_MONGO_TARGET_DB.");
  }
  if (!db.getCollectionInfos({ name: "contents", "options.validationLevel": "strict" }).length) {
    throw Error("Primero ejecuta 02_catalog_admin.js.");
  }

  // Mismos títulos e IDs que los datos sintéticos referenciados por playback.
  const fs = require("fs");
  const contents = EJSON.parse(fs.readFileSync("database/mongodb/demo/json/contents.json", "utf8"),
    { relaxed: false });
  let inserted = 0;
  for (const content of contents) {
    const { _id, ...fields } = content;
    inserted += db.contents.updateOne({ _id }, { $setOnInsert: fields }, { upsert: true }).upsertedCount;
  }
  print("Títulos nuevos: " + inserted);
}());
