package co.edu.icesi.cineconecta.persistence.mongo.repository;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingProgressDocument;

/** Fragmento personalizado del repositorio de progreso. */
public interface ViewingProgressStore {
    /**
     * Upsert por (profileId, unitKey). Es idempotente, así que puede
     * reintentarse sin duplicar documentos (además lo garantiza el índice único).
     */
    void upsert(ViewingProgressDocument progress);
}
