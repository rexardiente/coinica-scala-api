# --- !Ups

create table "LOGIN" (USERNAME" VARCHAR NOT NULL,"PASSWORD" timestamp NOT NULL);

create table "USER" ("ID" UUID NOT NULL,"ACCOUNT" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GAME" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"IMG_URL" VARCHAR NOT NULL,"PATH" VARCHAR NOT NULL,"GENRE" UUID NOT NULL,"DESCRIPTION" VARCHAR);

create table "GENRE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR);

create table "TASK" ("ID" UUID NOT NULL PRIMARY KEY,"GAME_ID" UUID NOT NULL,"INFO" VARCHAR NOT NULL,"IS_VALID" BOOLEAN NOT NULL,"DATECREATED" BIGINT NOT NULL);

create table "TRANSACTION" ("ID" UUID NOT NULL PRIMARY KEY,"TRACE_ID" VARCHAR NOT NULL,"BLOCK_NUM" BIGINT NOT NULL,"BLOCK_TIMESTAMP" BIGINT NOT NULL,"TRACE" VARCHAR NOT NULL);

create table "REFERRAL" ("ID" UUID NOT NULL PRIMARY KEY,"REFFERALNAME" VARCHAR NOT NULL,"REFERRALLINK" VARCHAR NOT NULL,"RATE" DOUBLE PRECISION NOT NULL,"FEEAMOUNT" DOUBLE PRECISION NOT NULL,"REFERRALCREATED" BIGINT NOT NULL);

create table "LOGIN" ("ID" UUID NOT NULL PRIMARY KEY,"USERNAME" VARCHAR NOT NULL,"PASSWORD" VARCHAR NOT NULL);

create table "CHALLENGE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"BETS" DOUBLE PRECISION NOT NULL,"PROFIT" DOUBLE PRECISION NOT NULL,"RATIO" DOUBLE PRECISION NOT NULL,"VIPPOINTS" DOUBLE PRECISION NOT NULL,"CHALLENGECREATED" BIGINT NOT NULL);

create table "GQ_CHARACTER_DATA" ("CHARACTER_ID" VARCHAR NOT NULL PRIMARY KEY,"OWNER" VARCHAR NOT NULL,"LIFE" INTEGER NOT NULL,"INITIAL_HP" INTEGER NOT NULL,"CLASS" INTEGER NOT NULL,"LEVEL" INTEGER NOT NULL,"STATUS" INTEGER NOT NULL,"ATTACK" INTEGER NOT NULL,"DEFENSE" INTEGER NOT NULL,"SPEED" INTEGER NOT NULL,"LUCK" INTEGER NOT NULL,"PRIZE" VARCHAR NOT NULL,"BATTLE_LIMIT" INTEGER NOT NULL,"BATTLE_COUNT" INTEGER NOT NULL,"LAST_MATCH" BIGINT NOT NULL);

create table "GQ_CHARACTER_GAME_HISTORY" ("ID" UUID NOT NULL PRIMARY KEY,"GAME_ID" VARCHAR NOT NULL,"PLAYER" VARCHAR NOT NULL,"ENEMY" VARCHAR NOT NULL,"PLAYER_ID" VARCHAR NOT NULL,"ENEMY_ID" VARCHAR NOT NULL,"TIME_EXECUTED" BIGINT NOT NULL,"GAME_LOG" text [] NOT NULL,"IS_WIN" BOOLEAN NOT NULL);

create table "GQ_CHARACTER_DATA_HISTORY" ("CHARACTER_ID" VARCHAR NOT NULL PRIMARY KEY,"PLAYER" VARCHAR NOT NULL,"ENEMY" INTEGER NOT NULL,"HP" INTEGER NOT NULL,"CLASS" INTEGER NOT NULL,"LEVEL" INTEGER NOT NULL,"STATUS" INTEGER NOT NULL,"ATTACT" INTEGER NOT NULL,"DEFENSE" INTEGER NOT NULL,"SPEED" INTEGER NOT NULL,"LUCK" INTEGER NOT NULL,"PRIZE" VARCHAR NOT NULL,"BATTLE_LIMIT" INTEGER NOT NULL,"BATTLE_COUNT" INTEGER NOT NULL,"LAST_MATCH" BIGINT NOT NULL);



# --- !Downs

drop table "GQ_CHARACTER_DATA_HISTORY";
drop table "GQ_CHARACTER_GAME_HISTORY";
drop table "GQ_CHARACTER_DATA";
drop table "CHALLENGE";
drop table "LOGIN";
drop table "RANKING";
drop table "REFERRAL";
drop table "TRANSACTION";
drop table "TASK";
drop table "GENRE";
drop table "GAME";
drop table "USER";
