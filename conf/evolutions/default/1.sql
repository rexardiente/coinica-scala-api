# --- !Ups

create table "USER" ("ID" UUID NOT NULL,"ACCOUNT" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GAME" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"IMG_URL" VARCHAR NOT NULL,"PATH" VARCHAR NOT NULL,"GENRE" UUID NOT NULL,"DESCRIPTION" VARCHAR);

create table "GENRE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR);

create table "TASK" ("ID" UUID NOT NULL PRIMARY KEY,"GAME_ID" UUID NOT NULL,"INFO" VARCHAR NOT NULL,"IS_VALID" BOOLEAN NOT NULL,"DATECREATED" timestamp NOT NULL);

create table "TRANSACTION" ("ID" UUID NOT NULL PRIMARY KEY,"TX_ID" VARCHAR NOT NULL,"STATUS" VARCHAR NOT NULL,"CPU_USAGE_US" INTEGER NOT NULL,"NET_USAGE_WORDS" INTEGER NOT NULL,"ELAPSED" INTEGER NOT NULL,"NET_USAGE" INTEGER NOT NULL,"SCHEDULED" BOOLEAN NOT NULL,"ACT_TRACES" VARCHAR NOT NULL,"ACC_RAM_DELTA" VARCHAR,"EXCEPT" VARCHAR,"ERR_CODE" VARCHAR,"FAILED_DTRX_TRACE" VARCHAR NOT NULL,"PARTIAL" VARCHAR);



# --- !Downs

drop table "TRANSACTION";
drop table "TASK";
drop table "GENRE";
drop table "GAME";
drop table "USER";
