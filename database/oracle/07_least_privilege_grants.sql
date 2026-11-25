-- Privilegios mínimos para el usuario de aplicación Oracle (módulo cuentas y perfiles).
-- Ejecutar conectado como el dueño del esquema o DBA. Reemplazar CINECONECTA_APP
-- y CINECONECTA_OWNER según corresponda en el entorno del equipo/curso.
--
-- Nota:
-- - La aplicación de cuentas y perfiles no ejecuta DDL (CREATE/ALTER/DROP) ni DELETE.
-- - Las bajas lógicas de perfiles se realizan actualizando isActive / DM_PROFILE_STATES_stateCode a 'I'.
-- - El bloqueo pesimista (SELECT ... FOR UPDATE) sobre ACCOUNTS solo requiere SELECT/UPDATE.

-- CREATE USER CINECONECTA_APP IDENTIFIED BY "<definir_clave_por_entorno>";
GRANT CREATE SESSION TO CINECONECTA_APP;

-- Lectura de catálogos y suscripciones vigentes
GRANT SELECT ON CINECONECTA_OWNER.DM_AGE_RATINGS TO CINECONECTA_APP;
GRANT SELECT ON CINECONECTA_OWNER.DM_PROFILE_STATES TO CINECONECTA_APP;
GRANT SELECT ON CINECONECTA_OWNER.PLANS TO CINECONECTA_APP;
GRANT SELECT ON CINECONECTA_OWNER.SUBSCRIPTIONS TO CINECONECTA_APP;

-- Operación de cuentas y perfiles (sin DELETE)
GRANT SELECT, INSERT, UPDATE ON CINECONECTA_OWNER.ACCOUNTS TO CINECONECTA_APP;
GRANT SELECT, INSERT, UPDATE ON CINECONECTA_OWNER.PROFILES TO CINECONECTA_APP;

-- Secuencias para alta de cuentas y perfiles
GRANT SELECT ON CINECONECTA_OWNER.SEQ_ACCOUNTS TO CINECONECTA_APP;
GRANT SELECT ON CINECONECTA_OWNER.SEQ_PROFILES TO CINECONECTA_APP;
