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

-- Repetir el archivo restaura el escenario sin índices secundarios primero.
DROP INDEX IF EXISTS benchmark.ix_subscriptions_started_plan;
DROP INDEX IF EXISTS benchmark.ix_payments_paid_subscription;
ANALYZE benchmark.subscriptions;
ANALYZE benchmark.payments;

\echo ''
\echo 'ANTES: EXPLAIN ANALYZE, con solo índices de claves primarias.'
EXPLAIN (ANALYZE, BUFFERS, SETTINGS, SUMMARY, FORMAT TEXT)
WITH subscriptions_by_plan AS (
    SELECT s.plan_id, COUNT(*) AS subscription_count
    FROM benchmark.subscriptions AS s
    WHERE s.starts_at >= TIMESTAMP '2026-11-01 00:00:00'
      AND s.starts_at <  TIMESTAMP '2026-12-01 00:00:00'
    GROUP BY s.plan_id
), payments_by_plan AS (
    SELECT s.plan_id, SUM(p.amount) AS revenue
    FROM benchmark.payments AS p
    JOIN benchmark.subscriptions AS s ON s.subscription_id = p.subscription_id
    WHERE p.paid_at >= TIMESTAMP '2026-11-01 00:00:00'
      AND p.paid_at <  TIMESTAMP '2026-12-01 00:00:00'
    GROUP BY s.plan_id
)
SELECT plan.plan_id, plan.name,
       COALESCE(payments.revenue, 0) AS revenue,
       COALESCE(subscriptions.subscription_count, 0) AS subscription_count
FROM benchmark.plans AS plan
LEFT JOIN payments_by_plan AS payments ON payments.plan_id = plan.plan_id
LEFT JOIN subscriptions_by_plan AS subscriptions ON subscriptions.plan_id = plan.plan_id
ORDER BY plan.plan_id;

\echo ''
\echo 'ÍNDICES: rango de suscripciones; rango de pagos con importe cubierto.'
CREATE INDEX ix_subscriptions_started_plan
    ON benchmark.subscriptions (starts_at, plan_id);
CREATE INDEX ix_payments_paid_subscription
    ON benchmark.payments (paid_at, subscription_id) INCLUDE (amount);
VACUUM (ANALYZE) benchmark.subscriptions;
VACUUM (ANALYZE) benchmark.payments;

\echo ''
\echo 'DESPUÉS: mismo SQL y mismo periodo con índices secundarios.'
EXPLAIN (ANALYZE, BUFFERS, SETTINGS, SUMMARY, FORMAT TEXT)
WITH subscriptions_by_plan AS (
    SELECT s.plan_id, COUNT(*) AS subscription_count
    FROM benchmark.subscriptions AS s
    WHERE s.starts_at >= TIMESTAMP '2026-11-01 00:00:00'
      AND s.starts_at <  TIMESTAMP '2026-12-01 00:00:00'
    GROUP BY s.plan_id
), payments_by_plan AS (
    SELECT s.plan_id, SUM(p.amount) AS revenue
    FROM benchmark.payments AS p
    JOIN benchmark.subscriptions AS s ON s.subscription_id = p.subscription_id
    WHERE p.paid_at >= TIMESTAMP '2026-11-01 00:00:00'
      AND p.paid_at <  TIMESTAMP '2026-12-01 00:00:00'
    GROUP BY s.plan_id
)
SELECT plan.plan_id, plan.name,
       COALESCE(payments.revenue, 0) AS revenue,
       COALESCE(subscriptions.subscription_count, 0) AS subscription_count
FROM benchmark.plans AS plan
LEFT JOIN payments_by_plan AS payments ON payments.plan_id = plan.plan_id
LEFT JOIN subscriptions_by_plan AS subscriptions ON subscriptions.plan_id = plan.plan_id
ORDER BY plan.plan_id;

\echo ''
\echo 'Tamaño de tablas e índices después del benchmark:'
SELECT relname AS relation,
       pg_size_pretty(pg_total_relation_size(oid)) AS total_size
FROM pg_class
WHERE oid IN ('benchmark.plans'::regclass,
              'benchmark.subscriptions'::regclass,
              'benchmark.payments'::regclass)
ORDER BY relname;

SELECT indexrelname AS index_name,
       pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'benchmark'
ORDER BY indexrelname;
