-- este script refuerza las tablas de planes, suscripciones y pagos del modelo del equipo.
-- no borra tablas ni datos, y se puede volver a ejecutar sin duplicar las reglas.
-- hay que correr primero el 01_schema.sql general si el esquema esta vacio.
-- ejecutar con f5 en sql developer, usando el esquema asignado al equipo.

SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

DECLARE
    v_faltantes VARCHAR2(200);

    PROCEDURE agregar_regla(
        p_tabla  IN VARCHAR2,
        p_nombre IN VARCHAR2,
        p_ddl    IN VARCHAR2
    ) IS
        v_total NUMBER;
        v_correctas NUMBER;
    BEGIN
        SELECT COUNT(*),
               SUM(CASE WHEN table_name = p_tabla THEN 1 ELSE 0 END)
          INTO v_total, v_correctas
          FROM user_constraints
         WHERE constraint_name = p_nombre;

        IF v_total = 0 THEN
            EXECUTE IMMEDIATE p_ddl;
            DBMS_OUTPUT.PUT_LINE('creada: ' || p_nombre);
        ELSIF v_correctas <> v_total THEN
            RAISE_APPLICATION_ERROR(-20011, 'el nombre ' || p_nombre || ' ya lo usa otra tabla');
        ELSE
            DBMS_OUTPUT.PUT_LINE('ya existia: ' || p_nombre);
        END IF;
    END agregar_regla;

    PROCEDURE crear_secuencia(
        p_nombre IN VARCHAR2,
        p_tabla  IN VARCHAR2,
        p_columna IN VARCHAR2
    ) IS
        v_existe NUMBER;
        v_siguiente NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_existe
          FROM user_sequences
         WHERE sequence_name = p_nombre;

        IF v_existe = 0 THEN
            EXECUTE IMMEDIATE
                'SELECT GREATEST(100000, NVL(MAX(' || p_columna || '), 0) + 1) FROM ' || p_tabla
                INTO v_siguiente;
            EXECUTE IMMEDIATE
                'CREATE SEQUENCE ' || p_nombre ||
                ' START WITH ' || TO_CHAR(v_siguiente) ||
                ' INCREMENT BY 1 NOCYCLE NOCACHE';
            DBMS_OUTPUT.PUT_LINE('creada: ' || p_nombre);
        ELSE
            DBMS_OUTPUT.PUT_LINE('ya existia: ' || p_nombre);
        END IF;
    END crear_secuencia;
BEGIN
    -- primero revisamos que la base general exista; este modulo no crea cuentas.
    SELECT LISTAGG(nombre, ', ') WITHIN GROUP (ORDER BY nombre)
      INTO v_faltantes
      FROM (
          SELECT 'ACCOUNTS' nombre FROM dual UNION ALL
          SELECT 'PLANS' FROM dual UNION ALL
          SELECT 'SUBSCRIPTIONS' FROM dual UNION ALL
          SELECT 'PAYMENTS' FROM dual
      ) tablas
     WHERE NOT EXISTS (
         SELECT 1 FROM user_tables t WHERE t.table_name = tablas.nombre
     );

    IF v_faltantes IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20010,
            'faltan tablas: ' || v_faltantes || '. ejecuta primero el esquema relacional general');
    END IF;

    -- estos checks cubren datos que una tabla debe rechazar por si sola.
    agregar_regla('PLANS', 'CK_CC_PLANS_PRICE',
        'ALTER TABLE PLANS ADD CONSTRAINT CK_CC_PLANS_PRICE CHECK (price >= 0)');
    agregar_regla('PLANS', 'CK_CC_PLANS_PROFILES',
        'ALTER TABLE PLANS ADD CONSTRAINT CK_CC_PLANS_PROFILES CHECK (maxProfiles >= 1)');
    agregar_regla('SUBSCRIPTIONS', 'CK_CC_SUBS_DATES',
        'ALTER TABLE SUBSCRIPTIONS ADD CONSTRAINT CK_CC_SUBS_DATES CHECK (endsAt > startsAt)');
    agregar_regla('SUBSCRIPTIONS', 'CK_CC_SUBS_30_DAYS',
        q'[ALTER TABLE SUBSCRIPTIONS ADD CONSTRAINT CK_CC_SUBS_30_DAYS CHECK (endsAt = startsAt + INTERVAL '30' DAY)]');
    agregar_regla('PAYMENTS', 'CK_CC_PAY_AMOUNT',
        'ALTER TABLE PAYMENTS ADD CONSTRAINT CK_CC_PAY_AMOUNT CHECK (amount >= 0)');

    -- la suscripcion puede tener como maximo un pago y la referencia no se reutiliza.
    -- el servicio de compra garantiza que el pago si exista al confirmar la compra.
    agregar_regla('PAYMENTS', 'UK_CC_PAY_SUB',
        'ALTER TABLE PAYMENTS ADD CONSTRAINT UK_CC_PAY_SUB UNIQUE (SUBSCRIPTIONS_subscriptionId)');
    agregar_regla('PAYMENTS', 'UK_CC_PAY_REFERENCE',
        'ALTER TABLE PAYMENTS ADD CONSTRAINT UK_CC_PAY_REFERENCE UNIQUE (reference)');

    -- estas secuencias reservan ids nuevos sin chocar con la semilla del equipo.
    crear_secuencia('CC_SUBSCRIPTIONS_SEQ', 'SUBSCRIPTIONS', 'subscriptionId');
    crear_secuencia('CC_PAYMENTS_SEQ', 'PAYMENTS', 'paymentId');

    DBMS_OUTPUT.PUT_LINE('reglas del modulo de facturacion listas');
END;
/
