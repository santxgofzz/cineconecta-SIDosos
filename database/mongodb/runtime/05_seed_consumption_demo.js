// Sesiones sinteticas para probar el informe de consumo en la BD local cineconecta.
// profileId y subscriptionId coinciden con database/oracle/03_seed.sql y con
// database/mongodb/demo/02_cargar_datos_sinteticos.js.
// Inserta por _id si no existe; no actualiza ni borra sesiones existentes.
(function () {
  const target = db.getSiblingDB("cineconecta");
  if (!target.getCollectionNames().includes("viewing_sessions")) {
    throw Error("Ejecuta primero 01_playback_activity.js para crear viewing_sessions.");
  }

  const sessions = [
    {
      _id: "SES-001", profileId: NumberLong("15"), subscriptionId: NumberLong("8"),
      contentId: "CNT-002", episodeId: "EP-002-01", unitKey: "EP-002-01",
      contentTypeSnapshot: "series", startedAt: ISODate("2026-11-02T15:00:00Z"),
      endedAt: ISODate("2026-11-02T15:20:00Z"), watchedSeconds: NumberInt(900),
      lastPositionSeconds: NumberInt(1200), endReason: "paused"
    },
    {
      _id: "SES-002", profileId: NumberLong("15"), subscriptionId: NumberLong("8"),
      contentId: "CNT-001", unitKey: "CNT-001", contentTypeSnapshot: "movie",
      startedAt: ISODate("2026-11-03T15:00:00Z"), endedAt: ISODate("2026-11-03T15:35:00Z"),
      watchedSeconds: NumberInt(1800), lastPositionSeconds: NumberInt(4000), endReason: "paused"
    },
    {
      _id: "SES-003", profileId: NumberLong("16"), subscriptionId: NumberLong("8"),
      contentId: "CNT-003", unitKey: "CNT-003", contentTypeSnapshot: "documentary",
      startedAt: ISODate("2026-11-04T11:00:00Z"), endedAt: ISODate("2026-11-04T11:50:00Z"),
      watchedSeconds: NumberInt(2800), lastPositionSeconds: NumberInt(3000), endReason: "finished"
    },
    {
      _id: "SES-004", profileId: NumberLong("15"), subscriptionId: NumberLong("8"),
      contentId: "CNT-005", unitKey: "CNT-005", contentTypeSnapshot: "movie",
      startedAt: ISODate("2026-11-04T17:00:00Z"), endedAt: ISODate("2026-11-04T17:25:00Z"),
      watchedSeconds: NumberInt(1200), lastPositionSeconds: NumberInt(1200), endReason: "paused"
    },
    {
      _id: "SES-005", profileId: NumberLong("21"), subscriptionId: NumberLong("9"),
      contentId: "CNT-004", unitKey: "CNT-004", contentTypeSnapshot: "movie",
      startedAt: ISODate("2026-11-05T19:00:00Z"), endedAt: ISODate("2026-11-05T21:00:00Z"),
      watchedSeconds: NumberInt(6400), lastPositionSeconds: NumberInt(6500), endReason: "finished"
    },
    {
      _id: "SES-006", profileId: NumberLong("15"), subscriptionId: NumberLong("10"),
      contentId: "CNT-002", episodeId: "EP-002-02", unitKey: "EP-002-02",
      contentTypeSnapshot: "series", startedAt: ISODate("2026-11-08T15:00:00Z"),
      endedAt: ISODate("2026-11-08T15:45:00Z"), watchedSeconds: NumberInt(2400),
      lastPositionSeconds: NumberInt(2450), endReason: "finished"
    },
    {
      _id: "SES-007", profileId: NumberLong("15"), subscriptionId: NumberLong("10"),
      contentId: "CNT-001", unitKey: "CNT-001", contentTypeSnapshot: "movie",
      startedAt: ISODate("2026-11-09T19:00:00Z"), endedAt: ISODate("2026-11-09T19:25:00Z"),
      watchedSeconds: NumberInt(1000), lastPositionSeconds: NumberInt(5000), endReason: "paused"
    },
    {
      _id: "SES-008", profileId: NumberLong("15"), subscriptionId: NumberLong("10"),
      contentId: "CNT-002", episodeId: "EP-002-03", unitKey: "EP-002-03",
      contentTypeSnapshot: "series", startedAt: ISODate("2026-11-10T14:00:00Z"),
      endedAt: ISODate("2026-11-10T14:20:00Z"), watchedSeconds: NumberInt(700),
      lastPositionSeconds: NumberInt(1000), endReason: "paused"
    },
    {
      _id: "SES-009", profileId: NumberLong("21"), subscriptionId: NumberLong("9"),
      contentId: "CNT-007", unitKey: "CNT-007", contentTypeSnapshot: "documentary",
      startedAt: ISODate("2026-11-11T10:00:00Z"), endedAt: ISODate("2026-11-11T10:15:00Z"),
      watchedSeconds: NumberInt(500), lastPositionSeconds: NumberInt(600), endReason: "paused"
    },
    {
      _id: "SES-010", profileId: NumberLong("21"), subscriptionId: NumberLong("9"),
      contentId: "CNT-008", episodeId: "EP-008-01", unitKey: "EP-008-01",
      contentTypeSnapshot: "series", startedAt: ISODate("2026-11-12T11:00:00Z"),
      endedAt: ISODate("2026-11-12T11:20:00Z"), watchedSeconds: NumberInt(850),
      lastPositionSeconds: NumberInt(1000), endReason: "paused"
    }
  ];

  let inserted = 0;
  let alreadyPresent = 0;
  for (const session of sessions) {
    const result = target.viewing_sessions.updateOne(
      { _id: session._id },
      { $setOnInsert: session },
      { upsert: true }
    );
    inserted += result.upsertedCount;
    alreadyPresent += result.matchedCount;
  }

  print(`Carga lista en cineconecta: ${inserted} sesiones insertadas, ${alreadyPresent} ya existian.`);
  print(`Total en viewing_sessions: ${target.viewing_sessions.countDocuments({})}`);
}());
