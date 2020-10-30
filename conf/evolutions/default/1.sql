# --- !Ups

create table "USER" ("ID" UUID NOT NULL,"ACCOUNT" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GAME" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"IMG_URL" VARCHAR NOT NULL,"PATH" VARCHAR NOT NULL,"GENRE" UUID NOT NULL,"DESCRIPTION" VARCHAR);

create table "GENRE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR);

create table "TASK" ("ID" UUID NOT NULL PRIMARY KEY,"GAME_ID" UUID NOT NULL,"INFO" VARCHAR NOT NULL,"IS_VALID" BOOLEAN NOT NULL,"DATE" timestamp NOT NULL);



# --- !Downs

drop table "TASK";
drop table "GENRE";
drop table "GAME";
drop table "USER";
