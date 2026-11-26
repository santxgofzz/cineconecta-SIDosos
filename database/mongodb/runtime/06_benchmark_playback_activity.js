(function () {
  const dbName = typeof benchmarkDb !== "undefined" ? benchmarkDb : "cineconecta_benchmark";
  if (dbName === "cineconecta") throw Error("El benchmark no se ejecuta sobre la base de la aplicación");
  const bench = db.getSiblingDB(dbName);
  const perfiles = typeof profiles !== "undefined" ? profiles : 2000;
  const sesionesPorPerfil = typeof sessionsPerProfile !== "undefined" ? sessionsPerProfile : 100;
  const unidadesPorPerfil = typeof unitsPerProfile !== "undefined" ? unitsPerProfile : 40;
  const repeticiones = 15;
  const perfilConsultado = NumberLong(String(Math.floor(perfiles / 2)));

  bench.dropDatabase();
  const inicio = new Date("2026-01-01T00:00:00Z").getTime();
  const dia = 24 * 3600 * 1000;

  let lote = [];
  for (let p = 1; p <= perfiles; p++) {
    for (let s = 0; s < sesionesPorPerfil; s++) {
      const startedAt = new Date(inicio + Math.floor(Math.random() * 330 * dia));
      lote.push({
        _id: "S-" + p + "-" + s,
        profileId: NumberLong(String(p)),
        subscriptionId: NumberLong(String(1 + (p % 500))),
        contentId: "CNT-" + String(1 + (s % 50)).padStart(3, "0"),
        unitKey: "U-" + (s % unidadesPorPerfil),
        contentTypeSnapshot: "movie",
        startedAt: startedAt,
        endedAt: new Date(startedAt.getTime() + 1800 * 1000),
        watchedSeconds: NumberInt(1800),
        lastPositionSeconds: NumberInt(1800),
        endReason: "paused"
      });
      if (lote.length === 5000) { bench.viewing_sessions.insertMany(lote, { ordered: false }); lote = []; }
    }
  }
  if (lote.length) bench.viewing_sessions.insertMany(lote, { ordered: false });

  lote = [];
  for (let p = 1; p <= perfiles; p++) {
    for (let u = 0; u < unidadesPorPerfil; u++) {
      lote.push({
        profileId: NumberLong(String(p)),
        contentId: "CNT-" + String(1 + (u % 50)).padStart(3, "0"),
        unitKey: "U-" + u,
        lastPositionSeconds: NumberInt(600),
        durationSeconds: NumberInt(3600),
        completed: u % 3 === 0,
        lastActivityAt: new Date(inicio + Math.floor(Math.random() * 330 * dia)),
        lastSessionId: "S-" + p + "-" + u
      });
      if (lote.length === 5000) { bench.viewing_progress.insertMany(lote, { ordered: false }); lote = []; }
    }
  }
  if (lote.length) bench.viewing_progress.insertMany(lote, { ordered: false });

  const pipelineConsumo = [
    { $match: { startedAt: { $gte: new Date("2026-11-01T00:00:00Z"), $lt: new Date("2026-12-01T00:00:00Z") } } },
    { $group: {
      _id: { subscriptionId: "$subscriptionId", contentType: "$contentTypeSnapshot" },
      sessionCount: { $sum: 1 },
      watchedSeconds: { $sum: "$watchedSeconds" },
      profileIds: { $addToSet: "$profileId" }
    } }
  ];

  const consultas = [
    {
      nombre: "Continuar viendo",
      coll: "viewing_progress",
      indice: { key: { profileId: 1, completed: 1, lastActivityAt: -1 }, name: "ix_progress_continue" },
      ejecutar: (c) => c.find({ profileId: perfilConsultado, completed: false }).sort({ lastActivityAt: -1 }).limit(12)
    },
    {
      nombre: "Historial de sesiones",
      coll: "viewing_sessions",
      indice: { key: { profileId: 1, startedAt: -1 }, name: "ix_sessions_profile_history" },
      ejecutar: (c) => c.find({ profileId: perfilConsultado }).sort({ startedAt: -1 }).limit(20)
    },
    {
      nombre: "Informe de consumo por periodo",
      coll: "viewing_sessions",
      indice: { key: { startedAt: 1, subscriptionId: 1 }, name: "ix_sessions_period_sub" },
      explicar: (c) => c.explain("executionStats").aggregate(pipelineConsumo),
      ejecutar: (c) => c.aggregate(pipelineConsumo)
    }
  ];

  function etapas(plan, out) {
    if (!plan || typeof plan !== "object") return out;
    if (plan.stage) out.push(plan.stage + (plan.indexName ? "(" + plan.indexName + ")" : ""));
    if (plan.queryPlan) etapas(plan.queryPlan, out);
    if (plan.inputStage) etapas(plan.inputStage, out);
    (plan.inputStages || []).forEach((p) => etapas(p, out));
    return out;
  }

  function buscar(obj, clave) {
    if (!obj || typeof obj !== "object") return null;
    if (obj[clave]) return obj[clave];
    for (const v of Object.values(obj)) {
      const r = buscar(v, clave);
      if (r) return r;
    }
    return null;
  }

  function medir(consulta) {
    const coll = bench[consulta.coll];
    const stats = consulta.explicar ? consulta.explicar(coll) : consulta.ejecutar(coll).explain("executionStats");
    const tiempos = [];
    let devueltos = 0;
    for (let i = 0; i < repeticiones; i++) {
      const t0 = Date.now();
      devueltos = consulta.ejecutar(coll).toArray().length;
      tiempos.push(Date.now() - t0);
    }
    tiempos.sort((a, b) => a - b);
    const ejecucion = buscar(stats, "executionStats");
    return {
      plan: etapas(buscar(stats, "queryPlanner").winningPlan, []).join(" <- "),
      docsExaminados: ejecucion.totalDocsExamined,
      clavesExaminadas: ejecucion.totalKeysExamined,
      devueltos: devueltos,
      medianaMs: tiempos[Math.floor(tiempos.length / 2)]
    };
  }

  print("Base: " + dbName);
  print("Volumen: viewing_sessions = " + bench.viewing_sessions.countDocuments()
    + ", viewing_progress = " + bench.viewing_progress.countDocuments()
    + ", perfiles = " + perfiles + ", perfil consultado = " + perfilConsultado);
  print("Tiempo = mediana de " + repeticiones + " ejecuciones\n");

  const filas = [];
  for (const consulta of consultas) {
    const antes = medir(consulta);
    bench[consulta.coll].createIndex(consulta.indice.key, { name: consulta.indice.name });
    const despues = medir(consulta);
    filas.push({ consulta: consulta.nombre, antes: antes, despues: despues });
    print("== " + consulta.nombre);
    print("   ANTES   plan: " + antes.plan);
    print("           docs examinados: " + antes.docsExaminados + ", claves: " + antes.clavesExaminadas
      + ", devueltos: " + antes.devueltos + ", mediana: " + antes.medianaMs + " ms");
    print("   DESPUÉS plan: " + despues.plan);
    print("           docs examinados: " + despues.docsExaminados + ", claves: " + despues.clavesExaminadas
      + ", devueltos: " + despues.devueltos + ", mediana: " + despues.medianaMs + " ms\n");
  }

  print("| Consulta | Antes: plan | Antes: docs | Antes: ms | Después: plan | Después: docs | Después: ms |");
  print("|---|---|---|---|---|---|---|");
  filas.forEach((f) => print("| " + f.consulta + " | " + f.antes.plan + " | " + f.antes.docsExaminados + " | "
    + f.antes.medianaMs + " | " + f.despues.plan + " | " + f.despues.docsExaminados + " | " + f.despues.medianaMs + " |"));

  if (typeof keep === "undefined" || !keep) bench.dropDatabase();
}());
