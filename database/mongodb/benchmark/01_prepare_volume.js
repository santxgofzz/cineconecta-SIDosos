// Datos aislados para comparar explain antes/después; nunca usa cineconecta.
(function () {
  const databaseName = "cineconecta_benchmark";
  const collectionName = "benchmark_viewing_sessions";
  if (db.getName() !== databaseName) {
    throw Error(`Conecta a ${databaseName}; base actual: ${db.getName()}`);
  }

  const target = db.getSiblingDB(databaseName);
  if (target.getCollectionNames().includes(collectionName)) {
    target.getCollection(collectionName).drop();
  }
  target.createCollection(collectionName);
  const sessions = target.getCollection(collectionName);
  const total = 500000;
  const batchSize = 10000;
  const firstDay = Date.UTC(2023, 0, 1);
  const dayMs = 24 * 60 * 60 * 1000;
  let batch = [];

  for (let i = 0; i < total; i++) {
    const dayOffset = Math.floor(i / 343);
    const minuteOffset = (i % 343) * 4;
    const type = ["movie", "series", "documentary"][i % 3];
    const sessionId = String(i + 1).padStart(7, "0");
    const contentId = `BENCH-${String((i % 8000) + 1).padStart(5, "0")}`;
    const unitKey = type === "series" ? `${contentId}-EP-${(i % 12) + 1}` : contentId;
    const document = {
      _id: `BSES-${sessionId}`,
      profileId: NumberLong(String((i * 29) % 50000 + 1)),
      subscriptionId: NumberLong(String((i * 73) % 250000 + 1)),
      contentId,
      unitKey,
      contentTypeSnapshot: type,
      startedAt: new Date(firstDay + dayOffset * dayMs + minuteOffset * 60 * 1000),
      watchedSeconds: NumberInt(60 + (i * 97) % 3600),
      lastPositionSeconds: NumberInt(60 + (i * 97) % 3600)
    };
    if (type === "series") {
      document.episodeId = unitKey;
    }
    batch.push(document);
    if (batch.length === batchSize) {
      sessions.insertMany(batch, { ordered: false });
      batch = [];
    }
  }
  if (batch.length) {
    sessions.insertMany(batch, { ordered: false });
  }

  printjson({
    database: databaseName,
    mongoVersion: db.version(),
    collection: collectionName,
    documents: sessions.countDocuments({}),
    secondaryIndexes: sessions.getIndexes().length - 1,
    minimumDate: sessions.find({}, { startedAt: 1 }).sort({ startedAt: 1 }).limit(1).toArray()[0].startedAt,
    maximumDate: sessions.find({}, { startedAt: 1 }).sort({ startedAt: -1 }).limit(1).toArray()[0].startedAt
  });
}());
