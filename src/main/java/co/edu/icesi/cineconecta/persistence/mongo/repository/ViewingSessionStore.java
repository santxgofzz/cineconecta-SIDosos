package co.edu.icesi.cineconecta.persistence.mongo.repository;

import co.edu.icesi.cineconecta.persistence.mongo.document.ViewingSessionDocument;

/** Fragmento personalizado del repositorio de sesiones. */
public interface ViewingSessionStore {
    /**
     * Reemplaza el estado mutable de la sesión solo si nadie la modificó desde
     * que se leyó con {@code expectedSeq} y sigue abierta. Evita que dos
     * eventos concurrentes o un reintento sumen dos veces el mismo tiempo.
     *
     * @return {@code true} si se aplicó la actualización.
     */
    boolean compareAndUpdate(ViewingSessionDocument updated, int expectedSeq);
}
