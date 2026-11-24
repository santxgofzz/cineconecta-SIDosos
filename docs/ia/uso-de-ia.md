# Reflexión sobre el uso de IA (completar como equipo)

- Herramientas utilizadas y tareas en que ayudaron.
- Qué sugerencias se aceptaron, modificaron o rechazaron, y por qué.
- Qué verificamos ejecutando consultas, pruebas y revisiones manuales.
- Límites: código no probado, referencias Oracle ficticias, riesgos de seguridad.
- Qué decisiones puede explicar individualmente cada integrante.

Agregar ejemplos reales y responsabilidad de revisión. No declarar pruebas
que no se hayan realizado.

## Catálogo y administración

Codex asistió la implementación, la revisión de cambios y la ejecución de
pruebas de la rama de Johan. Contrastó el esquema y la proyección de
episodios con los ejemplos BSON y contratos de playback. Una prueba detectó
que la proyección interpretaba el ID de episodio como un ID documental,
por lo que se fijó el mapeo al campo BSON `id`. La verificación separó los
resultados de MongoDB real, los de MockMvc y los escenarios con Oracle
simulado. El recorrido completo en navegador con Oracle y las mediciones
comparativas quedan pendientes.

Las decisiones que Johan debe poder explicar están en el
[anexo del catálogo](../modelos/catalogo-mongodb.md). La revisión de
integración por el equipo sigue el flujo del repositorio.
