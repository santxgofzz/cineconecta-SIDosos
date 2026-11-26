(function () {
  const target = db.getSiblingDB("cineconecta");
  const pid = NumberLong(String(typeof profileId !== "undefined" ? profileId : 1));
  let fallos = 0;
  let advertencias = 0;

  const ok = (msg) => print("  OK    " + msg);
  const falla = (msg) => { fallos++; print("  FALLA " + msg); };
  const aviso = (msg) => { advertencias++; print("  AVISO " + msg); };

  const esperado = {
    my_list: { uq_my_list_profile_content: true },
    viewing_sessions: { ix_sessions_period_sub: false, ix_sessions_profile_history: false },
    viewing_progress: { uq_progress_profile_unit: true, ix_progress_continue: false }
  };

  print("\n[1-2] Colecciones, validadores e índices");
  const existentes = new Set(target.getCollectionNames());
  for (const [coll, indices] of Object.entries(esperado)) {
    if (!existentes.has(coll)) {
      falla(coll + ": no existe (ejecutar 01_playback_activity.js)");
      continue;
    }
    const opts = target.getCollectionInfos({ name: coll })[0].options || {};
    if (opts.validator && opts.validator.$jsonSchema
        && opts.validationLevel === "strict" && opts.validationAction === "error") {
      ok(coll + ": $jsonSchema estricto");
    } else {
      falla(coll + ": validador ausente o no estricto (" + tojson({
        validator: !!opts.validator, level: opts.validationLevel, action: opts.validationAction }) + ")");
    }
    const actuales = Object.fromEntries(target[coll].getIndexes().map((ix) => [ix.name, ix]));
    for (const [nombre, unico] of Object.entries(indices)) {
      const ix = actuales[nombre];
      if (!ix) falla(coll + "." + nombre + ": no existe");
      else if (unico && !ix.unique) falla(coll + "." + nombre + ": debería ser único");
      else ok(coll + "." + nombre + (unico ? " (único)" : ""));
    }
  }

  print("\n[3] Planes de consulta (profileId = " + pid + ")");

  function etapas(plan, out) {
    if (!plan || typeof plan !== "object") return out;
    if (plan.stage) out.push({ stage: plan.stage, indexName: plan.indexName });
    if (plan.queryPlan) etapas(plan.queryPlan, out);
    if (plan.inputStage) etapas(plan.inputStage, out);
    (plan.inputStages || []).forEach((p) => etapas(p, out));
    return out;
  }

  function revisarPlan(descripcion, coll, cursor, indice, sinSort) {
    if (!existentes.has(coll)) return;
    const plan = etapas(cursor.explain("queryPlanner").queryPlanner.winningPlan, []);
    const resumen = plan.map((e) => e.stage + (e.indexName ? "(" + e.indexName + ")" : "")).join(" <- ");
    const usaIndice = plan.some((e) => e.stage === "IXSCAN" && e.indexName === indice);
    const collscan = plan.some((e) => e.stage === "COLLSCAN");
    const sortMemoria = plan.some((e) => e.stage === "SORT");
    if (usaIndice && !collscan && !(sinSort && sortMemoria)) ok(descripcion + ": " + resumen);
    else falla(descripcion + ": se esperaba IXSCAN(" + indice + ")" + (sinSort ? " sin SORT" : "")
      + ", plan = " + resumen);
  }

  const ahora = new Date();
  const haceUnMes = new Date(ahora.getTime() - 30 * 24 * 3600 * 1000);

  revisarPlan("Mi lista del perfil", "my_list",
    target.my_list.find({ profileId: pid }).sort({ addedAt: -1 }),
    "uq_my_list_profile_content", false);
  revisarPlan("Mi lista: ¿ya está el contenido?", "my_list",
    target.my_list.find({ profileId: pid, contentId: "verificacion" }),
    "uq_my_list_profile_content", false);
  revisarPlan("Continuar viendo", "viewing_progress",
    target.viewing_progress.find({ profileId: pid, completed: false }).sort({ lastActivityAt: -1 }).limit(12),
    "ix_progress_continue", true);
  revisarPlan("Progreso de una unidad (reanudar)", "viewing_progress",
    target.viewing_progress.find({ profileId: pid, unitKey: "verificacion" }),
    "uq_progress_profile_unit", false);
  revisarPlan("Historial de sesiones", "viewing_sessions",
    target.viewing_sessions.find({ profileId: pid }).sort({ startedAt: -1 }).limit(20),
    "ix_sessions_profile_history", true);
  revisarPlan("Sesiones abiertas de una unidad", "viewing_sessions",
    target.viewing_sessions.find({ profileId: pid, unitKey: "verificacion", endedAt: null }),
    "ix_sessions_profile_history", false);
  revisarPlan("Sesiones por periodo (informe de consumo)", "viewing_sessions",
    target.viewing_sessions.find({ startedAt: { $gte: haceUnMes, $lt: ahora } }),
    "ix_sessions_period_sub", false);

  print("\n[4] Consistencia de datos");

  function contar(coll, pipeline) {
    if (!existentes.has(coll)) return 0;
    const r = target[coll].aggregate(pipeline.concat([{ $count: "n" }])).toArray();
    return r.length ? r[0].n : 0;
  }
  function revisarDatos(descripcion, n) {
    if (n === 0) ok(descripcion + ": 0");
    else aviso(descripcion + ": " + n);
  }

  revisarDatos("Progreso pendiente con posición >= 90 % (debería estar completado)",
    contar("viewing_progress", [{ $match: { completed: false,
      $expr: { $gte: [{ $multiply: ["$lastPositionSeconds", 10] }, { $multiply: ["$durationSeconds", 9] }] } } }]));
  revisarDatos("Progreso con posición mayor que la duración",
    contar("viewing_progress", [{ $match: { $expr: { $gt: ["$lastPositionSeconds", "$durationSeconds"] } } }]));
  revisarDatos("Progreso cuya lastSessionId no existe en viewing_sessions",
    contar("viewing_progress", [
      { $lookup: { from: "viewing_sessions", localField: "lastSessionId", foreignField: "_id", as: "s" } },
      { $match: { s: { $size: 0 } } }]));
  revisarDatos("Sesiones cerradas sin endReason",
    contar("viewing_sessions", [{ $match: { endedAt: { $type: "date" }, endReason: null } }]));
  revisarDatos("Sesiones con endReason pero sin endedAt",
    contar("viewing_sessions", [{ $match: { endReason: { $type: "string" }, endedAt: null } }]));
  print("  INFO  Sesiones abiertas (sin endedAt): "
    + contar("viewing_sessions", [{ $match: { endedAt: null } }]));

  print("\nRESULTADO: " + (fallos === 0 ? "OK" : fallos + " fallo(s)") + ", " + advertencias + " aviso(s)");
  if (fallos > 0) quit(1);
}());
