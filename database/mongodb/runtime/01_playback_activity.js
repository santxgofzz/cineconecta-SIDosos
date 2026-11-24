// CineConecta - BD real `cineconecta`: my_list, viewing_sessions, viewing_progress.
// Crea (o actualiza con collMod) las colecciones con $jsonSchema estricto e índices.
// Idempotente: no borra colecciones ni documentos.
//
//   mongosh "mongodb://localhost:27017" --file database/mongodb/runtime/01_playback_activity.js
//
// Basado en database/mongodb/demo/01_crear_colecciones_validadores_indices.js.
// Diferencias: BD `cineconecta` y campos técnicos opcionales en viewing_sessions
// (durationSecondsSnapshot, completedSnapshot, lastEventSeq, lastEventAt) usados por
// el backend para reintentos idempotentes y para limitar el tiempo visto declarado.
(function () {
  const target = db.getSiblingDB("cineconecta");

  const schemas = {
    my_list: {
      bsonType: "object",
      required: ["_id", "profileId", "contentId", "addedAt"],
      properties: {
        _id: { bsonType: "objectId" },
        profileId: { bsonType: "long" },
        contentId: { bsonType: "string", minLength: 1 },
        addedAt: { bsonType: "date" }
      }
    },
    viewing_sessions: {
      bsonType: "object",
      required: ["_id", "profileId", "subscriptionId", "contentId", "unitKey",
                 "contentTypeSnapshot", "startedAt", "watchedSeconds", "lastPositionSeconds"],
      properties: {
        _id: { bsonType: "string", minLength: 1 },
        profileId: { bsonType: "long" },
        subscriptionId: { bsonType: "long" },
        contentId: { bsonType: "string", minLength: 1 },
        episodeId: { bsonType: "string", minLength: 1 },
        unitKey: { bsonType: "string", minLength: 1 },
        contentTypeSnapshot: { enum: ["movie", "series", "documentary"] },
        startedAt: { bsonType: "date" },
        endedAt: { bsonType: ["date", "null"] },
        watchedSeconds: { bsonType: "int", minimum: 0 },
        lastPositionSeconds: { bsonType: "int", minimum: 0 },
        endReason: { enum: ["paused", "finished", "interrupted", null] },
        durationSecondsSnapshot: { bsonType: "int", minimum: 1 },
        completedSnapshot: { bsonType: "bool" },
        lastEventSeq: { bsonType: "int", minimum: 0 },
        lastEventAt: { bsonType: "date" }
      },
      oneOf: [
        { properties: { contentTypeSnapshot: { enum: ["series"] } }, required: ["episodeId"] },
        { properties: { contentTypeSnapshot: { enum: ["movie", "documentary"] } } }
      ]
    },
    viewing_progress: {
      bsonType: "object",
      required: ["_id", "profileId", "contentId", "unitKey", "lastPositionSeconds",
                 "durationSeconds", "completed", "lastActivityAt", "lastSessionId"],
      properties: {
        _id: { bsonType: "objectId" },
        profileId: { bsonType: "long" },
        contentId: { bsonType: "string", minLength: 1 },
        episodeId: { bsonType: "string", minLength: 1 },
        unitKey: { bsonType: "string", minLength: 1 },
        lastPositionSeconds: { bsonType: "int", minimum: 0 },
        durationSeconds: { bsonType: "int", minimum: 1 },
        completed: { bsonType: "bool" },
        lastActivityAt: { bsonType: "date" },
        lastSessionId: { bsonType: "string", minLength: 1 }
      }
    }
  };

  for (const [name, schema] of Object.entries(schemas)) {
    const cfg = { validator: { $jsonSchema: schema }, validationLevel: "strict", validationAction: "error" };
    if (!target.getCollectionNames().includes(name)) {
      target.createCollection(name, cfg);
      print("CREADA: " + name);
    } else {
      const res = target.runCommand({ collMod: name, ...cfg });
      if (!res.ok) throw Error("No se pudo actualizar el validador de " + name + ": " + tojson(res));
      print("VALIDADOR ACTUALIZADO: " + name);
    }
  }

  target.my_list.createIndex({ profileId: 1, contentId: 1 }, { unique: true, name: "uq_my_list_profile_content" });
  target.viewing_sessions.createIndex({ startedAt: 1, subscriptionId: 1 }, { name: "ix_sessions_period_sub" });
  target.viewing_sessions.createIndex({ profileId: 1, startedAt: -1 }, { name: "ix_sessions_profile_history" });
  target.viewing_progress.createIndex({ profileId: 1, unitKey: 1 }, { unique: true, name: "uq_progress_profile_unit" });
  target.viewing_progress.createIndex({ profileId: 1, completed: 1, lastActivityAt: -1 }, { name: "ix_progress_continue" });

  print("LISTO: my_list, viewing_sessions y viewing_progress con validadores e índices en `cineconecta`");
}());
