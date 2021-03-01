# --- !Ups

create table "LOGIN" ("ID" UUID NOT NULL PRIMARY KEY,"USERNAME" VARCHAR NOT NULL,"PASSWORD" VARCHAR NOT NULL,"LOGINCREATED" BIGINT NOT NULL);

create table "USER" ("ID" UUID NOT NULL,"ACCOUNT" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GAME" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"IMG_URL" VARCHAR NOT NULL,"PATH" VARCHAR NOT NULL,"GENRE" UUID NOT NULL,"DESCRIPTION" VARCHAR);

create table "GENRE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR);

create table "TASK" ("ID" UUID NOT NULL PRIMARY KEY,"GAME_ID" UUID NOT NULL,"INFO" VARCHAR NOT NULL,"IS_VALID" BOOLEAN NOT NULL,"DATECREATED" BIGINT NOT NULL);

create table "TRANSACTION" ("ID" UUID NOT NULL PRIMARY KEY,"TRACE_ID" VARCHAR NOT NULL,"BLOCK_NUM" BIGINT NOT NULL,"BLOCK_TIMESTAMP" BIGINT NOT NULL,"TRACE" VARCHAR NOT NULL);

create table "REFERRAL" ("ID" UUID NOT NULL PRIMARY KEY,"REFFERALNAME" VARCHAR NOT NULL,"REFERRALLINK" VARCHAR NOT NULL,"RATE" DOUBLE PRECISION NOT NULL,"FEEAMOUNT" DOUBLE PRECISION NOT NULL,"REFERRALCREATED" BIGINT NOT NULL);

create table "RANKING" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"BETS" DOUBLE PRECISION NOT NULL,"PROFIT" DOUBLE PRECISION NOT NULL,"MULTIPLIERAMOUNT" DOUBLE PRECISION NOT NULL,"RANKINGCREATED" BIGINT NOT NULL);

create table "CHALLENGE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR NOT NULL,"START_AT" timestamp NOT NULL,"EXPIRE_AT" timestamp NOT NULL,"IS_AVAILABLE" BOOLEAN NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "CHALLENGE_HISTORY" ("ID" UUID NOT NULL PRIMARY KEY,"CHALLENGE_ID" UUID NOT NULL,"RANK" INTEGER NOT NULL,"NAME" VARCHAR NOT NULL,"BET_AMOUNT" DOUBLE PRECISION NOT NULL,"PROFIT" DOUBLE PRECISION NOT NULL,"RATIO" DOUBLE PRECISION NOT NULL,"VIP_POINTS" DOUBLE PRECISION NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GQ_CHARACTER_DATA" ("CHARACTER_ID" VARCHAR NOT NULL PRIMARY KEY,"PLAYER" VARCHAR NOT NULL,"LIFE" INTEGER NOT NULL,"HP" INTEGER NOT NULL,"CLASS" INTEGER NOT NULL,"LEVEL" INTEGER NOT NULL,"STATUS" INTEGER NOT NULL,"ATTACK" INTEGER NOT NULL,"DEFENSE" INTEGER NOT NULL,"SPEED" INTEGER NOT NULL,"LUCK" INTEGER NOT NULL,"PRIZE" DOUBLE PRECISION NOT NULL,"BATTLE_LIMIT" INTEGER NOT NULL,"BATTLE_COUNT" INTEGER NOT NULL,"LAST_MATCH" BIGINT NOT NULL,"CREATED_AT" BIGINT NOT NULL);

create table "GQ_CHARACTER_GAME_HISTORY" ("GAME_ID" VARCHAR NOT NULL PRIMARY KEY,"PLAYER_1" VARCHAR NOT NULL,"PLAYER_1_ID" VARCHAR NOT NULL,"PLAYER_2" VARCHAR NOT NULL,"PLAYER_2_ID" VARCHAR NOT NULL,"TIME_EXECUTED" BIGINT NOT NULL,"GAME_LOG" text [] NOT NULL,"STATUS" VARCHAR NOT NULL);

create table "GQ_CHARACTER_DATA_HISTORY" ("CHARACTER_ID" VARCHAR NOT NULL PRIMARY KEY,"PLAYER" VARCHAR NOT NULL,"LIFE" INTEGER NOT NULL,"HP" INTEGER NOT NULL,"CLASS" INTEGER NOT NULL,"LEVEL" INTEGER NOT NULL,"STATUS" INTEGER NOT NULL,"ATTACK" INTEGER NOT NULL,"DEFENSE" INTEGER NOT NULL,"SPEED" INTEGER NOT NULL,"LUCK" INTEGER NOT NULL,"PRIZE" DOUBLE PRECISION NOT NULL,"BATTLE_LIMIT" INTEGER NOT NULL,"BATTLE_COUNT" INTEGER NOT NULL,"LAST_MATCH" BIGINT NOT NULL,"CREATED_AT" BIGINT NOT NULL);

create table "ADMIN" ("ID" UUID NOT NULL PRIMARY KEY,"EMAIL" VARCHAR NOT NULL,"ROLE" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL,"HASHED_PASSWORD" VARCHAR NOT NULL,"VERIFICATION_INITIAL_TIME" timestamp,"RESET_PASSWORD_CODE" VARCHAR,"NEW_EMAIL" VARCHAR,"NEW_EMAIL_CODE" VARCHAR,"DISABLED_AT" timestamp);

create table "VIP" ("USER" VARCHAR NOT NULL PRIMARY KEY,"RANK" VARCHAR NOT NULL,"NEXT_RANK" VARCHAR NOT NULL,"PAYOUT" BIGINT NOT NULL,"POINTS" BIGINT NOT NULL,"NEXT_LEVEL" INTEGER NOT NULL,"UPDATED_AT" timestamp NOT NULL);

create table "VIP_BENEFITS" ("ID" VARCHAR NOT NULL PRIMARY KEY,"CASH_BACK" DOUBLE PRECISION NOT NULL,"REDEMPTION_RATE" DOUBLE PRECISION NOT NULL,"REFERRAL_RATE" DOUBLE PRECISION NOT NULL,"CLOSED_BETA" BOOLEAN NOT NULL,"CONCIERGE" BOOLEAN NOT NULL,"AMOUNT" INTEGER NOT NULL,"POINTS" INTEGER NOT NULL,"UPDATED_AT" timestamp NOT NULL);

create table "NEWS" ("ID" UUID NOT NULL PRIMARY KEY,"TITLE" VARCHAR NOT NULL,"SUB_TITLE" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR NOT NULL,"AUTHOR" VARCHAR NOT NULL,"IMAGES" text [] NOT NULL,"CREATED_AT" timestamp NOT NULL);



# --- !Downs

drop table "NEWS";
drop table "VIP_BENEFITS";
drop table "VIP";
drop table "ADMIN";
drop table "GQ_CHARACTER_DATA_HISTORY";
drop table "GQ_CHARACTER_GAME_HISTORY";
drop table "GQ_CHARACTER_DATA";
drop table "CHALLENGE_HISTORY";
drop table "CHALLENGE";
drop table "RANKING";
drop table "REFERRAL";
drop table "TRANSACTION";
drop table "TASK";
drop table "GENRE";
drop table "GAME";
drop table "USER";
drop table "LOGIN";
