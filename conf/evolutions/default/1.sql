# --- !Ups

create table "USER" ("ID" UUID NOT NULL,"ACCOUNT" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GAME" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"IMG_URL" VARCHAR NOT NULL,"GENRE" UUID NOT NULL,"DESCRIPTION" VARCHAR);

create table "GENRE" ("ID" UUID NOT NULL PRIMARY KEY,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR);



# --- !Downs

drop table "GENRE";
drop table "GAME";
drop table "USER";
