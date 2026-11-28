-- Esquema Oracle según docs/modelos/modelo-relacional.pdf.
-- Ejecutar conectado al esquema dueño de estas tablas. Este archivo crea
-- estructura solamente: no agrega datos, secuencias ni reglas fuera del MR.

CREATE TABLE DM_AGE_RATINGS (
    ageValue INTEGER NOT NULL,
    label    VARCHAR2(60) NOT NULL,
    CONSTRAINT DM_AGE_RATINGS_PK PRIMARY KEY (ageValue)
);

CREATE TABLE DM_PROFILE_STATES (
    stateCode   CHAR(1) NOT NULL,
    description VARCHAR2(60) NOT NULL,
    CONSTRAINT DM_PROFILE_STATES_PK PRIMARY KEY (stateCode)
);

CREATE TABLE ACCOUNTS (
    accountId    INTEGER NOT NULL,
    fullName     VARCHAR2(150) NOT NULL,
    email        VARCHAR2(254) NOT NULL,
    passwordHash VARCHAR2(255) NOT NULL,
    createdAt    TIMESTAMP NOT NULL,
    CONSTRAINT ACCOUNTS_PK PRIMARY KEY (accountId),
    CONSTRAINT ACCOUNTS_EMAIL_UK UNIQUE (email)
);

CREATE TABLE PLANS (
    planId      INTEGER NOT NULL,
    price       NUMBER(12, 2) NOT NULL,
    maxProfiles INTEGER NOT NULL,
    name        VARCHAR2(60) NOT NULL,
    CONSTRAINT PLANS_PK PRIMARY KEY (planId),
    CONSTRAINT PLANS_NAME_UK UNIQUE (name)
);

CREATE TABLE SUBSCRIPTIONS (
    subscriptionId INTEGER NOT NULL,
    startsAt       TIMESTAMP NOT NULL,
    endsAt         TIMESTAMP NOT NULL,
    ACCOUNTS_accountId INTEGER NOT NULL,
    PLANS_planId       INTEGER NOT NULL,
    CONSTRAINT SUBSCRIPTIONS_PK PRIMARY KEY (subscriptionId),
    CONSTRAINT SUBS_ACCOUNTS_FK FOREIGN KEY (ACCOUNTS_accountId)
        REFERENCES ACCOUNTS (accountId),
    CONSTRAINT SUBS_PLANS_FK FOREIGN KEY (PLANS_planId)
        REFERENCES PLANS (planId)
);

CREATE TABLE PROFILES (
    profileId          INTEGER NOT NULL,
    name               VARCHAR2(60) NOT NULL,
    maxAgeRating       INTEGER NOT NULL,
    isActive           CHAR(1) NOT NULL,
    ACCOUNTS_accountId INTEGER NOT NULL,
    CONSTRAINT PROFILES_PK PRIMARY KEY (profileId),
    CONSTRAINT PROFILES_ACCOUNTS_FK FOREIGN KEY (ACCOUNTS_accountId)
        REFERENCES ACCOUNTS (accountId),
    CONSTRAINT PROFILES_AGE_RATINGS_FK FOREIGN KEY (maxAgeRating)
        REFERENCES DM_AGE_RATINGS (ageValue),
    CONSTRAINT PROFILES_STATES_FK FOREIGN KEY (isActive)
        REFERENCES DM_PROFILE_STATES (stateCode)
);

CREATE TABLE PAYMENTS (
    paymentId INTEGER NOT NULL,
    paidAt    TIMESTAMP NOT NULL,
    amount    NUMBER(12, 2) NOT NULL,
    reference VARCHAR2(1200) NOT NULL,
    SUBSCRIPTIONS_subscriptionId INTEGER NOT NULL,
    CONSTRAINT PAYMENTS_PK PRIMARY KEY (paymentId),
    CONSTRAINT PAYMENTS_SUBSCRIPTIONS_FK
        FOREIGN KEY (SUBSCRIPTIONS_subscriptionId)
        REFERENCES SUBSCRIPTIONS (subscriptionId)
);
