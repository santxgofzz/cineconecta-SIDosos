-- consultas de evidencia para la sustentacion de s3.
-- solo consultan informacion, no hacen cambios en oracle.
-- ingresa el accountId que usaste en la demo por api.
SET PAGESIZE 100
SET LINESIZE 200
ACCEPT id_cuenta NUMBER PROMPT 'accountId a verificar: '

PROMPT ===== suscripciones y pagos de esta cuenta =====
SELECT s.subscriptionId, s.ACCOUNTS_accountId, s.PLANS_planId,
       s.startsAt, s.endsAt, p.paymentId, p.amount, p.reference
  FROM SUBSCRIPTIONS s
  LEFT JOIN PAYMENTS p ON p.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
 WHERE s.ACCOUNTS_accountId = &id_cuenta
 ORDER BY s.startsAt, s.subscriptionId;

PROMPT ===== periodos superpuestos: debe dar 0 =====
SELECT COUNT(*) AS periodos_superpuestos
  FROM SUBSCRIPTIONS a
  JOIN SUBSCRIPTIONS b
    ON a.ACCOUNTS_accountId = b.ACCOUNTS_accountId
   AND a.subscriptionId < b.subscriptionId
   AND a.startsAt < b.endsAt
   AND b.startsAt < a.endsAt
 WHERE a.ACCOUNTS_accountId = &id_cuenta;

PROMPT ===== suscripciones sin pago: debe dar 0 =====
SELECT COUNT(*) AS suscripciones_sin_pago
  FROM SUBSCRIPTIONS s
 WHERE s.ACCOUNTS_accountId = &id_cuenta
   AND NOT EXISTS (
       SELECT 1 FROM PAYMENTS p
        WHERE p.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
   );

PROMPT ===== total de compras y pagos confirmados =====
SELECT (SELECT COUNT(*) FROM SUBSCRIPTIONS s
         WHERE s.ACCOUNTS_accountId = &id_cuenta) AS total_suscripciones,
       (SELECT COUNT(*) FROM PAYMENTS p
          JOIN SUBSCRIPTIONS s
            ON s.subscriptionId = p.SUBSCRIPTIONS_subscriptionId
         WHERE s.ACCOUNTS_accountId = &id_cuenta) AS total_pagos
  FROM DUAL;

UNDEFINE id_cuenta
