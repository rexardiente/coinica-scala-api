# --- !Ups

create table "USER" ("ID" UUID NOT NULL,"ACCOUNT" VARCHAR NOT NULL,"CREATED_AT" timestamp NOT NULL);

create table "GAME" ("ID" UUID NOT NULL,"NAME" VARCHAR NOT NULL,"DESCRIPTION" VARCHAR);



# --- !Downs

drop table "GAME";
drop table "USER";
