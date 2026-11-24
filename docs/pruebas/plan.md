# Pruebas imprescindibles para la sustentación

- [ ] Registrar cliente y comprobar hash y correo único.
- [ ] Suscripción válida por 30 días; inicio inclusivo, fin exclusivo.
- [ ] Pago rechazado no crea ni pago confirmado ni suscripción.
- [ ] Error tras insertar parte de la compra: ROLLBACK de ambas tablas Oracle.
- [ ] Dos compras simultáneas para misma cuenta: solo una válida, sin solapamiento.
- [ ] Crear perfiles concurrentemente: no superar capacidad del plan.
- [ ] Perfil desactivado/inexistente/de otra cuenta: acceso prohibido.
- [ ] Catálogo publicado con filtros, paginación y edad máxima autorizada.
- [ ] Serie con temporadas/episodios; números duplicados rechazados.
- [ ] Mi lista no acepta la misma pareja (perfil, contenido) dos veces.
- [ ] Reproducción simulada: iniciar, pausar, avanzar, finalizar; saltos no cuentan.
- [ ] Completar al finalizar o al 90%; guardar posición por unidad/episodio.
- [ ] Repetir reproducción conserva cada sesión histórica.
- [ ] Contenido retirado deja de reproducirse y de aparecer en Continuar viendo.
- [ ] Informe de ingresos por plan con filtro de fechas y CSV.
- [ ] Informe de consumo por plan histórico y tipo con CSV; perfiles distintos.
- [ ] Simular caída Oracle y MongoDB sin acceso indebido ni éxito falso.
- [ ] Medir una consulta Oracle y una MongoDB antes/después de índices.
- [ ] Validar login admin Mongo independiente de cuentas cliente Oracle.
- [ ] Probar autorización desde servidor y no solo desde la interfaz.

Para evidencia: conservar comando, dataset/volumen, salida, explicación, fecha y
capturas o reportes en `docs/evidencias/`. No inventar resultados.
