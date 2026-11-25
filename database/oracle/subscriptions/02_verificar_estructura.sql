-- verificacion de la estructura de facturacion; no modifica registros.
-- si falta una regla o no esta habilitada, muestra error en vez de darla por buena.

SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    v_cantidad NUMBER;

    PROCEDURE existe_regla(p_tabla VARCHAR2, p_nombre VARCHAR2) IS
    BEGIN
        SELECT COUNT(*) INTO v_cantidad
          FROM user_constraints
         WHERE table_name = p_tabla
           AND constraint_name = p_nombre
           AND status = 'ENABLED'
           AND validated = 'VALIDATED';
        IF v_cantidad <> 1 THEN
            RAISE_APPLICATION_ERROR(-20101,
                'falta o no esta validada la regla ' || p_nombre || ' de ' || p_tabla);
        END IF;
    END existe_regla;

    PROCEDURE existe_fk(p_tabla VARCHAR2, p_columna VARCHAR2) IS
    BEGIN
        SELECT COUNT(*) INTO v_cantidad
          FROM user_constraints c
          JOIN user_cons_columns cc
            ON cc.constraint_name = c.constraint_name
           AND cc.table_name = c.table_name
         WHERE c.table_name = p_tabla
           AND c.constraint_type = 'R'
           AND c.status = 'ENABLED'
           AND cc.column_name = p_columna;
        IF v_cantidad = 0 THEN
            RAISE_APPLICATION_ERROR(-20102,
                'falta la llave foranea de ' || p_tabla || '.' || p_columna);
        END IF;
    END existe_fk;

    PROCEDURE existe_secuencia(p_nombre VARCHAR2) IS
    BEGIN
        SELECT COUNT(*) INTO v_cantidad
          FROM user_sequences
         WHERE sequence_name = p_nombre;
        IF v_cantidad <> 1 THEN
            RAISE_APPLICATION_ERROR(-20103, 'falta la secuencia ' || p_nombre);
        END IF;
    END existe_secuencia;
BEGIN
    existe_regla('PLANS', 'CK_CC_PLANS_PRICE');
    existe_regla('PLANS', 'CK_CC_PLANS_PROFILES');
    existe_regla('SUBSCRIPTIONS', 'CK_CC_SUBS_DATES');
    existe_regla('SUBSCRIPTIONS', 'CK_CC_SUBS_30_DAYS');
    existe_regla('PAYMENTS', 'CK_CC_PAY_AMOUNT');
    existe_regla('PAYMENTS', 'UK_CC_PAY_SUB');
    existe_regla('PAYMENTS', 'UK_CC_PAY_REFERENCE');

    existe_fk('SUBSCRIPTIONS', 'ACCOUNTS_ACCOUNTID');
    existe_fk('SUBSCRIPTIONS', 'PLANS_PLANID');
    existe_fk('PAYMENTS', 'SUBSCRIPTIONS_SUBSCRIPTIONID');

    existe_secuencia('CC_SUBSCRIPTIONS_SEQ');
    existe_secuencia('CC_PAYMENTS_SEQ');

    SELECT COUNT(*) INTO v_cantidad
      FROM subscriptions s
      JOIN subscriptions siguiente
        ON s.ACCOUNTS_accountId = siguiente.ACCOUNTS_accountId
       AND s.subscriptionId < siguiente.subscriptionId
       AND s.startsAt < siguiente.endsAt
       AND siguiente.startsAt < s.endsAt;
    IF v_cantidad > 0 THEN
        RAISE_APPLICATION_ERROR(-20104,
            'hay periodos de suscripcion superpuestos: ' || v_cantidad);
    END IF;

    SELECT COUNT(*) INTO v_cantidad
      FROM subscriptions s
     WHERE NOT EXISTS (
         SELECT 1 FROM payments p
          WHERE p.SUBSCRIPTIONS_subscriptionId = s.subscriptionId
     );
    IF v_cantidad > 0 THEN
        RAISE_APPLICATION_ERROR(-20105,
            'hay suscripciones sin pago registrado: ' || v_cantidad);
    END IF;

    DBMS_OUTPUT.PUT_LINE('ok: reglas, llaves, secuencias y datos de facturacion consistentes');
END;
/
