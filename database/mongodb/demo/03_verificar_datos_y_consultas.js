// Verificaciones de lectura (no escribe ni elimina datos)
(function () {
  const demo = db.getSiblingDB("cineconecta_modelo_demo");
  print("--- COLECCIONES Y DOCUMENTOS ---");
  for (const name of ["contents", "my_list", "viewing_sessions", "viewing_progress", "admin_users"]) {
    print(name + ": " + demo.getCollection(name).countDocuments({}));
  }
  print("--- TITULOS PUBLICADOS PARA PERFIL CON EDAD MAXIMA 12 (sin validar Oracle) ---");
  printjson(demo.contents.find({status:"published", ageRating:{$lte:NumberInt(12)}}, {_id:1,title:1,type:1}).toArray());
  print("--- DETALLE DE SERIE CON EPISODIOS ---");
  printjson(demo.contents.findOne({_id:"CNT-002"}, {title:1,seasons:1}));
  print("--- MI LISTA PERFIL 15 ---");
  printjson(demo.my_list.find({profileId:NumberLong("15")}, {contentId:1,addedAt:1}).toArray());
  print("--- CONTINUAR VIENDO PERFIL 15 SIN RETIRADOS (filtro simulado con consulta a contents) ---");
  const pending = demo.viewing_progress.find({profileId:NumberLong("15"),completed:false}).sort({lastActivityAt:-1}).toArray();
  for(const p of pending) {
    const title = demo.contents.findOne({_id:p.contentId,status:"published"});
    if(title) printjson({title:title.title,unitKey:p.unitKey,lastPositionSeconds:p.lastPositionSeconds});
  }
  print("--- SESIONES HISTORICAS EN DOS PLANES DEL MISMO PERFIL ---");
  printjson(demo.viewing_sessions.find({profileId:NumberLong("15")},{_id:1,subscriptionId:1,contentId:1,startedAt:1}).sort({startedAt:1}).toArray());
  print("--- INDICES DEL PROGRESO ---");
  printjson(demo.viewing_progress.getIndexes());
  print("IMPORTANTE: IDs Oracle y permisos NO son verificados por este script de prueba.");
}());
