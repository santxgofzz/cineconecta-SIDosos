package co.edu.icesi.cineconecta.integration;

/** Datos de un perfil leídos de Oracle (fuente de verdad). */
public record ProfileAccess(long profileId, long accountId, int maxAgeRating, boolean active) {}
