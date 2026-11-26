-- Informe de ingresos y suscripciones agrupados por plan.
-- Parametros Oracle (TIMESTAMP):
--   :fecha_inicio  incluido
--   :fecha_fin     excluido
-- Ejemplo: inicio 2026-01-01 00:00:00, fin 2026-02-01 00:00:00.
-- Los ingresos se atribuyen por PAYMENTS.paidAt; las suscripciones, por
-- SUBSCRIPTIONS.startsAt. El monto reportado es el pago recibido, no PLANS.price.

WITH parametros AS (
    SELECT CAST(:fecha_inicio AS TIMESTAMP) AS fecha_inicio,
           CAST(:fecha_fin AS TIMESTAMP) AS fecha_fin
    FROM dual
),
suscripciones_periodo AS (
    SELECT s.PLANS_planId AS planId,
           COUNT(*) AS cantidad_suscripciones
    FROM SUBSCRIPTIONS s
    CROSS JOIN parametros p
    WHERE s.startsAt >= p.fecha_inicio
      AND s.startsAt <  p.fecha_fin
    GROUP BY s.PLANS_planId
),
ingresos_periodo AS (
    SELECT s.PLANS_planId AS planId,
           SUM(pg.amount) AS ingresos
    FROM PAYMENTS pg
    JOIN SUBSCRIPTIONS s
      ON s.subscriptionId = pg.SUBSCRIPTIONS_subscriptionId
    CROSS JOIN parametros p
    WHERE pg.paidAt >= p.fecha_inicio
      AND pg.paidAt <  p.fecha_fin
    GROUP BY s.PLANS_planId
)
SELECT pl.planId AS plan_id,
       pl.name AS nombre_plan,
       NVL(ip.ingresos, 0) AS ingresos,
       NVL(sp.cantidad_suscripciones, 0) AS cantidad_suscripciones
FROM PLANS pl
LEFT JOIN ingresos_periodo ip
  ON ip.planId = pl.planId
LEFT JOIN suscripciones_periodo sp
  ON sp.planId = pl.planId
ORDER BY pl.planId;
