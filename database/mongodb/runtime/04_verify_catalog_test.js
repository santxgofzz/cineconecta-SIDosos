// Verificaciones con escrituras controladas en una base de prueba local.
(function () {
  const name = process.env.CINE_MONGO_TARGET_DB;
  if (!name || db.getName() !== name || !/^cineconecta_test_[a-z0-9_]+$/.test(name)
      || !/^mongodb:\/\/(?:[^/@]+@)?(?:127\.0\.0\.1|localhost):\d+\//.test(db.getMongo().getURI())) {
    throw Error("Usa una base local cineconecta_test_* seleccionada explícitamente.");
  }

  const contents = db.contents;
  const admins = db.admin_users;
  assert.strictEqual(contents.countDocuments({}), 8);
  assert.strictEqual(contents.distinct("type").sort().join(","), "documentary,movie,series");
  assert.strictEqual(contents.distinct("status").sort().join(","), "draft,published,retired");
  const movie = contents.findOne({ _id: "CNT-001" });
  const series = contents.findOne({ _id: "CNT-002" });
  assert.strictEqual(series.seasons.length, 2);
  assert.strictEqual(series.seasons[0].episodes[0].id, "EP-002-01");
  assert(Number(movie.durationSeconds) > 0);
  const invalidId = "TEST-INVALID-" + new ObjectId().toHexString();
  const { durationSeconds, ...missingDuration } = movie;
  const incompleteSeason = { ...series.seasons[0], episodes: [{ id: "EP-INCOMPLETE", number: NumberInt(1) }] };

  const id = new ObjectId();
  const admin = {
    _id: id, usernameNormalized: "check." + id.toHexString(),
    passwordHash: "$2a$10$" + "A".repeat(53), role: "ADMIN", active: false, createdAt: new Date()
  };
  admins.insertOne(admin);
  const invalidDocuments = [
    ["contents", { ...missingDuration, _id: invalidId }, 121],
    ["contents", { ...movie, _id: invalidId, status: "unknown" }, 121],
    ["contents", { ...movie, _id: invalidId, type: "unknown" }, 121],
    ["contents", { ...movie, _id: invalidId, seasons: series.seasons }, 121],
    ["contents", { ...series, _id: invalidId, seasons: [incompleteSeason] }, 121],
    ["admin_users", { ...admin, _id: new ObjectId() }, 11000],
    ["admin_users", { ...admin, _id: new ObjectId(), usernameNormalized: "Not.Normalized" }, 121]
  ];
  for (const [collection, document, code] of invalidDocuments) {
    const result = db.runCommand({ insert: collection, documents: [document] });
    assert.strictEqual(result.writeErrors?.[0]?.code, code);
  }
  assert.strictEqual(contents.countDocuments({}), 8);
  assert(admins.getIndexes().some(index => index.name === "uq_admin_username" && index.unique));
  print("Catálogo verificado: 8 títulos, tres tipos y estados, 7 escrituras inválidas rechazadas.");
}());
