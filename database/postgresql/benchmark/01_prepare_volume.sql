\set ON_ERROR_STOP on
\pset pager off
\timing on

DO $guard$
BEGIN
    IF current_database() <> 'cineconecta_benchmark' THEN
        RAISE EXCEPTION 'Conecta solo a cineconecta_benchmark; base actual: %', current_database();
    END IF;
END
$guard$;

-- Esta base y este esquema son exclusivos para el benchmark.
DROP SCHEMA IF EXISTS benchmark CASCADE;
CREATE SCHEMA benchmark;

CREATE TABLE benchmark.plans (
    plan_id INTEGER PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    price NUMERIC(12, 2) NOT NULL
);

CREATE TABLE benchmark.subscriptions (
    subscription_id BIGINT PRIMARY KEY,
    plan_id INTEGER NOT NULL REFERENCES benchmark.plans(plan_id),
    starts_at TIMESTAMP NOT NULL
);

CREATE TABLE benchmark.payments (
    payment_id BIGINT PRIMARY KEY,
    subscription_id BIGINT NOT NULL REFERENCES benchmark.subscriptions(subscription_id),
    paid_at TIMESTAMP NOT NULL,
    amount NUMERIC(12, 2) NOT NULL
);

INSERT INTO benchmark.plans (plan_id, name, price)
VALUES (1, 'Basico', 29900), (2, 'Familiar', 49900), (3, 'Premium', 69900),
       (4, 'Estudiante', 19900), (5, 'Duo', 39900), (6, 'Max', 89900),
       (7, 'Cineclub', 24900), (8, 'Anual', 299000);

-- Volumen relacional reproducible: 250 mil suscripciones y 500 mil pagos.
-- Los registros se insertan ordenados por fecha para que el índice temporal
-- pueda demostrar lectura por rango después de la medición secuencial.
INSERT INTO benchmark.subscriptions (subscription_id, plan_id, starts_at)
SELECT g::BIGINT,
       ((g * 13) % 8 + 1)::INTEGER,
       TIMESTAMP '2023-01-01 00:00:00'
           + (((g::BIGINT * 37) % 1461)::INTEGER * INTERVAL '1 day')
           + ((g % 86400)::INTEGER * INTERVAL '1 second')
FROM generate_series(1, 250000) AS g
ORDER BY 3, 1;

INSERT INTO benchmark.payments (payment_id, subscription_id, paid_at, amount)
SELECT ((s.subscription_id - 1) * 2 + p.payment_number)::BIGINT,
       s.subscription_id,
       s.starts_at + ((p.payment_number - 1) * INTERVAL '30 days'),
       plan.price
FROM benchmark.subscriptions AS s
JOIN benchmark.plans AS plan ON plan.plan_id = s.plan_id
CROSS JOIN generate_series(1, 2) AS p(payment_number)
ORDER BY 3, 1;

VACUUM (ANALYZE) benchmark.plans;
VACUUM (ANALYZE) benchmark.subscriptions;
VACUUM (ANALYZE) benchmark.payments;

\echo 'Volumen preparado (250k suscripciones / 500k pagos):'
SELECT 'plans' AS table_name, COUNT(*) AS rows FROM benchmark.plans
UNION ALL SELECT 'subscriptions', COUNT(*) FROM benchmark.subscriptions
UNION ALL SELECT 'payments', COUNT(*) FROM benchmark.payments;

SELECT current_setting('server_version') AS postgresql_version,
       pg_size_pretty(pg_database_size(current_database())) AS database_size;
