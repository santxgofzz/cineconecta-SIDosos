-- aca revisamos una cuenta de la demo sin cambiar datos.
-- usar f5 en sql developer con la misma cuenta que se uso para contratar.
SET PAGESIZE 100
SET LINESIZE 200
ACCEPT id_cuenta NUMBER PROMPT 'accountId de prueba: '

PROMPT ===== planes historicos y vigencia en utc =====
-- oracle guarda las fechas del modulo como timestamp sin zona en utc.
SELECT s.subscriptionId,
       p.name AS plan,
       s.startsAt,
       s.endsAt,
       CASE
           WHEN s.startsAt <= CAST(SYS_EXTRACT_UTC(SYSTIMESTAMP) AS TIMESTAMP)
            AND s.endsAt > CAST(SYS_EXTRACT_UTC(SYSTIMESTAMP) AS TIMESTAMP)
           THEN 'vigente'
           ELSE 'no vigente'
       END AS estado,
       pay.paymentId,
       pay.amount
  FROM SUBSCRIPTIONS s
  JOIN PLANS p ON p.planId = s.PLANS_planId
  LEFT JOIN PAYMENTS pay ON pay.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
 WHERE s.ACCOUNTS_accountId = &id_cuenta
 ORDER BY s.startsAt, s.subscriptionId;

PROMPT ===== cupos de cada plan frente a perfiles activos =====
-- esta consulta muestra datos para explicar el rechazo al bajar de plan.
SELECT p.planId,
       p.name,
       p.maxProfiles,
       (SELECT COUNT(*)
          FROM PROFILES pr
         WHERE pr.ACCOUNTS_accountId = &id_cuenta
           AND UPPER(TRIM(pr.isActive)) IN ('1', 'Y', 'S', 'T')) AS perfiles_activos
  FROM PLANS p
 ORDER BY p.planId;

PROMPT ===== suscripciones sin pago: debe ser cero =====
SELECT COUNT(*) AS suscripciones_sin_pago
  FROM SUBSCRIPTIONS s
 WHERE s.ACCOUNTS_accountId = &id_cuenta
   AND NOT EXISTS (
       SELECT 1 FROM PAYMENTS pay
        WHERE pay.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
   );

PROMPT ===== superposiciones de fechas: debe ser cero =====
SELECT COUNT(*) AS periodos_superpuestos
  FROM SUBSCRIPTIONS a
  JOIN SUBSCRIPTIONS b
    ON a.ACCOUNTS_accountId = b.ACCOUNTS_accountId
   AND a.subscriptionId < b.subscriptionId
   AND a.startsAt < b.endsAt
   AND b.startsAt < a.endsAt
 WHERE a.ACCOUNTS_accountId = &id_cuenta;

UNDEFINE id_cuenta
