(function () {
  const name = process.env.CINE_MONGO_TARGET_DB;
  const uri = db.getMongo().getURI();
  if (!name || db.getName() !== name || !/^cineconecta(?:_test_[a-z0-9_]+)?$/.test(name)
      || !/^mongodb:\/\/(?:[^/@]+@)?(?:127\.0\.0\.1|localhost):\d+\//.test(uri)) {
    throw Error("La carga requiere una conexión local y la BD explícita en CINE_MONGO_TARGET_DB.");
  }
  for (const coll of ["my_list", "viewing_sessions", "viewing_progress"]) {
    if (!db.getCollectionInfos({ name: coll, "options.validationLevel": "strict" }).length) {
      throw Error("Primero ejecuta 01_playback_activity.js.");
    }
  }
  if (db.contents.countDocuments({ _id: { $in: ["CNT-001", "CNT-002", "CNT-003"] } }) < 3) {
    throw Error("Primero ejecuta 02_catalog_admin.js y 03_seed_catalog_dev.js.");
  }

  const fs = require("fs");
  const leer = (archivo) => EJSON.parse(
    fs.readFileSync("database/mongodb/demo/json/" + archivo + ".json", "utf8"), { relaxed: false });

  function cargar(coll, docs, claveNatural) {
    let nuevos = 0;
    for (const doc of docs) {
      const { _id, ...campos } = doc;
      const filtro = claveNatural ? Object.fromEntries(claveNatural.map((k) => [k, doc[k]])) : { _id };
      const insertar = claveNatural ? { _id, ...campos } : campos;
      for (const k of claveNatural || []) delete insertar[k];
      nuevos += db[coll].updateOne(filtro, { $setOnInsert: insertar }, { upsert: true }).upsertedCount;
    }
    print(coll + ": " + nuevos + " nuevos de " + docs.length);
  }

  cargar("viewing_sessions", leer("viewing_sessions"), null);
  cargar("viewing_progress", leer("viewing_progress"), ["profileId", "unitKey"]);
  cargar("my_list", leer("my_list"), ["profileId", "contentId"]);

  const porPerfil = db.viewing_sessions.aggregate([
    { $group: { _id: "$profileId", sesiones: { $sum: 1 }, tipos: { $addToSet: "$contentTypeSnapshot" } } },
    { $sort: { _id: 1 } }
  ]).toArray();
  porPerfil.forEach((p) => print("perfil " + p._id + ": " + p.sesiones + " sesiones, tipos " + p.tipos.sort().join("/")));
}());
