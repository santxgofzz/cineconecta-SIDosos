// CineConecta: Modelo NoSQL / ENTREGA 1 / DEMOSTRACION (no produccion)
// Base exclusiva de prueba, sin borrar bases ni colecciones existentes.
// Ejecutar en mongosh conectado a Atlas o MongoDB local.
(function () {
  const demo = db.getSiblingDB("cineconecta_modelo_demo");
  const schemas = {
  "contents": {
    "bsonType": "object",
    "required": [
      "_id",
      "type",
      "title",
      "synopsis",
      "releaseYear",
      "genres",
      "countries",
      "audioLanguages",
      "subtitleLanguages",
      "ageRating",
      "coverUrl",
      "status",
      "createdAt",
      "updatedAt"
    ],
    "properties": {
      "_id": {
        "bsonType": "string"
      },
      "type": {
        "enum": [
          "movie",
          "series",
          "documentary"
        ]
      },
      "title": {
        "bsonType": "string",
        "minLength": 1
      },
      "synopsis": {
        "bsonType": "string"
      },
      "releaseYear": {
        "bsonType": "int",
        "minimum": 1888
      },
      "genres": {
        "bsonType": "array",
        "items": {
          "bsonType": "string"
        },
        "minItems": 1
      },
      "countries": {
        "bsonType": "array",
        "items": {
          "bsonType": "string"
        },
        "minItems": 1
      },
      "audioLanguages": {
        "bsonType": "array",
        "items": {
          "bsonType": "string"
        },
        "minItems": 1
      },
      "subtitleLanguages": {
        "bsonType": "array",
        "items": {
          "bsonType": "string"
        }
      },
      "ageRating": {
        "bsonType": "int",
        "minimum": 0,
        "maximum": 18
      },
      "coverUrl": {
        "bsonType": "string",
        "minLength": 1
      },
      "status": {
        "enum": [
          "draft",
          "published",
          "retired"
        ]
      },
      "createdAt": {
        "bsonType": "date"
      },
      "updatedAt": {
        "bsonType": "date"
      },
      "durationSeconds": {
        "bsonType": "int",
        "minimum": 1
      },
      "director": {
        "bsonType": "string",
        "minLength": 1
      },
      "cast": {
        "bsonType": "array",
        "items": {
          "bsonType": "object",
          "required": [
            "actorName"
          ],
          "properties": {
            "actorName": {
              "bsonType": "string",
              "minLength": 1
            },
            "characterName": {
              "bsonType": "string"
            }
          }
        }
      },
      "seasons": {
        "bsonType": "array",
        "items": {
          "bsonType": "object",
          "required": [
            "number",
            "description",
            "episodes"
          ],
          "properties": {
            "number": {
              "bsonType": "int",
              "minimum": 1
            },
            "description": {
              "bsonType": "string"
            },
            "episodes": {
              "bsonType": "array",
              "items": {
                "bsonType": "object",
                "required": [
                  "id",
                  "number",
                  "title",
                  "synopsis",
                  "durationSeconds"
                ],
                "properties": {
                  "id": {
                    "bsonType": "string",
                    "minLength": 1
                  },
                  "number": {
                    "bsonType": "int",
                    "minimum": 1
                  },
                  "title": {
                    "bsonType": "string",
                    "minLength": 1
                  },
                  "synopsis": {
                    "bsonType": "string"
                  },
                  "durationSeconds": {
                    "bsonType": "int",
                    "minimum": 1
                  }
                }
              }
            }
          }
        }
      },
      "topics": {
        "bsonType": "array",
        "items": {
          "bsonType": "string"
        }
      },
      "interviewees": {
        "bsonType": "array",
        "items": {
          "bsonType": "string"
        }
      },
      "sources": {
        "bsonType": "array",
        "items": {
          "bsonType": "object",
          "required": [
            "name"
          ],
          "properties": {
            "name": {
              "bsonType": "string",
              "minLength": 1
            },
            "url": {
              "bsonType": "string"
            }
          }
        }
      }
    },
    "oneOf": [
      {
        "required": [
          "durationSeconds",
          "director",
          "cast"
        ],
        "properties": {
          "type": {
            "enum": [
              "movie"
            ]
          }
        }
      },
      {
        "required": [
          "seasons"
        ],
        "properties": {
          "type": {
            "enum": [
              "series"
            ]
          }
        }
      },
      {
        "required": [
          "durationSeconds",
          "director",
          "topics"
        ],
        "properties": {
          "type": {
            "enum": [
              "documentary"
            ]
          }
        }
      }
    ]
  },
  "my_list": {
    "bsonType": "object",
    "required": [
      "_id",
      "profileId",
      "contentId",
      "addedAt"
    ],
    "properties": {
      "_id": {
        "bsonType": "objectId"
      },
      "profileId": {
        "bsonType": "long"
      },
      "contentId": {
        "bsonType": "string",
        "minLength": 1
      },
      "addedAt": {
        "bsonType": "date"
      }
    }
  },
  "viewing_sessions": {
    "bsonType": "object",
    "required": [
      "_id",
      "profileId",
      "subscriptionId",
      "contentId",
      "unitKey",
      "contentTypeSnapshot",
      "startedAt",
      "watchedSeconds",
      "lastPositionSeconds"
    ],
    "properties": {
      "_id": {
        "bsonType": "string",
        "minLength": 1
      },
      "profileId": {
        "bsonType": "long"
      },
      "subscriptionId": {
        "bsonType": "long"
      },
      "contentId": {
        "bsonType": "string",
        "minLength": 1
      },
      "episodeId": {
        "bsonType": "string",
        "minLength": 1
      },
      "unitKey": {
        "bsonType": "string",
        "minLength": 1
      },
      "contentTypeSnapshot": {
        "enum": [
          "movie",
          "series",
          "documentary"
        ]
      },
      "startedAt": {
        "bsonType": "date"
      },
      "endedAt": {
        "bsonType": [
          "date",
          "null"
        ]
      },
      "watchedSeconds": {
        "bsonType": "int",
        "minimum": 0
      },
      "lastPositionSeconds": {
        "bsonType": "int",
        "minimum": 0
      },
      "endReason": {
        "enum": [
          "paused",
          "finished",
          "interrupted",
          null
        ]
      }
    },
    "oneOf": [
      {
        "properties": {
          "contentTypeSnapshot": {
            "enum": [
              "series"
            ]
          }
        },
        "required": [
          "episodeId"
        ]
      },
      {
        "properties": {
          "contentTypeSnapshot": {
            "enum": [
              "movie",
              "documentary"
            ]
          }
        }
      }
    ]
  },
  "viewing_progress": {
    "bsonType": "object",
    "required": [
      "_id",
      "profileId",
      "contentId",
      "unitKey",
      "lastPositionSeconds",
      "durationSeconds",
      "completed",
      "lastActivityAt",
      "lastSessionId"
    ],
    "properties": {
      "_id": {
        "bsonType": "objectId"
      },
      "profileId": {
        "bsonType": "long"
      },
      "contentId": {
        "bsonType": "string",
        "minLength": 1
      },
      "episodeId": {
        "bsonType": "string",
        "minLength": 1
      },
      "unitKey": {
        "bsonType": "string",
        "minLength": 1
      },
      "lastPositionSeconds": {
        "bsonType": "int",
        "minimum": 0
      },
      "durationSeconds": {
        "bsonType": "int",
        "minimum": 1
      },
      "completed": {
        "bsonType": "bool"
      },
      "lastActivityAt": {
        "bsonType": "date"
      },
      "lastSessionId": {
        "bsonType": "string",
        "minLength": 1
      }
    }
  },
  "admin_users": {
    "bsonType": "object",
    "required": [
      "_id",
      "usernameNormalized",
      "passwordHash",
      "role",
      "active",
      "createdAt"
    ],
    "properties": {
      "_id": {
        "bsonType": "objectId"
      },
      "usernameNormalized": {
        "bsonType": "string",
        "minLength": 1
      },
      "passwordHash": {
        "bsonType": "string",
        "minLength": 1
      },
      "role": {
        "enum": [
          "ADMIN"
        ]
      },
      "active": {
        "bsonType": "bool"
      },
      "createdAt": {
        "bsonType": "date"
      }
    }
  }
};
  for (const [name, schema] of Object.entries(schemas)) {
    const cfg = { validator: { $jsonSchema: schema }, validationLevel: "strict", validationAction: "error" };
    if (!demo.getCollectionNames().includes(name)) {
      demo.createCollection(name, cfg);
      print("CREADA: " + name);
    } else {
      const res = demo.runCommand({ collMod: name, ...cfg });
      if (!res.ok) throw Error("No se pudo actualizar el validador de " + name + ": " + tojson(res));
      print("VALIDADOR ACTUALIZADO: " + name);
    }
  }
  demo.contents.createIndex({status: 1, type: 1, ageRating: 1, releaseYear: -1}, {name:"ix_contents_filters"});
  demo.contents.createIndex({title: "text"}, {name:"ix_contents_title_text", default_language:"spanish"});
  demo.my_list.createIndex({profileId: 1, contentId: 1}, {unique:true, name:"uq_my_list_profile_content"});
  demo.viewing_sessions.createIndex({startedAt: 1, subscriptionId: 1}, {name:"ix_sessions_period_sub"});
  demo.viewing_sessions.createIndex({profileId: 1, startedAt: -1}, {name:"ix_sessions_profile_history"});
  demo.viewing_progress.createIndex({profileId: 1, unitKey: 1}, {unique:true, name:"uq_progress_profile_unit"});
  demo.viewing_progress.createIndex({profileId: 1, completed: 1, lastActivityAt: -1}, {name:"ix_progress_continue"});
  demo.admin_users.createIndex({usernameNormalized: 1}, {unique:true, name:"uq_admin_username"});
  print("LISTO: cinco colecciones, validadores e indices en cineconecta_modelo_demo");
}());
