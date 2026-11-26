-- Reglas adicionales, secuencias e índices de unicidad para CineConecta en Oracle.
-- Ejecutar después de 01_schema.sql conectado al esquema dueño de las tablas.

-- Secuencias para alta de cuentas y perfiles sin colisionar con IDs semilla (1, 2 / 15, 16, 21).
CREATE SEQUENCE SEQ_ACCOUNTS START WITH 100 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_PROFILES START WITH 100 INCREMENT BY 1 NOCACHE NOCYCLE;

-- Unicidad de correo insensible a mayúsculas/minúsculas.
CREATE UNIQUE INDEX ACCOUNTS_EMAIL_LOWER_UK ON ACCOUNTS (LOWER(email));

-- Unicidad de nombre de perfil dentro de cada cuenta (insensible a mayúsculas).
CREATE UNIQUE INDEX PROFILES_ACC_NAME_UK ON PROFILES (ACCOUNTS_accountId, LOWER(name));

-- Reglas de dominio para evitar contradicciones entre columnas redundantes del MR y los catálogos.
ALTER TABLE PROFILES
    ADD CONSTRAINT PROFILES_IS_ACTIVE_CK CHECK (isActive IN ('A', 'I'));

ALTER TABLE PROFILES
    ADD CONSTRAINT PROFILES_STATE_SYNC_CK CHECK (isActive = DM_PROFILE_STATES_stateCode);

ALTER TABLE PROFILES
    ADD CONSTRAINT PROFILES_AGE_SYNC_CK CHECK (maxAgeRating = DM_AGE_RATINGS_ageValue);

-- Reglas de integridad para planes, suscripciones y pagos.
ALTER TABLE PLANS
    ADD CONSTRAINT PLANS_PRICE_CK CHECK (price >= 0);

ALTER TABLE PLANS
    ADD CONSTRAINT PLANS_MAX_PROFILES_CK CHECK (maxProfiles >= 1);

ALTER TABLE SUBSCRIPTIONS
    ADD CONSTRAINT SUBSCRIPTIONS_WINDOW_CK CHECK (endsAt > startsAt);

ALTER TABLE PAYMENTS
    ADD CONSTRAINT PAYMENTS_AMOUNT_CK CHECK (amount >= 0);
