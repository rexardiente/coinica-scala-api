# --- !Ups

create table "ACCOUNT" ("ID" UUID NOT NULL PRIMARY KEY,"USERNAME" VARCHAR NOT NULL,"PASSWORD" VARCHAR,"CREATED_AT" BIGINT NOT NULL);

create table "USER_ACCOUNT" ("ID" UUID NOT NULL PRIMARY KEY,"USERNAME" VARCHAR NOT NULL,"PASSWORD" VARCHAR NOT NULL,"REFERRED_BY" VARCHAR,"REFERRAL_CODE" VARCHAR NOT NULL,"REFERRAL_AMOUNT" DOUBLE PRECISION NOT NULL,"REFERRAL_RATE" DOUBLE PRECISION NOT NULL,"WIN_RATE" DOUBLE PRECISION NOT NULL,"SESSION_TOKEN" VARCHAR,"SESSION_TOKEN_LIMIT" BIGINT,"LAST_SIGN_IN" timestamp NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GAME" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"IMG_URL" VARCHAR NOT NULL,"PATH" VARCHAR NOT NULL,"GENRE" UUID NOT NULL,"DESCRIPTION" VARCHAR);

create table "GENRE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR);

create table "TASK" ("ID" UUID NOT NULL PRIMARY KEY,"TASKS" VARCHAR NOT NULL,"CREATED_AT" BIGINT NOT NULL);

create table "TASK_TRACKER" ("USER" UUID NOT NULL,"GAME_ID" UUID NOT NULL,"RATIO" INTEGER NOT NULL);

create table "TASK_HISTORY" ("ID" UUID NOT NULL PRIMARY KEY,"TASKS_ID" UUID NOT NULL,"GAME_ID" UUID NOT NULL,"USER" UUID NOT NULL,"GAME_COUNT" INTEGER NOT NULL,"CREATED_AT" timestamp NOT NULL,"EXPIRED_AT" timestamp NOT NULL);

create table "EOS_NET_TRANSACTION" ("ID" UUID NOT NULL PRIMARY KEY,"TRACE_ID" VARCHAR NOT NULL,"BLOCK_NUM" BIGINT NOT NULL,"BLOCK_TIMESTAMP" BIGINT NOT NULL,"TRACE" VARCHAR NOT NULL);

create table "REFERRAL_HISTORY" ("ID" UUID NOT NULL PRIMARY KEY,"CODE" VARCHAR NOT NULL,"APPLIED_BY" UUID NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "RANKING_HISTORY" ("ID" UUID NOT NULL PRIMARY KEY,"PROFITS" VARCHAR NOT NULL,"PAYOUTS" VARCHAR NOT NULL,"WAGERED" VARCHAR NOT NULL,"MULTIPLIERS" VARCHAR NOT NULL,"CREATED_AT" BIGINT NOT NULL);

create table "CHALLENGE" ("ID" UUID NOT NULL PRIMARY KEY,"GAME_ID" UUID NOT NULL,"DESCRIPTION" VARCHAR NOT NULL,"CREATED_AT" BIGINT NOT NULL,"EXPIRE_AT" BIGINT NOT NULL);

create table "CHALLENGE_HISTORY" ("ID" UUID NOT NULL PRIMARY KEY,"USERS" VARCHAR NOT NULL,"CREATED_AT" BIGINT NOT NULL);

create table "CHALLENGE_TRACKER" ("USER" UUID NOT NULL PRIMARY KEY,"BETS" DOUBLE PRECISION NOT NULL,"WAGERED" DOUBLE PRECISION NOT NULL,"RATIO" DOUBLE PRECISION NOT NULL,"VIP_POINTS" DOUBLE PRECISION NOT NULL);

create table "GQ_CHARACTER_DATA" ("CHARACTER_ID" VARCHAR NOT NULL PRIMARY KEY,"PLAYER" UUID NOT NULL,"LIFE" INTEGER NOT NULL,"HP" INTEGER NOT NULL,"CLASS" INTEGER NOT NULL,"LEVEL" INTEGER NOT NULL,"STATUS" INTEGER NOT NULL,"ATTACK" INTEGER NOT NULL,"DEFENSE" INTEGER NOT NULL,"SPEED" INTEGER NOT NULL,"LUCK" INTEGER NOT NULL,"BATTLE_LIMIT" INTEGER NOT NULL,"BATTLE_COUNT" INTEGER NOT NULL,"IS_NEW" BOOLEAN NOT NULL,"CREATED_AT" BIGINT NOT NULL);

create table "GQ_CHARACTER_GAME_HISTORY" ("GAME_ID" VARCHAR NOT NULL PRIMARY KEY,"TX_HASH" VARCHAR NOT NULL,"PLAYER_1" UUID NOT NULL,"PLAYER_1_ID" VARCHAR NOT NULL,"PLAYER_2" UUID NOT NULL,"PLAYER_2_ID" VARCHAR NOT NULL,"GAME_LOG" VARCHAR NOT NULL,"TIME_EXECUTED" BIGINT NOT NULL);

create table "GQ_CHARACTER_DATA_HISTORY" ("CHARACTER_ID" VARCHAR NOT NULL PRIMARY KEY,"PLAYER" UUID NOT NULL,"LIFE" INTEGER NOT NULL,"HP" INTEGER NOT NULL,"CLASS" INTEGER NOT NULL,"LEVEL" INTEGER NOT NULL,"STATUS" INTEGER NOT NULL,"ATTACK" INTEGER NOT NULL,"DEFENSE" INTEGER NOT NULL,"SPEED" INTEGER NOT NULL,"LUCK" INTEGER NOT NULL,"BATTLE_LIMIT" INTEGER NOT NULL,"BATTLE_COUNT" INTEGER NOT NULL,"IS_NEW" BOOLEAN NOT NULL,"CREATED_AT" BIGINT NOT NULL);

create table "VIP" ("ID" UUID NOT NULL PRIMARY KEY,"RANK" VARCHAR NOT NULL,"NEXT_RANK" VARCHAR NOT NULL,"REFERRAL_COUNT" INTEGER NOT NULL,"PAYOUT" DOUBLE PRECISION NOT NULL,"POINTS" DOUBLE PRECISION NOT NULL,"UPDATED_AT" timestamp NOT NULL);

create table "VIP_BENEFITS" ("ID" VARCHAR NOT NULL PRIMARY KEY,"CASH_BACK" DOUBLE PRECISION NOT NULL,"REDEMPTION_RATE" DOUBLE PRECISION NOT NULL,"REFERRAL_RATE" DOUBLE PRECISION NOT NULL,"CLOSED_BETA" BOOLEAN NOT NULL,"CONCIERGE" BOOLEAN NOT NULL,"AMOUNT" INTEGER NOT NULL,"POINTS" INTEGER NOT NULL,"UPDATED_AT" timestamp NOT NULL);

create table "NEWS" ("ID" UUID NOT NULL PRIMARY KEY,"TITLE" VARCHAR NOT NULL,"SUB_TITLE" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR NOT NULL,"AUTHOR" VARCHAR NOT NULL,"URL" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "OVER_ALL_GAME_HISTORY" ("ID" UUID NOT NULL PRIMARY KEY,"TX_HASH" VARCHAR NOT NULL,"GAME_ID" VARCHAR NOT NULL,"GAME" VARCHAR NOT NULL,"TYPE" VARCHAR NOT NULL,"IS_CONFIRMED" BOOLEAN NOT NULL,"CREATED_AT" BIGINT NOT NULL);



# --- !Downs

drop table "OVER_ALL_GAME_HISTORY";
drop table "NEWS";
drop table "VIP_BENEFITS";
drop table "VIP";
drop table "GQ_CHARACTER_DATA_HISTORY";
drop table "GQ_CHARACTER_GAME_HISTORY";
drop table "GQ_CHARACTER_DATA";
drop table "CHALLENGE_TRACKER";
drop table "CHALLENGE_HISTORY";
drop table "CHALLENGE";
drop table "RANKING_HISTORY";
drop table "REFERRAL_HISTORY";
drop table "EOS_NET_TRANSACTION";
drop table "TASK_HISTORY";
drop table "TASK_TRACKER";
drop table "TASK";
drop table "GENRE";
drop table "GAME";
drop table "USER_ACCOUNT";
drop table "ACCOUNT";
