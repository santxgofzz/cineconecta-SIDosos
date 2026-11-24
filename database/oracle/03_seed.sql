-- Datos sinteticos para desarrollo y demostracion; no usar en produccion.
-- Ejecutar una sola vez despues de 01_schema.sql, en un esquema vacio/de prueba.
-- No borra ni actualiza datos. Si un ID ya existe, la carga falla para evitar
-- sobrescribir filas del esquema.
--
-- Los profileId 15/16/21 y subscriptionId 8/9/10 coinciden con las referencias
-- del fixture Mongo en database/mongodb/demo. Las fechas de suscripcion cubren
-- las sesiones de noviembre de 2026 del mismo fixture.
-- Hash BCrypt de la clave sintetica "SemillaLocal2026!"; solo para esta semilla.

WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT antes_de_semilla_cineconecta;

BEGIN
    INSERT INTO DM_AGE_RATINGS (ageValue, label) VALUES (0,  'Todo publico');
    INSERT INTO DM_AGE_RATINGS (ageValue, label) VALUES (7,  'Mayores de 7 anos');
    INSERT INTO DM_AGE_RATINGS (ageValue, label) VALUES (12, 'Mayores de 12 anos');
    INSERT INTO DM_AGE_RATINGS (ageValue, label) VALUES (15, 'Mayores de 15 anos');
    INSERT INTO DM_AGE_RATINGS (ageValue, label) VALUES (18, 'Mayores de 18 anos');

    INSERT INTO DM_PROFILE_STATES (stateCode, description) VALUES ('A', 'Activo');
    INSERT INTO DM_PROFILE_STATES (stateCode, description) VALUES ('I', 'Inactivo');

    INSERT INTO PLANS (planId, price, maxProfiles, name)
    VALUES (1, 29900, 1, 'Basico');
    INSERT INTO PLANS (planId, price, maxProfiles, name)
    VALUES (2, 49900, 3, 'Familiar');
    INSERT INTO PLANS (planId, price, maxProfiles, name)
    VALUES (3, 69900, 5, 'Premium');

    INSERT INTO ACCOUNTS (accountId, fullName, email, passwordHash, createdAt)
    VALUES (1, 'Cuenta Demo Familiar', 'familia@ejemplo.invalid',
            '$2a$10$N6x1FpG9O.z.1m2WdNsOHuvafSqXwrWftEnp2u9xulEVTyNECduRK',
            TIMESTAMP '2026-09-15 10:00:00');
    INSERT INTO ACCOUNTS (accountId, fullName, email, passwordHash, createdAt)
    VALUES (2, 'Cuenta Demo Individual', 'individual@ejemplo.invalid',
            '$2a$10$N6x1FpG9O.z.1m2WdNsOHuvafSqXwrWftEnp2u9xulEVTyNECduRK',
            TIMESTAMP '2026-10-15 10:00:00');

    INSERT INTO PROFILES (
        profileId, name, maxAgeRating, isActive, ACCOUNTS_accountId,
        DM_AGE_RATINGS_ageValue, DM_PROFILE_STATES_stateCode
    ) VALUES (15, 'Principal', 18, 'A', 1, 18, 'A');
    INSERT INTO PROFILES (
        profileId, name, maxAgeRating, isActive, ACCOUNTS_accountId,
        DM_AGE_RATINGS_ageValue, DM_PROFILE_STATES_stateCode
    ) VALUES (16, 'Ninos', 7, 'A', 1, 7, 'A');
    INSERT INTO PROFILES (
        profileId, name, maxAgeRating, isActive, ACCOUNTS_accountId,
        DM_AGE_RATINGS_ageValue, DM_PROFILE_STATES_stateCode
    ) VALUES (21, 'Principal', 15, 'A', 2, 15, 'A');

    -- Cuenta familiar: continuidad exacta entre periodos [inicio, fin).
    INSERT INTO SUBSCRIPTIONS (
        subscriptionId, startsAt, endsAt, ACCOUNTS_accountId, PLANS_planId
    ) VALUES (
        8, TIMESTAMP '2026-10-09 00:00:00', TIMESTAMP '2026-11-08 00:00:00', 1, 2
    );
    INSERT INTO SUBSCRIPTIONS (
        subscriptionId, startsAt, endsAt, ACCOUNTS_accountId, PLANS_planId
    ) VALUES (
        10, TIMESTAMP '2026-11-08 00:00:00', TIMESTAMP '2026-12-08 00:00:00', 1, 3
    );

    -- Cuenta individual; su suscripcion cubre las sesiones del perfil 21.
    INSERT INTO SUBSCRIPTIONS (
        subscriptionId, startsAt, endsAt, ACCOUNTS_accountId, PLANS_planId
    ) VALUES (
        9, TIMESTAMP '2026-11-01 00:00:00', TIMESTAMP '2026-12-01 00:00:00', 2, 1
    );

    INSERT INTO PAYMENTS (paymentId, paidAt, amount, reference, SUBSCRIPTIONS_subscriptionId)
    VALUES (1, TIMESTAMP '2026-10-09 00:00:00', 49900,
            'SEED-DEMO-SUB-8-20261009', 8);
    INSERT INTO PAYMENTS (paymentId, paidAt, amount, reference, SUBSCRIPTIONS_subscriptionId)
    VALUES (2, TIMESTAMP '2026-11-01 00:00:00', 29900,
            'SEED-DEMO-SUB-9-20261101', 9);
    INSERT INTO PAYMENTS (paymentId, paidAt, amount, reference, SUBSCRIPTIONS_subscriptionId)
    VALUES (3, TIMESTAMP '2026-11-08 00:00:00', 69900,
            'SEED-DEMO-SUB-10-20261108', 10);
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO antes_de_semilla_cineconecta;
        RAISE;
END;
/

COMMIT;

PROMPT Semilla Oracle cargada. Periodo 2026-10-01 a 2026-12-01: 3 suscripciones, $149700 de ingresos.
PROMPT Periodo 2026-11-01 a 2026-12-01: 2 suscripciones, $99800 de ingresos.
