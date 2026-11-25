-- prueba aislada de facturacion, no depende de la semilla del equipo.
-- creamos una cuenta y un plan temporales para no tocar datos de los compañeros.
-- al terminar hacemos rollback, asi que no quedan cuentas, planes, pagos ni suscripciones.
-- las secuencias si avanzan aunque hagamos rollback, eso es normal en oracle.

SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    v_cuenta       accounts.accountId%TYPE;
    v_plan         plans.planId%TYPE;
    v_precio       plans.price%TYPE := 29900;
    v_sub_1        subscriptions.subscriptionId%TYPE;
    v_sub_2        subscriptions.subscriptionId%TYPE;
    v_sub_invalida subscriptions.subscriptionId%TYPE;
    v_pago_1       payments.paymentId%TYPE;
    v_pago_2       payments.paymentId%TYPE;
    v_referencia   payments.reference%TYPE;
    v_correo       accounts.email%TYPE;
    v_cantidad     NUMBER;
    v_rechazado    BOOLEAN;
BEGIN
    SAVEPOINT inicio_prueba_s1;

    -- usamos ids que no esten ocupados; al revertir no queda ningun registro de prueba.
    SELECT NVL(MAX(accountId), 0) + 1 INTO v_cuenta FROM accounts;
    SELECT NVL(MAX(planId), 0) + 1 INTO v_plan FROM plans;
    v_correo := 's1.' || LOWER(RAWTOHEX(SYS_GUID())) || '@example.invalid';

    INSERT INTO accounts (accountId, fullName, email, passwordHash, createdAt)
    VALUES (
        v_cuenta, 'Cuenta temporal S1', v_correo,
        '$2a$10$N6x1FpG9O.z.1m2WdNsOHuvafSqXwrWftEnp2u9xulEVTyNECduRK',
        SYSTIMESTAMP
    );

    INSERT INTO plans (planId, price, maxProfiles, name)
    VALUES (v_plan, v_precio, 1, 'Plan temporal S1 ' || TO_CHAR(v_plan));

    SELECT CC_SUBSCRIPTIONS_SEQ.NEXTVAL INTO v_sub_1 FROM dual;
    SELECT CC_SUBSCRIPTIONS_SEQ.NEXTVAL INTO v_sub_2 FROM dual;
    SELECT CC_SUBSCRIPTIONS_SEQ.NEXTVAL INTO v_sub_invalida FROM dual;
    SELECT CC_PAYMENTS_SEQ.NEXTVAL INTO v_pago_1 FROM dual;
    SELECT CC_PAYMENTS_SEQ.NEXTVAL INTO v_pago_2 FROM dual;

    -- una compra valida tiene una suscripcion de 30 dias y un pago asociado.
    INSERT INTO subscriptions (
        subscriptionId, startsAt, endsAt, ACCOUNTS_accountId, PLANS_planId
    ) VALUES (
        v_sub_1, TIMESTAMP '2099-01-01 00:00:00',
        TIMESTAMP '2099-01-31 00:00:00', v_cuenta, v_plan
    );

    v_referencia := 'S1-PRUEBA-' || TO_CHAR(v_pago_1);
    INSERT INTO payments (
        paymentId, paidAt, amount, reference, SUBSCRIPTIONS_subscriptionId
    ) VALUES (
        v_pago_1, TIMESTAMP '2099-01-01 00:00:00', v_precio,
        v_referencia, v_sub_1
    );

    SELECT COUNT(*) INTO v_cantidad
      FROM subscriptions s
      JOIN payments p ON p.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
     WHERE s.subscriptionId = v_sub_1;
    IF v_cantidad <> 1 THEN
        RAISE_APPLICATION_ERROR(-20202, 'la compra valida no quedo relacionada');
    END IF;

    -- un segundo pago para la misma suscripcion tiene que fallar por unicidad.
    v_rechazado := FALSE;
    BEGIN
        INSERT INTO payments (
            paymentId, paidAt, amount, reference, SUBSCRIPTIONS_subscriptionId
        ) VALUES (
            v_pago_2, TIMESTAMP '2099-01-02 00:00:00', v_precio,
            'S1-DUPLICADO-' || TO_CHAR(v_pago_2), v_sub_1
        );
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            v_rechazado := TRUE;
    END;
    IF NOT v_rechazado THEN
        RAISE_APPLICATION_ERROR(-20203, 'se aceptaron dos pagos para una suscripcion');
    END IF;

    -- 29 dias no cumple la duracion fija de una suscripcion.
    v_rechazado := FALSE;
    BEGIN
        INSERT INTO subscriptions (
            subscriptionId, startsAt, endsAt, ACCOUNTS_accountId, PLANS_planId
        ) VALUES (
            v_sub_invalida, TIMESTAMP '2099-06-01 00:00:00',
            TIMESTAMP '2099-06-30 00:00:00', v_cuenta, v_plan
        );
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -2290 THEN
                v_rechazado := TRUE;
            ELSE
                RAISE;
            END IF;
    END;
    IF NOT v_rechazado THEN
        RAISE_APPLICATION_ERROR(-20204, 'se acepto una suscripcion de 29 dias');
    END IF;

    -- una segunda suscripcion valida permite comprobar el rechazo de pagos negativos.
    INSERT INTO subscriptions (
        subscriptionId, startsAt, endsAt, ACCOUNTS_accountId, PLANS_planId
    ) VALUES (
        v_sub_2, TIMESTAMP '2099-04-01 00:00:00',
        TIMESTAMP '2099-05-01 00:00:00', v_cuenta, v_plan
    );

    v_rechazado := FALSE;
    BEGIN
        INSERT INTO payments (
            paymentId, paidAt, amount, reference, SUBSCRIPTIONS_subscriptionId
        ) VALUES (
            v_pago_2, TIMESTAMP '2099-04-01 00:00:00', -1,
            'S1-NEGATIVO-' || TO_CHAR(v_pago_2), v_sub_2
        );
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -2290 THEN
                v_rechazado := TRUE;
            ELSE
                RAISE;
            END IF;
    END;
    IF NOT v_rechazado THEN
        RAISE_APPLICATION_ERROR(-20205, 'se acepto un pago negativo');
    END IF;

    -- revertimos incluso los datos de apoyo, y verificamos que no quedaron filas.
    ROLLBACK TO inicio_prueba_s1;

    SELECT COUNT(*) INTO v_cantidad FROM accounts WHERE accountId = v_cuenta;
    IF v_cantidad <> 0 THEN
        RAISE_APPLICATION_ERROR(-20206, 'la cuenta temporal no se revirtio');
    END IF;
    SELECT COUNT(*) INTO v_cantidad FROM plans WHERE planId = v_plan;
    IF v_cantidad <> 0 THEN
        RAISE_APPLICATION_ERROR(-20207, 'el plan temporal no se revirtio');
    END IF;
    SELECT COUNT(*) INTO v_cantidad FROM subscriptions WHERE subscriptionId IN (v_sub_1, v_sub_2, v_sub_invalida);
    IF v_cantidad <> 0 THEN
        RAISE_APPLICATION_ERROR(-20208, 'las suscripciones temporales no se revirtieron');
    END IF;
    SELECT COUNT(*) INTO v_cantidad FROM payments WHERE paymentId IN (v_pago_1, v_pago_2);
    IF v_cantidad <> 0 THEN
        RAISE_APPLICATION_ERROR(-20209, 'los pagos temporales no se revirtieron');
    END IF;

    DBMS_OUTPUT.PUT_LINE('ok: compra valida y pago asociado');
    DBMS_OUTPUT.PUT_LINE('ok: segundo pago rechazado');
    DBMS_OUTPUT.PUT_LINE('ok: suscripcion de 29 dias rechazada');
    DBMS_OUTPUT.PUT_LINE('ok: pago negativo rechazado');
    DBMS_OUTPUT.PUT_LINE('ok: cuenta, plan, pagos y suscripciones temporales revertidos');
EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK TO inicio_prueba_s1;
        RAISE;
END;
/
