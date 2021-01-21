### EOS GAME STORE - API

# PostgreSQL DB command

## Creating user

> createuser eos-game-store-api

## Creating Database

> createdb eos-game-store-api

## PSQL login user

> psql eos-game-store-api

## Giving the user a password

> Alter user "eos-game-store-api" with encrypted password 'eos-game-store-api';

## Granting privileges on database

> Grant all privileges on database "eos-game-store-api" to "eos-game-store-api";

# Note: development branch before merging into the master branch.


Note: the WebSocket protocol does not implement Same Origin Policy, and so does not protect against Cross-Site WebSocket Hijacking. To secure a websocket against hijacking, the Origin header in the request must be checked against the server’s origin, and manual authentication (including CSRF tokens) should be implemented. If a WebSocket request does not pass the security checks, then acceptOrResult should reject the request by returning a Forbidden result.

subscribe WS
{
  "id": "user1",
  "input": {
  	"character_created": true
  }
}

{
"message": "connection_reset"
}

sbt run -Dplay.server.http.idleTimeout=180s