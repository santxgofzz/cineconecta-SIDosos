# Decisiones de arquitectura (propuesta inicial)

- **Aplicación**: monolito modular Spring Boot y MVC con Thymeleaf.
- **Oracle**: autoridad para cuentas, perfiles, planes, suscripciones y pagos.
- **MongoDB**: `contents`, `my_list`, `viewing_sessions`, `viewing_progress`, `admin_users`.
- **Identificadores**: `profileId`/`subscriptionId` numéricos Oracle = BSON long;
  `contentId` y `episodeId` son strings estables en MongoDB.
- **Compra**: transacción solo Oracle; bloqueo de la cuenta para serializar
  contrataciones y cambios de perfiles críticos; denegar solapamientos.
- **Reproducción**: comprobar cuenta/perfil/suscripción en Oracle, publicación,
  edad y episodio en MongoDB; manejar fallas sin asumir atomicidad cruzada.
- **Historial**: sesiones separadas del progreso actual. Registrar `subscriptionId`
  histórico para atribuir consumo al plan de inicio, no al actual.
- **Seguridad**: contraseñas con BCrypt, usuarios admin independientes en Mongo;
  cada endpoint verifica propiedad del perfil y permisos del rol.

Pendiente pa revisión: reglas de las tablas relacionales auxiliares,
plan de fallos de dos almacenes, alcance de frontend y estrategia de seeds.
