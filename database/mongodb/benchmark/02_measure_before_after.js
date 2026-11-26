// Ejecutar después de 01_prepare_volume.js en cineconecta_benchmark.
(function () {
  const databaseName = "cineconecta_benchmark";
  const collectionName = "benchmark_viewing_sessions";
  const indexName = "ix_benchmark_period_subscription_type";
  if (db.getName() !== databaseName) {
    throw Error(`Conecta a ${databaseName}; base actual: ${db.getName()}`);
  }

  const sessions = db.getCollection(collectionName);
  if (sessions.countDocuments({}) !== 500000) {
    throw Error("Prepara primero las 500000 sesiones con 01_prepare_volume.js.");
  }
  if (sessions.getIndexes().some(index => index.name === indexName)) {
    sessions.dropIndex(indexName);
  }

  const pipeline = [
    { $match: { startedAt: {
      $gte: ISODate("2026-11-01T00:00:00Z"),
      $lt: ISODate("2026-12-01T00:00:00Z")
    } } },
    { $group: {
      _id: { subscriptionId: "$subscriptionId", contentType: "$contentTypeSnapshot" },
      sessions: { $sum: 1 },
      watchedSeconds: { $sum: "$watchedSeconds" },
      profileIds: { $addToSet: "$profileId" }
    } }
  ];

  function collectPlanDetails(value, stages, indexes) {
    if (Array.isArray(value)) {
      value.forEach(child => collectPlanDetails(child, stages, indexes));
      return;
    }
    if (value && typeof value === "object") {
      if (typeof value.stage === "string") stages.add(value.stage);
      if (typeof value.indexName === "string") indexes.add(value.indexName);
      Object.values(value).forEach(child => collectPlanDetails(child, stages, indexes));
    }
  }

  function median(values) {
    const sorted = [...values].sort((left, right) => left - right);
    return sorted[Math.floor(sorted.length / 2)];
  }

  function measure(phase) {
    const explain = sessions.explain("executionStats").aggregate(pipeline);
    const cursorStage = (explain.stages || []).find(stage => stage.$cursor)?.$cursor;
    const planRoot = cursorStage || explain;
    const stats = planRoot.executionStats || {};
    const winningPlan = planRoot.queryPlanner?.winningPlan || {};
    const stages = new Set();
    const indexes = new Set();
    collectPlanDetails(winningPlan, stages, indexes);

    const runTimesMs = [];
    let outputGroups = 0;
    for (let run = 0; run < 5; run++) {
      const start = Date.now();
      outputGroups = sessions.aggregate(pipeline).toArray().length;
      runTimesMs.push(Date.now() - start);
    }

    printjson({
      phase,
      mongoVersion: db.version(),
      queryWindowUtc: ["2026-11-01T00:00:00Z", "2026-12-01T00:00:00Z"],
      inputDocuments: stats.nReturned,
      documentsExamined: stats.totalDocsExamined,
      indexKeysExamined: stats.totalKeysExamined,
      explainExecutionMs: stats.executionTimeMillis,
      outputGroups,
      warmRunTimesMs: runTimesMs,
      warmRunMedianMs: median(runTimesMs),
      winningPlanStages: [...stages],
      indexNames: [...indexes],
      winningPlan,
      aggregationStages: (explain.stages || []).map(stage => Object.keys(stage)[0])
    });
  }

  print("ANTES: solo el índice automático _id_. Las cinco ejecuciones son lecturas calientes.");
  measure("before");

  sessions.createIndex(
    { startedAt: 1, subscriptionId: 1, contentTypeSnapshot: 1 },
    { name: indexName }
  );

  print("DESPUÉS: índice temporal y de agrupación. Misma consulta y periodo.");
  measure("after");
  printjson({ indexes: sessions.getIndexes() });
  const storage = db.runCommand({ collStats: collectionName, scale: 1 });
  printjson({
    collectionStats: {
      count: storage.count,
      documentBytes: storage.size,
      storageBytes: storage.storageSize,
      totalIndexBytes: storage.totalIndexSize,
      indexSizesBytes: storage.indexSizes
    }
  });
}());
