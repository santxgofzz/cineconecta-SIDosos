package co.edu.icesi.cineconecta.common.security;

/**
 * Contrato que debe implementar el principal de un titular autenticado
 * (cuenta Oracle). La autenticación real de clientes (módulo account) debe
 * devolver un {@code UserDetails} que implemente esta interfaz.
 * Los administradores (MongoDB) NO la implementan.
 */
public interface AccountPrincipal {
    long getAccountId();
}
