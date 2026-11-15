// CineConecta: datos sinteticos, DEMO (no son datos reales de Oracle).
// IDs SQL de ejemplo: profileId 15, 16 y 21; subscriptionId 8, 9 y 10.
// EJECUTAR despues de 01_crear_colecciones_validadores_indices.js.
// Reejecutable: replaceOne por _id evita duplicar registros al correrlo de nuevo.
(function () {
  const demo = db.getSiblingDB("cineconecta_modelo_demo");
  const datasets = {
  "contents": [
    {
      "_id": "CNT-001",
      "type": "movie",
      "title": "La ruta del viento",
      "synopsis": "Una familia recorre distintas regiones del país.",
      "releaseYear": {
        "$numberInt": "2022"
      },
      "genres": [
        "Drama"
      ],
      "countries": [
        "Colombia"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [
        "en"
      ],
      "ageRating": {
        "$numberInt": "12"
      },
      "coverUrl": "https://example.org/covers/cnt-001.jpg",
      "status": "published",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "durationSeconds": {
        "$numberInt": "5820"
      },
      "director": "Ana Torres",
      "cast": [
        {
          "actorName": "Luis Pérez",
          "characterName": "Daniel"
        },
        {
          "actorName": "Paula Reyes",
          "characterName": "Elena"
        }
      ]
    },
    {
      "_id": "CNT-002",
      "type": "series",
      "title": "Caminos del mar",
      "synopsis": "Historias de una comunidad costera.",
      "releaseYear": {
        "$numberInt": "2023"
      },
      "genres": [
        "Aventura"
      ],
      "countries": [
        "Colombia"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [],
      "ageRating": {
        "$numberInt": "7"
      },
      "coverUrl": "https://example.org/covers/cnt-002.jpg",
      "status": "published",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "seasons": [
        {
          "number": {
            "$numberInt": "1"
          },
          "description": "El comienzo",
          "episodes": [
            {
              "id": "EP-002-01",
              "number": {
                "$numberInt": "1"
              },
              "title": "La llegada",
              "synopsis": "Comienza el recorrido.",
              "durationSeconds": {
                "$numberInt": "2400"
              }
            },
            {
              "id": "EP-002-02",
              "number": {
                "$numberInt": "2"
              },
              "title": "El viaje",
              "synopsis": "Un nuevo destino.",
              "durationSeconds": {
                "$numberInt": "2520"
              }
            }
          ]
        },
        {
          "number": {
            "$numberInt": "2"
          },
          "description": "Nuevos caminos",
          "episodes": [
            {
              "id": "EP-002-03",
              "number": {
                "$numberInt": "1"
              },
              "title": "El regreso",
              "synopsis": "El grupo vuelve a casa.",
              "durationSeconds": {
                "$numberInt": "2280"
              }
            }
          ]
        }
      ]
    },
    {
      "_id": "CNT-003",
      "type": "documentary",
      "title": "Semillas de futuro",
      "synopsis": "Experiencias sobre agricultura sostenible.",
      "releaseYear": {
        "$numberInt": "2024"
      },
      "genres": [
        "Ciencia"
      ],
      "countries": [
        "Colombia"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [
        "en"
      ],
      "ageRating": {
        "$numberInt": "0"
      },
      "coverUrl": "https://example.org/covers/cnt-003.jpg",
      "status": "published",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "durationSeconds": {
        "$numberInt": "3300"
      },
      "director": "María Rojas",
      "topics": [
        "Agricultura",
        "Sostenibilidad"
      ],
      "interviewees": [
        "Elena Gómez"
      ],
      "sources": [
        {
          "name": "Instituto Agrario",
          "url": "https://example.org"
        }
      ]
    },
    {
      "_id": "CNT-004",
      "type": "movie",
      "title": "La ciudad invisible",
      "synopsis": "Un misterio policial se desarrolla en la ciudad.",
      "releaseYear": {
        "$numberInt": "2021"
      },
      "genres": [
        "Suspenso"
      ],
      "countries": [
        "Colombia"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [],
      "ageRating": {
        "$numberInt": "18"
      },
      "coverUrl": "https://example.org/covers/cnt-004.jpg",
      "status": "published",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "durationSeconds": {
        "$numberInt": "7200"
      },
      "director": "Julián Méndez",
      "cast": [
        {
          "actorName": "Sofía Valencia"
        }
      ]
    },
    {
      "_id": "CNT-005",
      "type": "movie",
      "title": "La última estación",
      "synopsis": "Un viaje que dejó recuerdos.",
      "releaseYear": {
        "$numberInt": "2020"
      },
      "genres": [
        "Drama"
      ],
      "countries": [
        "Colombia"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [],
      "ageRating": {
        "$numberInt": "7"
      },
      "coverUrl": "https://example.org/covers/cnt-005.jpg",
      "status": "retired",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "durationSeconds": {
        "$numberInt": "5400"
      },
      "director": "Laura Muñoz",
      "cast": [
        {
          "actorName": "Carlos Díaz",
          "characterName": "Tomás"
        }
      ]
    },
    {
      "_id": "CNT-006",
      "type": "movie",
      "title": "El último vuelo",
      "synopsis": "Una tripulación enfrenta una emergencia.",
      "releaseYear": {
        "$numberInt": "2025"
      },
      "genres": [
        "Acción"
      ],
      "countries": [
        "Colombia",
        "México"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [
        "en"
      ],
      "ageRating": {
        "$numberInt": "16"
      },
      "coverUrl": "https://example.org/covers/cnt-006.jpg",
      "status": "draft",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "durationSeconds": {
        "$numberInt": "6500"
      },
      "director": "Andrés Salazar",
      "cast": []
    },
    {
      "_id": "CNT-007",
      "type": "documentary",
      "title": "Agua para todos",
      "synopsis": "Una investigación sobre fuentes de agua.",
      "releaseYear": {
        "$numberInt": "2025"
      },
      "genres": [
        "Ciencia",
        "Sociedad"
      ],
      "countries": [
        "Colombia"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [],
      "ageRating": {
        "$numberInt": "12"
      },
      "coverUrl": "https://example.org/covers/cnt-007.jpg",
      "status": "published",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "durationSeconds": {
        "$numberInt": "3600"
      },
      "director": "Valeria Ospina",
      "topics": [
        "Agua",
        "Medio ambiente"
      ]
    },
    {
      "_id": "CNT-008",
      "type": "series",
      "title": "Código nocturno",
      "synopsis": "Un grupo resuelve problemas tecnológicos.",
      "releaseYear": {
        "$numberInt": "2026"
      },
      "genres": [
        "Ciencia ficción",
        "Suspenso"
      ],
      "countries": [
        "Colombia"
      ],
      "audioLanguages": [
        "es"
      ],
      "subtitleLanguages": [
        "en"
      ],
      "ageRating": {
        "$numberInt": "13"
      },
      "coverUrl": "https://example.org/covers/cnt-008.jpg",
      "status": "published",
      "createdAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "updatedAt": {
        "$date": "2026-11-01T12:00:00.000Z"
      },
      "seasons": [
        {
          "number": {
            "$numberInt": "1"
          },
          "description": "Las primeras señales",
          "episodes": [
            {
              "id": "EP-008-01",
              "number": {
                "$numberInt": "1"
              },
              "title": "Mensaje desconocido",
              "synopsis": "Aparece una señal extraña.",
              "durationSeconds": {
                "$numberInt": "2640"
              }
            },
            {
              "id": "EP-008-02",
              "number": {
                "$numberInt": "2"
              },
              "title": "Contraseña",
              "synopsis": "El equipo investiga la señal.",
              "durationSeconds": {
                "$numberInt": "2580"
              }
            }
          ]
        }
      ]
    }
  ],
  "my_list": [
    {
      "_id": {
        "$oid": "650000000000000000000001"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-001",
      "addedAt": {
        "$date": "2026-11-03T10:00:00.000Z"
      }
    },
    {
      "_id": {
        "$oid": "650000000000000000000002"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-002",
      "addedAt": {
        "$date": "2026-11-03T10:05:00.000Z"
      }
    },
    {
      "_id": {
        "$oid": "650000000000000000000003"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-005",
      "addedAt": {
        "$date": "2026-11-03T10:10:00.000Z"
      }
    },
    {
      "_id": {
        "$oid": "650000000000000000000004"
      },
      "profileId": {
        "$numberLong": "16"
      },
      "contentId": "CNT-003",
      "addedAt": {
        "$date": "2026-11-04T10:00:00.000Z"
      }
    },
    {
      "_id": {
        "$oid": "650000000000000000000005"
      },
      "profileId": {
        "$numberLong": "21"
      },
      "contentId": "CNT-004",
      "addedAt": {
        "$date": "2026-11-04T12:00:00.000Z"
      }
    },
    {
      "_id": {
        "$oid": "650000000000000000000006"
      },
      "profileId": {
        "$numberLong": "21"
      },
      "contentId": "CNT-008",
      "addedAt": {
        "$date": "2026-11-06T12:00:00.000Z"
      }
    }
  ],
  "viewing_sessions": [
    {
      "_id": "SES-001",
      "profileId": {
        "$numberLong": "15"
      },
      "subscriptionId": {
        "$numberLong": "8"
      },
      "contentId": "CNT-002",
      "unitKey": "EP-002-01",
      "contentTypeSnapshot": "series",
      "startedAt": {
        "$date": "2026-11-02T15:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-02T15:20:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "900"
      },
      "lastPositionSeconds": {
        "$numberInt": "1200"
      },
      "endReason": "paused",
      "episodeId": "EP-002-01"
    },
    {
      "_id": "SES-002",
      "profileId": {
        "$numberLong": "15"
      },
      "subscriptionId": {
        "$numberLong": "8"
      },
      "contentId": "CNT-001",
      "unitKey": "CNT-001",
      "contentTypeSnapshot": "movie",
      "startedAt": {
        "$date": "2026-11-03T15:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-03T15:35:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "1800"
      },
      "lastPositionSeconds": {
        "$numberInt": "4000"
      },
      "endReason": "paused"
    },
    {
      "_id": "SES-003",
      "profileId": {
        "$numberLong": "16"
      },
      "subscriptionId": {
        "$numberLong": "8"
      },
      "contentId": "CNT-003",
      "unitKey": "CNT-003",
      "contentTypeSnapshot": "documentary",
      "startedAt": {
        "$date": "2026-11-04T11:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-04T11:50:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "2800"
      },
      "lastPositionSeconds": {
        "$numberInt": "3000"
      },
      "endReason": "finished"
    },
    {
      "_id": "SES-004",
      "profileId": {
        "$numberLong": "15"
      },
      "subscriptionId": {
        "$numberLong": "8"
      },
      "contentId": "CNT-005",
      "unitKey": "CNT-005",
      "contentTypeSnapshot": "movie",
      "startedAt": {
        "$date": "2026-11-04T17:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-04T17:25:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "1200"
      },
      "lastPositionSeconds": {
        "$numberInt": "1200"
      },
      "endReason": "paused"
    },
    {
      "_id": "SES-005",
      "profileId": {
        "$numberLong": "21"
      },
      "subscriptionId": {
        "$numberLong": "9"
      },
      "contentId": "CNT-004",
      "unitKey": "CNT-004",
      "contentTypeSnapshot": "movie",
      "startedAt": {
        "$date": "2026-11-05T19:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-05T21:00:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "6400"
      },
      "lastPositionSeconds": {
        "$numberInt": "6500"
      },
      "endReason": "finished"
    },
    {
      "_id": "SES-006",
      "profileId": {
        "$numberLong": "15"
      },
      "subscriptionId": {
        "$numberLong": "10"
      },
      "contentId": "CNT-002",
      "unitKey": "EP-002-02",
      "contentTypeSnapshot": "series",
      "startedAt": {
        "$date": "2026-11-08T15:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-08T15:45:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "2400"
      },
      "lastPositionSeconds": {
        "$numberInt": "2450"
      },
      "endReason": "finished",
      "episodeId": "EP-002-02"
    },
    {
      "_id": "SES-007",
      "profileId": {
        "$numberLong": "15"
      },
      "subscriptionId": {
        "$numberLong": "10"
      },
      "contentId": "CNT-001",
      "unitKey": "CNT-001",
      "contentTypeSnapshot": "movie",
      "startedAt": {
        "$date": "2026-11-09T19:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-09T19:25:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "1000"
      },
      "lastPositionSeconds": {
        "$numberInt": "5000"
      },
      "endReason": "paused"
    },
    {
      "_id": "SES-008",
      "profileId": {
        "$numberLong": "15"
      },
      "subscriptionId": {
        "$numberLong": "10"
      },
      "contentId": "CNT-002",
      "unitKey": "EP-002-03",
      "contentTypeSnapshot": "series",
      "startedAt": {
        "$date": "2026-11-10T14:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-10T14:20:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "700"
      },
      "lastPositionSeconds": {
        "$numberInt": "1000"
      },
      "endReason": "paused",
      "episodeId": "EP-002-03"
    },
    {
      "_id": "SES-009",
      "profileId": {
        "$numberLong": "21"
      },
      "subscriptionId": {
        "$numberLong": "9"
      },
      "contentId": "CNT-007",
      "unitKey": "CNT-007",
      "contentTypeSnapshot": "documentary",
      "startedAt": {
        "$date": "2026-11-11T10:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-11T10:15:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "500"
      },
      "lastPositionSeconds": {
        "$numberInt": "600"
      },
      "endReason": "paused"
    },
    {
      "_id": "SES-010",
      "profileId": {
        "$numberLong": "21"
      },
      "subscriptionId": {
        "$numberLong": "9"
      },
      "contentId": "CNT-008",
      "unitKey": "EP-008-01",
      "contentTypeSnapshot": "series",
      "startedAt": {
        "$date": "2026-11-12T11:00:00.000Z"
      },
      "endedAt": {
        "$date": "2026-11-12T11:20:00.000Z"
      },
      "watchedSeconds": {
        "$numberInt": "850"
      },
      "lastPositionSeconds": {
        "$numberInt": "1000"
      },
      "endReason": "paused",
      "episodeId": "EP-008-01"
    }
  ],
  "viewing_progress": [
    {
      "_id": {
        "$oid": "660000000000000000000001"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-002",
      "unitKey": "EP-002-01",
      "lastPositionSeconds": {
        "$numberInt": "1200"
      },
      "durationSeconds": {
        "$numberInt": "2400"
      },
      "completed": false,
      "lastActivityAt": {
        "$date": "2026-11-02T15:20:00.000Z"
      },
      "lastSessionId": "SES-001",
      "episodeId": "EP-002-01"
    },
    {
      "_id": {
        "$oid": "660000000000000000000002"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-001",
      "unitKey": "CNT-001",
      "lastPositionSeconds": {
        "$numberInt": "5000"
      },
      "durationSeconds": {
        "$numberInt": "5820"
      },
      "completed": false,
      "lastActivityAt": {
        "$date": "2026-11-09T19:25:00.000Z"
      },
      "lastSessionId": "SES-007"
    },
    {
      "_id": {
        "$oid": "660000000000000000000003"
      },
      "profileId": {
        "$numberLong": "16"
      },
      "contentId": "CNT-003",
      "unitKey": "CNT-003",
      "lastPositionSeconds": {
        "$numberInt": "3000"
      },
      "durationSeconds": {
        "$numberInt": "3300"
      },
      "completed": true,
      "lastActivityAt": {
        "$date": "2026-11-04T11:50:00.000Z"
      },
      "lastSessionId": "SES-003"
    },
    {
      "_id": {
        "$oid": "660000000000000000000004"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-005",
      "unitKey": "CNT-005",
      "lastPositionSeconds": {
        "$numberInt": "1200"
      },
      "durationSeconds": {
        "$numberInt": "5400"
      },
      "completed": false,
      "lastActivityAt": {
        "$date": "2026-11-04T17:25:00.000Z"
      },
      "lastSessionId": "SES-004"
    },
    {
      "_id": {
        "$oid": "660000000000000000000005"
      },
      "profileId": {
        "$numberLong": "21"
      },
      "contentId": "CNT-004",
      "unitKey": "CNT-004",
      "lastPositionSeconds": {
        "$numberInt": "6500"
      },
      "durationSeconds": {
        "$numberInt": "7200"
      },
      "completed": true,
      "lastActivityAt": {
        "$date": "2026-11-05T21:00:00.000Z"
      },
      "lastSessionId": "SES-005"
    },
    {
      "_id": {
        "$oid": "660000000000000000000006"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-002",
      "unitKey": "EP-002-02",
      "lastPositionSeconds": {
        "$numberInt": "2450"
      },
      "durationSeconds": {
        "$numberInt": "2520"
      },
      "completed": true,
      "lastActivityAt": {
        "$date": "2026-11-08T15:45:00.000Z"
      },
      "lastSessionId": "SES-006",
      "episodeId": "EP-002-02"
    },
    {
      "_id": {
        "$oid": "660000000000000000000007"
      },
      "profileId": {
        "$numberLong": "15"
      },
      "contentId": "CNT-002",
      "unitKey": "EP-002-03",
      "lastPositionSeconds": {
        "$numberInt": "1000"
      },
      "durationSeconds": {
        "$numberInt": "2280"
      },
      "completed": false,
      "lastActivityAt": {
        "$date": "2026-11-10T14:20:00.000Z"
      },
      "lastSessionId": "SES-008",
      "episodeId": "EP-002-03"
    },
    {
      "_id": {
        "$oid": "660000000000000000000008"
      },
      "profileId": {
        "$numberLong": "21"
      },
      "contentId": "CNT-007",
      "unitKey": "CNT-007",
      "lastPositionSeconds": {
        "$numberInt": "600"
      },
      "durationSeconds": {
        "$numberInt": "3600"
      },
      "completed": false,
      "lastActivityAt": {
        "$date": "2026-11-11T10:15:00.000Z"
      },
      "lastSessionId": "SES-009"
    },
    {
      "_id": {
        "$oid": "660000000000000000000009"
      },
      "profileId": {
        "$numberLong": "21"
      },
      "contentId": "CNT-008",
      "unitKey": "EP-008-01",
      "lastPositionSeconds": {
        "$numberInt": "1000"
      },
      "durationSeconds": {
        "$numberInt": "2640"
      },
      "completed": false,
      "lastActivityAt": {
        "$date": "2026-11-12T11:20:00.000Z"
      },
      "lastSessionId": "SES-010",
      "episodeId": "EP-008-01"
    }
  ],
  "admin_users": [
    {
      "_id": {
        "$oid": "670000000000000000000001"
      },
      "usernameNormalized": "admin.demo",
      "passwordHash": "NO_ES_UN_HASH_REAL_NO_USAR_EN_LOGIN_DEMO_2026",
      "role": "ADMIN",
      "active": false,
      "createdAt": {
        "$date": "2026-11-01T09:00:00.000Z"
      }
    }
  ]
};
  for (const [name, docs] of Object.entries(datasets)) {
    if (!demo.getCollectionNames().includes(name)) throw Error("Falta crear coleccion " + name + ". Ejecuta el script 01.");
    const collection = demo.getCollection(name);
    for (const raw of docs) {
      const doc = EJSON.deserialize(raw, {relaxed:false});
      collection.replaceOne({_id:doc._id}, doc, {upsert:true});
    }
    print(name + ": " + collection.countDocuments({}) + " documentos en la base de prueba");
  }
  print("CARGA FINALIZADA. Refresca MongoDB Compass.");
}());
