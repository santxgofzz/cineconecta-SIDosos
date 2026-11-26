// Semilla de 48 peliculas y documentales reales extraidos de loadMovieDetailsDataset.js.
// Ejecutar despues de 02_catalog_admin.js en cineconecta o cineconecta_test_*.
(function () {
  const name = process.env.CINE_MONGO_TARGET_DB;
  const uri = db.getMongo().getURI();
  if (!name || db.getName() !== name || !/^cineconecta(?:_test_[a-z0-9_]+)?$/.test(name)
      || !/^mongodb:\/\/(?:[^/@]+@)?(?:127\.0\.0\.1|localhost):\d+\//.test(uri)) {
    throw Error("La carga requiere una conexion local y la BD explicita en CINE_MONGO_TARGET_DB.");
  }
  if (!db.getCollectionInfos({ name: "contents", "options.validationLevel": "strict" }).length) {
    throw Error("Primero ejecuta 02_catalog_admin.js.");
  }

  const now = new Date("2026-11-25T00:00:00.000Z");
  const rawDocs = [
  {
    "_id": "MV-tt0076759",
    "type": "movie",
    "title": "Star Wars: Episode IV - A New Hope",
    "synopsis": "Luke Skywalker joins forces with a Jedi Knight, a cocky pilot, a wookiee and two droids to save the universe from the Empire's world-destroying battle-station, while also attempting to rescue Princess Leia from the evil Darth Vader.",
    "releaseYear": 1977,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTU4NTczODkwM15BMl5BanBnXkFtZTcwMzEyMTIyMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7260,
    "director": "George Lucas",
    "cast": [
      {
        "actorName": "Mark Hamill"
      },
      {
        "actorName": "Harrison Ford"
      },
      {
        "actorName": "Carrie Fisher"
      },
      {
        "actorName": "Peter Cushing"
      }
    ]
  },
  {
    "_id": "MV-tt1130884",
    "type": "movie",
    "title": "Shutter Island",
    "synopsis": "A U.S Marshal investigates the disappearance of a murderess who escaped from a hospital for the criminally insane.",
    "releaseYear": 2010,
    "genres": [
      "Mystery",
      "Thriller"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTMxMTIyNzMxMV5BMl5BanBnXkFtZTcwOTc4OTI3Mg@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8280,
    "director": "Martin Scorsese",
    "cast": [
      {
        "actorName": "Leonardo DiCaprio"
      },
      {
        "actorName": "Mark Ruffalo"
      },
      {
        "actorName": "Ben Kingsley"
      },
      {
        "actorName": "Max von Sydow"
      }
    ]
  },
  {
    "_id": "MV-tt0371746",
    "type": "movie",
    "title": "Iron Man",
    "synopsis": "After being held captive in an Afghan cave, an industrialist creates a unique weaponized suit of armor to fight evil.",
    "releaseYear": 2008,
    "genres": [
      "Action",
      "Adventure",
      "Sci-Fi"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTczNTI2ODUwOF5BMl5BanBnXkFtZTcwMTU0NTIzMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7560,
    "director": "Jon Favreau",
    "cast": [
      {
        "actorName": "Robert Downey Jr."
      },
      {
        "actorName": "Terrence Howard"
      },
      {
        "actorName": "Jeff Bridges"
      },
      {
        "actorName": "Gwyneth Paltrow"
      }
    ]
  },
  {
    "_id": "MV-tt0082971",
    "type": "movie",
    "title": "Raiders of the Lost Ark",
    "synopsis": "Archaeologist and adventurer Indiana Jones is hired by the US government to find the Ark of the Covenant before the Nazis.",
    "releaseYear": 1981,
    "genres": [
      "Action",
      "Adventure"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjA0ODEzMTc1Nl5BMl5BanBnXkFtZTcwODM2MjAxNA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6900,
    "director": "Steven Spielberg",
    "cast": [
      {
        "actorName": "Harrison Ford"
      },
      {
        "actorName": "Karen Allen"
      },
      {
        "actorName": "Paul Freeman"
      },
      {
        "actorName": "Ronald Lacey"
      }
    ]
  },
  {
    "_id": "MV-tt0903624",
    "type": "movie",
    "title": "The Hobbit: An Unexpected Journey",
    "synopsis": "A reluctant hobbit, Bilbo Baggins, sets out to the Lonely Mountain with a spirited group of dwarves to reclaim their mountain home - and the gold within it - from the dragon Smaug.",
    "releaseYear": 2012,
    "genres": [
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA",
      "New Zealand"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTcwNTE4MTUxMl5BMl5BanBnXkFtZTcwMDIyODM4OA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 10140,
    "director": "Peter Jackson",
    "cast": [
      {
        "actorName": "Ian McKellen"
      },
      {
        "actorName": "Martin Freeman"
      },
      {
        "actorName": "Richard Armitage"
      },
      {
        "actorName": "Ken Stott"
      }
    ]
  },
  {
    "_id": "MV-tt0477348",
    "type": "movie",
    "title": "No Country for Old Men",
    "synopsis": "Violence and mayhem ensue after a hunter stumbles upon a drug deal gone wrong and more than two million dollars in cash near the Rio Grande.",
    "releaseYear": 2007,
    "genres": [
      "Crime",
      "Drama",
      "Thriller"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjA5Njk3MjM4OV5BMl5BanBnXkFtZTcwMTc5MTE1MQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7320,
    "director": "Ethan Coen, Joel Coen",
    "cast": [
      {
        "actorName": "Tommy Lee Jones"
      },
      {
        "actorName": "Javier Bardem"
      },
      {
        "actorName": "Josh Brolin"
      },
      {
        "actorName": "Woody Harrelson"
      }
    ]
  },
  {
    "_id": "MV-tt1300854",
    "type": "movie",
    "title": "Iron Man 3",
    "synopsis": "When Tony Stark's world is torn apart by a formidable terrorist called the Mandarin, he starts an odyssey of rebuilding and retribution.",
    "releaseYear": 2013,
    "genres": [
      "Action",
      "Adventure",
      "Sci-Fi"
    ],
    "countries": [
      "USA",
      "China"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjIzMzAzMjQyM15BMl5BanBnXkFtZTcwNzM2NjcyOQ@@._V1._SY317_SY132_CR0,0,89,132_AL_.jpg_V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7800,
    "director": "Shane Black",
    "cast": [
      {
        "actorName": "Robert Downey Jr."
      },
      {
        "actorName": "Gwyneth Paltrow"
      },
      {
        "actorName": "Don Cheadle"
      },
      {
        "actorName": "Guy Pearce"
      }
    ]
  },
  {
    "_id": "MV-tt0145487",
    "type": "movie",
    "title": "Spider-Man",
    "synopsis": "When bitten by a genetically modified spider, a nerdy, shy, and awkward high school student gains spider-like abilities that he eventually must use to fight evil as a superhero after tragedy befalls his family.",
    "releaseYear": 2002,
    "genres": [
      "Action",
      "Adventure"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMzk3MTE5MDU5NV5BMl5BanBnXkFtZTYwMjY3NTY3._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7260,
    "director": "Sam Raimi",
    "cast": [
      {
        "actorName": "Tobey Maguire"
      },
      {
        "actorName": "Willem Dafoe"
      },
      {
        "actorName": "Kirsten Dunst"
      },
      {
        "actorName": "James Franco"
      }
    ]
  },
  {
    "_id": "MV-tt2267998",
    "type": "movie",
    "title": "Gone Girl",
    "synopsis": "With his wife's disappearance having become the focus of an intense media circus, a man sees the spotlight turned on him when it's suspected that he may not be innocent.",
    "releaseYear": 2014,
    "genres": [
      "Crime",
      "Drama",
      "Mystery"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTk0MDQ3MzAzOV5BMl5BanBnXkFtZTgwNzU1NzE3MjE@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8940,
    "director": "David Fincher",
    "cast": [
      {
        "actorName": "Ben Affleck"
      },
      {
        "actorName": "Rosamund Pike"
      },
      {
        "actorName": "Neil Patrick Harris"
      },
      {
        "actorName": "Tyler Perry"
      }
    ]
  },
  {
    "_id": "MV-tt0435761",
    "type": "movie",
    "title": "Toy Story 3",
    "synopsis": "The toys are mistakenly delivered to a day-care center instead of the attic right before Andy leaves for college, and it's up to Woody to convince the other toys that they weren't abandoned and to return home.",
    "releaseYear": 2010,
    "genres": [
      "Animation",
      "Adventure",
      "Comedy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 0,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTgxOTY4Mjc0MF5BMl5BanBnXkFtZTcwNTA4MDQyMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6180,
    "director": "Lee Unkrich",
    "cast": [
      {
        "actorName": "Tom Hanks"
      },
      {
        "actorName": "Tim Allen"
      },
      {
        "actorName": "Joan Cusack"
      },
      {
        "actorName": "Ned Beatty"
      }
    ]
  },
  {
    "_id": "MV-tt0770828",
    "type": "movie",
    "title": "Man of Steel",
    "synopsis": "Clark Kent, one of the last of an extinguished race disguised as an unremarkable human, is forced to reveal his identity when Earth is invaded by an army of survivors who threaten to bring the planet to the brink of destruction.",
    "releaseYear": 2013,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA",
      "Canada",
      "UK"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjI5OTYzNjI0Ml5BMl5BanBnXkFtZTcwMzM1NDA1OQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8580,
    "director": "Zack Snyder",
    "cast": [
      {
        "actorName": "Henry Cavill"
      },
      {
        "actorName": "Amy Adams"
      },
      {
        "actorName": "Michael Shannon"
      },
      {
        "actorName": "Diane Lane"
      }
    ]
  },
  {
    "_id": "MV-tt0120915",
    "type": "movie",
    "title": "Star Wars: Episode I - The Phantom Menace",
    "synopsis": "Two Jedi Knights escape a hostile blockade to find allies and come across a young boy who may bring balance to the Force, but the long dormant Sith resurface to reclaim their old glory.",
    "releaseYear": 1999,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTQ4NjEwNDA2Nl5BMl5BanBnXkFtZTcwNDUyNDQzNw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8160,
    "director": "George Lucas",
    "cast": [
      {
        "actorName": "Liam Neeson"
      },
      {
        "actorName": "Ewan McGregor"
      },
      {
        "actorName": "Natalie Portman"
      },
      {
        "actorName": "Jake Lloyd"
      }
    ]
  },
  {
    "_id": "MV-tt0383574",
    "type": "movie",
    "title": "Pirates of the Caribbean: Dead Man's Chest",
    "synopsis": "Jack Sparrow races to recover the heart of Davy Jones to avoid enslaving his soul to Jones' service, as other friends and foes seek the heart for their own agenda as well.",
    "releaseYear": 2006,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTcwODc1MTMxM15BMl5BanBnXkFtZTYwMDg1NzY3._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 9060,
    "director": "Gore Verbinski",
    "cast": [
      {
        "actorName": "Johnny Depp"
      },
      {
        "actorName": "Orlando Bloom"
      },
      {
        "actorName": "Keira Knightley"
      },
      {
        "actorName": "Jack Davenport"
      }
    ]
  },
  {
    "_id": "MV-tt0121766",
    "type": "movie",
    "title": "Star Wars: Episode III - Revenge of the Sith",
    "synopsis": "Three years after the onset of the Clone Wars; the noble Jedi Knights are spread out across the galaxy leading a massive clone army in the war against the Separatists. After Chancellor ...",
    "releaseYear": 2005,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BNTc4MTc3NTQ5OF5BMl5BanBnXkFtZTcwOTg0NjI4NA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8400,
    "director": "George Lucas",
    "cast": [
      {
        "actorName": "Ewan McGregor"
      },
      {
        "actorName": "Natalie Portman"
      },
      {
        "actorName": "Hayden Christensen"
      },
      {
        "actorName": "Ian McDiarmid"
      }
    ]
  },
  {
    "_id": "MV-tt0796366",
    "type": "movie",
    "title": "Star Trek",
    "synopsis": "The brash James T. Kirk tries to live up to his father's legacy with Mr. Spock keeping him in check as a vengeful, time-traveling Romulan creates black holes to destroy the Federation one planet at a time.",
    "releaseYear": 2009,
    "genres": [
      "Action",
      "Adventure",
      "Sci-Fi"
    ],
    "countries": [
      "USA",
      "Germany"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjE5NDQ5OTE4Ml5BMl5BanBnXkFtZTcwOTE3NDIzMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7620,
    "director": "J.J. Abrams",
    "cast": [
      {
        "actorName": "Chris Pine"
      },
      {
        "actorName": "Zachary Quinto"
      },
      {
        "actorName": "Leonard Nimoy"
      },
      {
        "actorName": "Eric Bana"
      }
    ]
  },
  {
    "_id": "MV-tt1228705",
    "type": "movie",
    "title": "Iron Man 2",
    "synopsis": "With the world now aware of his identity as Iron Man, Tony Stark must contend with both his declining health and a vengeful mad man with ties to his father's legacy.",
    "releaseYear": 2010,
    "genres": [
      "Action",
      "Adventure",
      "Sci-Fi"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTM0MDgwNjMyMl5BMl5BanBnXkFtZTcwNTg3NzAzMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7440,
    "director": "Jon Favreau",
    "cast": [
      {
        "actorName": "Robert Downey Jr."
      },
      {
        "actorName": "Gwyneth Paltrow"
      },
      {
        "actorName": "Don Cheadle"
      },
      {
        "actorName": "Scarlett Johansson"
      }
    ]
  },
  {
    "_id": "MV-tt0264464",
    "type": "movie",
    "title": "Catch Me If You Can",
    "synopsis": "A true story about Frank Abagnale Jr., who, before his 19th birthday, successfully conned millions of dollars' worth of checks as a Pan Am pilot, doctor, and legal prosecutor.",
    "releaseYear": 2002,
    "genres": [
      "Biography",
      "Crime",
      "Drama"
    ],
    "countries": [
      "USA",
      "Canada"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTY5MzYzNjc5NV5BMl5BanBnXkFtZTYwNTUyNTc2._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8460,
    "director": "Steven Spielberg",
    "cast": [
      {
        "actorName": "Leonardo DiCaprio"
      },
      {
        "actorName": "Tom Hanks"
      },
      {
        "actorName": "Christopher Walken"
      },
      {
        "actorName": "Martin Sheen"
      }
    ]
  },
  {
    "_id": "MV-tt0816711",
    "type": "movie",
    "title": "World War Z",
    "synopsis": "United Nations employee Gerry Lane traverses the world in a race against time to stop the Zombie pandemic that is toppling armies and governments, and threatening to destroy humanity itself.",
    "releaseYear": 2013,
    "genres": [
      "Action",
      "Adventure",
      "Horror"
    ],
    "countries": [
      "USA",
      "Malta"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTg0NTgxMjIxOF5BMl5BanBnXkFtZTcwMDM0MDY1OQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6960,
    "director": "Marc Forster",
    "cast": [
      {
        "actorName": "Brad Pitt"
      },
      {
        "actorName": "Mireille Enos"
      },
      {
        "actorName": "Daniella Kertesz"
      },
      {
        "actorName": "James Badge Dale"
      }
    ]
  },
  {
    "_id": "MV-tt0121765",
    "type": "movie",
    "title": "Star Wars: Episode II - Attack of the Clones",
    "synopsis": "Ten years after initially meeting, Anakin Skywalker shares a forbidden romance with Padmé, while Obi-Wan investigates an assassination attempt on the Senator and discovers a secret clone army crafted for the Jedi.",
    "releaseYear": 2002,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTY5MjI5NTIwNl5BMl5BanBnXkFtZTYwMTM1Njg2._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8520,
    "director": "George Lucas",
    "cast": [
      {
        "actorName": "Ewan McGregor"
      },
      {
        "actorName": "Natalie Portman"
      },
      {
        "actorName": "Hayden Christensen"
      },
      {
        "actorName": "Christopher Lee"
      }
    ]
  },
  {
    "_id": "MV-tt0948470",
    "type": "movie",
    "title": "The Amazing Spider-Man",
    "synopsis": "After Peter Parker is bitten by a genetically altered spider, he gains newfound, spider-like powers and ventures out to solve the mystery of his parent's mysterious death.",
    "releaseYear": 2012,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjMyOTM4MDMxNV5BMl5BanBnXkFtZTcwNjIyNzExOA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8160,
    "director": "Marc Webb",
    "cast": [
      {
        "actorName": "Andrew Garfield"
      },
      {
        "actorName": "Emma Stone"
      },
      {
        "actorName": "Rhys Ifans"
      },
      {
        "actorName": "Denis Leary"
      }
    ]
  },
  {
    "_id": "MV-tt0454876",
    "type": "movie",
    "title": "Life of Pi",
    "synopsis": "A young man who survives a disaster at sea is hurtled into an epic journey of adventure and discovery. While cast away, he forms an unexpected connection with another survivor: a fearsome Bengal tiger.",
    "releaseYear": 2012,
    "genres": [
      "Adventure",
      "Drama",
      "Fantasy"
    ],
    "countries": [
      "USA",
      "Taiwan",
      "UK"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BNTg2OTY2ODg5OF5BMl5BanBnXkFtZTcwODM5MTYxOA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7620,
    "director": "Ang Lee",
    "cast": [
      {
        "actorName": "Suraj Sharma"
      },
      {
        "actorName": "Irrfan Khan"
      },
      {
        "actorName": "Ayush Tandon"
      },
      {
        "actorName": "Gautam Belur"
      }
    ]
  },
  {
    "_id": "MV-tt0062622",
    "type": "movie",
    "title": "2001: A Space Odyssey",
    "synopsis": "Humanity finds a mysterious, obviously artificial object buried beneath the Lunar surface and, with the intelligent computer H.A.L. 9000, sets off on a quest.",
    "releaseYear": 1968,
    "genres": [
      "Mystery",
      "Sci-Fi"
    ],
    "countries": [
      "USA",
      "UK"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 0,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BNDYyMDgxNDQ5Nl5BMl5BanBnXkFtZTcwMjc1ODg3OA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8940,
    "director": "Stanley Kubrick",
    "cast": [
      {
        "actorName": "Keir Dullea"
      },
      {
        "actorName": "Gary Lockwood"
      },
      {
        "actorName": "William Sylvester"
      },
      {
        "actorName": "Daniel Richter"
      }
    ]
  },
  {
    "_id": "MV-tt0758758",
    "type": "movie",
    "title": "Into the Wild",
    "synopsis": "After graduating from Emory University, top student and athlete Christopher McCandless abandons his possessions, gives his entire $24,000 savings account to charity and hitchhikes to Alaska to live in the wilderness. Along the way, Christopher encounters a series of characters that shape his life.",
    "releaseYear": 2007,
    "genres": [
      "Adventure",
      "Biography",
      "Drama"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTAwNDEyODU1MjheQTJeQWpwZ15BbWU2MDc3NDQwNw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8880,
    "director": "Sean Penn",
    "cast": [
      {
        "actorName": "Emile Hirsch"
      },
      {
        "actorName": "Marcia Gay Harden"
      },
      {
        "actorName": "William Hurt"
      },
      {
        "actorName": "Jena Malone"
      }
    ]
  },
  {
    "_id": "MV-tt1318514",
    "type": "movie",
    "title": "Rise of the Planet of the Apes",
    "synopsis": "A substance, designed to help the brain repair itself, gives rise to a super-intelligent chimp who leads an ape uprising.",
    "releaseYear": 2011,
    "genres": [
      "Action",
      "Drama",
      "Sci-Fi"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTQyMjUxNTc0Ml5BMl5BanBnXkFtZTcwMjg1ODExNg@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6300,
    "director": "Rupert Wyatt",
    "cast": [
      {
        "actorName": "Andy Serkis"
      },
      {
        "actorName": "Karin Konoval"
      },
      {
        "actorName": "Terry Notary"
      },
      {
        "actorName": "Richard Ridings"
      }
    ]
  },
  {
    "_id": "MV-tt0365748",
    "type": "movie",
    "title": "Shaun of the Dead",
    "synopsis": "A man decides to turn his moribund life around by winning back his ex-girlfriend, reconciling his relationship with his mother, and dealing with an entire community that has returned from the dead to eat the living.",
    "releaseYear": 2004,
    "genres": [
      "Comedy",
      "Horror"
    ],
    "countries": [
      "UK",
      "France"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTU2NjA0NDk0NV5BMl5BanBnXkFtZTcwOTA0OTQzMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 5940,
    "director": "Edgar Wright",
    "cast": [
      {
        "actorName": "Simon Pegg"
      },
      {
        "actorName": "Kate Ashfield"
      },
      {
        "actorName": "Nick Frost"
      },
      {
        "actorName": "Lucy Davis"
      }
    ]
  },
  {
    "_id": "MV-tt1981115",
    "type": "movie",
    "title": "Thor: The Dark World",
    "synopsis": "When Dr Jane Foster gets cursed with a powerful object, Thor must protect it before an army and its ruthless leader try to get their hands on it to take over the remains of Earth.",
    "releaseYear": 2013,
    "genres": [
      "Action",
      "Adventure",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTQyNzAwOTUxOF5BMl5BanBnXkFtZTcwMTE0OTc5OQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6720,
    "director": "Alan Taylor",
    "cast": [
      {
        "actorName": "Chris Hemsworth"
      },
      {
        "actorName": "Natalie Portman"
      },
      {
        "actorName": "Tom Hiddleston"
      },
      {
        "actorName": "Anthony Hopkins"
      }
    ]
  },
  {
    "_id": "MV-tt1408101",
    "type": "movie",
    "title": "Star Trek Into Darkness",
    "synopsis": "After the crew of the Enterprise find an unstoppable force of terror from within their own organization, Captain Kirk leads a manhunt to a war-zone world to capture a one-man weapon of mass destruction.",
    "releaseYear": 2013,
    "genres": [
      "Action",
      "Adventure",
      "Sci-Fi"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTk2NzczOTgxNF5BMl5BanBnXkFtZTcwODQ5ODczOQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7920,
    "director": "J.J. Abrams",
    "cast": [
      {
        "actorName": "Chris Pine"
      },
      {
        "actorName": "Zachary Quinto"
      },
      {
        "actorName": "Zoe Saldana"
      },
      {
        "actorName": "Karl Urban"
      }
    ]
  },
  {
    "_id": "MV-tt0095953",
    "type": "movie",
    "title": "Rain Man",
    "synopsis": "Selfish yuppie Charlie Babbitt's father left a fortune to his savant brother Raymond and a pittance to Charlie; they travel cross-country.",
    "releaseYear": 1988,
    "genres": [
      "Drama"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTQ4NTA1NDU3NV5BMl5BanBnXkFtZTcwODUwMTU2NA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7980,
    "director": "Barry Levinson",
    "cast": [
      {
        "actorName": "Dustin Hoffman"
      },
      {
        "actorName": "Tom Cruise"
      },
      {
        "actorName": "Valeria Golino"
      },
      {
        "actorName": "Gerald R. Molen"
      }
    ]
  },
  {
    "_id": "MV-tt0120363",
    "type": "movie",
    "title": "Toy Story 2",
    "synopsis": "When Woody is stolen by a toy collector, Buzz and his friends vow to rescue him, but Woody finds the idea of immortality in a museum tempting.",
    "releaseYear": 1999,
    "genres": [
      "Animation",
      "Adventure",
      "Comedy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 0,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTQ0OTU0NTcyNl5BMl5BanBnXkFtZTcwOTk5Mjc4OA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 5520,
    "director": "John Lasseter, Ash Brannon, Lee Unkrich",
    "cast": [
      {
        "actorName": "Tom Hanks"
      },
      {
        "actorName": "Tim Allen"
      },
      {
        "actorName": "Joan Cusack"
      },
      {
        "actorName": "Kelsey Grammer"
      }
    ]
  },
  {
    "_id": "MV-tt1411697",
    "type": "movie",
    "title": "The Hangover Part II",
    "synopsis": "Two years after the bachelor party in Las Vegas, Phil, Stu, Alan, and Doug jet to Thailand for Stu's wedding. Stu's plan for a subdued pre-wedding brunch, however, goes seriously awry.",
    "releaseYear": 2011,
    "genres": [
      "Comedy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTM2MTM4MzY2OV5BMl5BanBnXkFtZTcwNjQ3NzI4NA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6120,
    "director": "Todd Phillips",
    "cast": [
      {
        "actorName": "Bradley Cooper"
      },
      {
        "actorName": "Ed Helms"
      },
      {
        "actorName": "Zach Galifianakis"
      },
      {
        "actorName": "Justin Bartha"
      }
    ]
  },
  {
    "_id": "MV-tt0413300",
    "type": "movie",
    "title": "Spider-Man 3",
    "synopsis": "A strange black entity from another world bonds with Peter Parker and causes inner turmoil as he contends with new villains, temptations, and revenge.",
    "releaseYear": 2007,
    "genres": [
      "Action",
      "Adventure"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BODUwMDc5Mzc5M15BMl5BanBnXkFtZTcwNDgzOTY0MQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8340,
    "director": "Sam Raimi",
    "cast": [
      {
        "actorName": "Tobey Maguire"
      },
      {
        "actorName": "Kirsten Dunst"
      },
      {
        "actorName": "James Franco"
      },
      {
        "actorName": "Thomas Haden Church"
      }
    ]
  },
  {
    "_id": "MV-tt0469494",
    "type": "movie",
    "title": "There Will Be Blood",
    "synopsis": "A story of family, religion, hatred, oil and madness, focusing on a turn-of-the-century prospector in the early days of the business.",
    "releaseYear": 2007,
    "genres": [
      "Drama"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjA0NjE1ODEyNV5BMl5BanBnXkFtZTcwNDIzMzE5NQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 9480,
    "director": "Paul Thomas Anderson",
    "cast": [
      {
        "actorName": "Daniel Day-Lewis"
      },
      {
        "actorName": "Martin Stringer"
      },
      {
        "actorName": "Matthew Braden Stringer"
      },
      {
        "actorName": "Jacob Stringer"
      }
    ]
  },
  {
    "_id": "MV-tt0319061",
    "type": "movie",
    "title": "Big Fish",
    "synopsis": "A son tries to learn more about his dying father by reliving stories and myths he told about his life.",
    "releaseYear": 2003,
    "genres": [
      "Adventure",
      "Drama",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTYyMzM3MzgyNV5BMl5BanBnXkFtZTcwMTI4MzUyMQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7500,
    "director": "Tim Burton",
    "cast": [
      {
        "actorName": "Ewan McGregor"
      },
      {
        "actorName": "Albert Finney"
      },
      {
        "actorName": "Billy Crudup"
      },
      {
        "actorName": "Jessica Lange"
      }
    ]
  },
  {
    "_id": "MV-tt0449059",
    "type": "movie",
    "title": "Little Miss Sunshine",
    "synopsis": "A family determined to get their young daughter into the finals of a beauty pageant take a cross-country trip in their VW bus.",
    "releaseYear": 2006,
    "genres": [
      "Comedy",
      "Drama"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTgzNTgzODU0NV5BMl5BanBnXkFtZTcwMjEyMjMzMQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6060,
    "director": "Jonathan Dayton, Valerie Faris",
    "cast": [
      {
        "actorName": "Abigail Breslin"
      },
      {
        "actorName": "Greg Kinnear"
      },
      {
        "actorName": "Paul Dano"
      },
      {
        "actorName": "Alan Arkin"
      }
    ]
  },
  {
    "_id": "MV-tt0369610",
    "type": "movie",
    "title": "Jurassic World",
    "synopsis": "A new theme park is built on the original site of Jurassic Park. Everything is going well until the park's newest attraction--a genetically modified giant stealth killing machine--escapes containment and goes on a killing spree.",
    "releaseYear": 2015,
    "genres": [
      "Action",
      "Adventure",
      "Sci-Fi"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTQ5MTE0MTk3Nl5BMl5BanBnXkFtZTgwMjczMzk2NTE@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7440,
    "director": "Colin Trevorrow",
    "cast": [
      {
        "actorName": "Chris Pratt"
      },
      {
        "actorName": "Bryce Dallas Howard"
      },
      {
        "actorName": "Irrfan Khan"
      },
      {
        "actorName": "Vincent D'Onofrio"
      }
    ]
  },
  {
    "_id": "MV-tt0356910",
    "type": "movie",
    "title": "Mr. & Mrs. Smith",
    "synopsis": "A bored married couple is surprised to learn that they are both assassins hired by competing agencies to kill each other.",
    "releaseYear": 2005,
    "genres": [
      "Action",
      "Comedy",
      "Crime"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTUxMzcxNzQzOF5BMl5BanBnXkFtZTcwMzQxNjUyMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7200,
    "director": "Doug Liman",
    "cast": [
      {
        "actorName": "Brad Pitt"
      },
      {
        "actorName": "Angelina Jolie"
      },
      {
        "actorName": "Vince Vaughn"
      },
      {
        "actorName": "Adam Brody"
      }
    ]
  },
  {
    "_id": "MV-tt0057012",
    "type": "movie",
    "title": "Dr. Strangelove or: How I Learned to Stop Worrying and Love the Bomb",
    "synopsis": "An insane general triggers a path to nuclear holocaust that a war room full of politicians and generals frantically try to stop.",
    "releaseYear": 1964,
    "genres": [
      "Comedy"
    ],
    "countries": [
      "USA",
      "UK"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTU2ODM2NTkxNF5BMl5BanBnXkFtZTcwOTMwMzU3Mg@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 5700,
    "director": "Stanley Kubrick",
    "cast": [
      {
        "actorName": "Peter Sellers"
      },
      {
        "actorName": "George C. Scott"
      },
      {
        "actorName": "Sterling Hayden"
      },
      {
        "actorName": "Keenan Wynn"
      }
    ]
  },
  {
    "_id": "MV-tt0319262",
    "type": "movie",
    "title": "The Day After Tomorrow",
    "synopsis": "Jack Hall, paleoclimatologist, must make a daring trek across America to reach his son, trapped in the cross-hairs of a sudden international storm which plunges the planet into a new Ice Age.",
    "releaseYear": 2004,
    "genres": [
      "Action",
      "Adventure",
      "Sci-Fi"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 12,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTU1NTA3NzMwOV5BMl5BanBnXkFtZTcwNzEzMTEzMw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 7440,
    "director": "Roland Emmerich",
    "cast": [
      {
        "actorName": "Dennis Quaid"
      },
      {
        "actorName": "Jake Gyllenhaal"
      },
      {
        "actorName": "Emmy Rossum"
      },
      {
        "actorName": "Dash Mihok"
      }
    ]
  },
  {
    "_id": "MV-tt1568346",
    "type": "movie",
    "title": "The Girl with the Dragon Tattoo",
    "synopsis": "Journalist Mikael Blomkvist is aided in his search for a woman who has been missing for forty years by Lisbeth Salander, a young computer hacker.",
    "releaseYear": 2011,
    "genres": [
      "Crime",
      "Drama",
      "Mystery"
    ],
    "countries": [
      "USA",
      "Sweden",
      "Norway"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTczNDk4NTQ0OV5BMl5BanBnXkFtZTcwNDAxMDgxNw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 9480,
    "director": "David Fincher",
    "cast": [
      {
        "actorName": "Daniel Craig"
      },
      {
        "actorName": "Rooney Mara"
      },
      {
        "actorName": "Christopher Plummer"
      },
      {
        "actorName": "Stellan Skarsgård"
      }
    ]
  },
  {
    "_id": "MV-tt0335266",
    "type": "movie",
    "title": "Lost in Translation",
    "synopsis": "A faded movie star and a neglected young woman form an unlikely bond after crossing paths in Tokyo.",
    "releaseYear": 2003,
    "genres": [
      "Drama"
    ],
    "countries": [
      "USA",
      "Japan"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTI2NDI5ODk4N15BMl5BanBnXkFtZTYwMTI3NTE3._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6060,
    "director": "Sofia Coppola",
    "cast": [
      {
        "actorName": "Scarlett Johansson"
      },
      {
        "actorName": "Bill Murray"
      },
      {
        "actorName": "Akiko Takeshita"
      },
      {
        "actorName": "Kazuyoshi Minamimagoe"
      }
    ]
  },
  {
    "_id": "MV-tt0314331",
    "type": "movie",
    "title": "Love Actually",
    "synopsis": "Follows the lives of eight very different couples in dealing with their love lives in various loosely interrelated tales all set during a frantic month before Christmas in London, England.",
    "releaseYear": 2003,
    "genres": [
      "Comedy",
      "Drama",
      "Romance"
    ],
    "countries": [
      "UK",
      "USA",
      "France"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTY4NjQ5NDc0Nl5BMl5BanBnXkFtZTYwNjk5NDM3._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8100,
    "director": "Richard Curtis",
    "cast": [
      {
        "actorName": "Bill Nighy"
      },
      {
        "actorName": "Gregor Fisher"
      },
      {
        "actorName": "Rory MacGregor"
      },
      {
        "actorName": "Colin Firth"
      }
    ]
  },
  {
    "_id": "MV-tt0032138",
    "type": "movie",
    "title": "The Wizard of Oz",
    "synopsis": "Dorothy Gale is swept away to a magical land in a tornado and embarks on a quest to see the Wizard who can help her return home.",
    "releaseYear": 1939,
    "genres": [
      "Adventure",
      "Family",
      "Fantasy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTU0MTA2OTIwNF5BMl5BanBnXkFtZTcwMzA0Njk3OA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 6120,
    "director": "Victor Fleming, George Cukor, Mervyn LeRoy, Norman Taurog, King Vidor",
    "cast": [
      {
        "actorName": "Judy Garland"
      },
      {
        "actorName": "Frank Morgan"
      },
      {
        "actorName": "Ray Bolger"
      },
      {
        "actorName": "Bert Lahr"
      }
    ]
  },
  {
    "_id": "MV-tt0497116",
    "type": "documentary",
    "title": "An Inconvenient Truth",
    "synopsis": "A documentary on Al Gore's campaign to make the issue of global warming a recognized problem worldwide.",
    "releaseYear": 2006,
    "genres": [
      "Documentary"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 7,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BOTg3NjYxMjM5OF5BMl5BanBnXkFtZTcwMzQzMDA0MQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 5760,
    "director": "Davis Guggenheim",
    "topics": [
      "Documentary"
    ]
  },
  {
    "_id": "MV-tt2130321",
    "type": "documentary",
    "title": "West of Memphis",
    "synopsis": "An examination of a failure of justice in the case against the West Memphis Three.",
    "releaseYear": 2012,
    "genres": [
      "Documentary"
    ],
    "countries": [
      "New Zealand",
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjIzNDM3NjkzOV5BMl5BanBnXkFtZTcwNjI5Nzg0OA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 8820,
    "director": "Amy Berg",
    "topics": [
      "Documentary"
    ]
  },
  {
    "_id": "MV-tt1190722",
    "type": "documentary",
    "title": "Louis C.K.: Chewed Up",
    "synopsis": "Louis C.K.'s second one-hour special filmed at the Berklee Performance Center in Boston, MA.",
    "releaseYear": 2008,
    "genres": [
      "Documentary",
      "Comedy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 15,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTU0NjA0ODk1Ml5BMl5BanBnXkFtZTcwNTU4OTAwMg@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 3660,
    "director": "Louis C.K., Shannon Hartman",
    "topics": [
      "Documentary",
      "Comedy"
    ]
  },
  {
    "_id": "MV-tt1421373",
    "type": "documentary",
    "title": "Louis C.K.: Hilarious",
    "synopsis": "In this unique and dynamic live concert experience, Louis C.K.'s exploration of life after 40 destroys politically correct images of modern life with thoughts we have all had...but would rarely admit to.",
    "releaseYear": 2010,
    "genres": [
      "Documentary",
      "Comedy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 15,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMTY1MzQ1MTM5OV5BMl5BanBnXkFtZTcwMTg0MTQzNA@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 4920,
    "director": "Louis C.K.",
    "topics": [
      "Documentary",
      "Comedy"
    ]
  },
  {
    "_id": "MV-tt2112999",
    "type": "documentary",
    "title": "Louis C.K.: Live at the Beacon Theater",
    "synopsis": "Louis jokes about fatherhood, success, and flying first class at the Beacon Theater in New York.",
    "releaseYear": 2011,
    "genres": [
      "Documentary",
      "Comedy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 15,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BNzIxMTUxMjYzMV5BMl5BanBnXkFtZTcwMDYwMjE0Nw@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 3780,
    "director": "Louis C.K.",
    "topics": [
      "Documentary",
      "Comedy"
    ]
  },
  {
    "_id": "MV-tt2510998",
    "type": "documentary",
    "title": "Louis C.K. Oh My God",
    "synopsis": "In February, 2013, Louis brings his impish nihilism to Phoenix, Arizona. He talks about an old lady and her pet, living in Manhattan, experiencing his body's aging (he's 45), men's ...",
    "releaseYear": 2013,
    "genres": [
      "Documentary",
      "Comedy"
    ],
    "countries": [
      "USA"
    ],
    "audioLanguages": [
      "en"
    ],
    "subtitleLanguages": [
      "es"
    ],
    "ageRating": 18,
    "coverUrl": "https://m.media-amazon.com/images/M/MV5BMjQ4NjQwOTYzNl5BMl5BanBnXkFtZTcwNjU2NjczOQ@@._V1_SX300.jpg",
    "status": "published",
    "durationSeconds": 3480,
    "director": "Louis C.K.",
    "topics": [
      "Documentary",
      "Comedy"
    ]
  }
];

  let inserted = 0;
  for (const item of rawDocs) {
    const doc = {
      ...item,
      releaseYear: Int32(item.releaseYear),
      ageRating: Int32(item.ageRating),
      durationSeconds: Int32(item.durationSeconds),
      createdAt: now,
      updatedAt: now
    };
    const { _id, ...fields } = doc;
    inserted += db.contents.updateOne({ _id }, { $setOnInsert: fields }, { upsert: true }).upsertedCount;
  }
  print("Peliculas nuevas del dataset insertadas: " + inserted + " (total en lote: " + rawDocs.length + ")");
}());
