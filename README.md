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


## SBT Play Docker
- sbt stage
- sbt docker:stage
- sbt docker:publishLocal
- docker run -p 9000:9000 eos-game-store-api:1.0-SNAPSHOT

Note: the WebSocket protocol does not implement Same Origin Policy, and so does not protect against Cross-Site WebSocket Hijacking. To secure a websocket against hijacking, the Origin header in the request must be checked against the server’s origin, and manual authentication (including CSRF tokens) should be implemented. If a WebSocket request does not pass the security checks, then acceptOrResult should reject the request by returning a Forbidden result.

// subscribe WS
{
	"id": "user1",
	"message": "subscribe"
}

// update character DB
{
  "id": "user1",
  "input": {
  	"character_created": true
  }
}

// reset WS connection
{
	"message": "connection_reset"
}

// VIP Requests
{
  "id": "user2",
  "input": {
  	"user": "user1",
  	"command": "vip",
  	"request": "" //  info, current_rank, payout, point, next_rank
  }
}

// Next Battle
{
  "id": "user1",
  "input": {
    "GQ_NEXT_BATTLE": "get"
  }
}

<!-- oes net transaction -->
{
  "id": "user1",
  "input": {
    "EOS_NET_TRANSACTION": "EOS_NET_TRANSACTION"
  }
}
<!-- TH game result -->
{
  "id": "user1",
  "input": {
    "tx_hash": "transaction hash",
    "game_id": "game id",
    "data": {
       "destination": 1,
      "enemy_count": 5,
      "maxprize": "3497.2672 EOS",
      "nextprize": "0.0000 EOS",
      "odds": "0.00000000000000000",
      "panel_set": [{"key": 1, "isopen": 1, "iswin": 1}, {"key": 1, "isopen": 1, "iswin": 1}],
      "prize": "0.0000 EOS",
      "status": 2,
      "unopentile": 0,
      "win_count": 4
    }
  }
}

sbt runProd -Dplay.server.http.idleTimeout=180s



GQ Smart Contract
  - move characters history to private chain
  - battle_action
    - Update character details

Server API
  - Scheduler
    - Battle on Standby Characters
      - Battle result will be save on memory(Sever API)

  - Validate Battle ID if exists in Smart Contract (notify every battle result)
    - If true, save to Game Tx History Table
      - Check if characters exceed battle limit
        - remove to smartcontract and save to Characters History DB
      - For character that loses the battle
        - Check if characters has no more HP
          - remove to smartcontract and save to Characters History DB
    - else discard Tx and remove to memory(Sever API)

  - Game Tx History Table
    - Game_ID
    - Player
    - IS_WIN
    - Character_ID
    - Battle Logs
    - Time executed

  - Notification from Chronicle Client server (Withdraw or Payment)
    - remove character if still exists in smartcontract
    - save to Characters History DB




Convert LocalDateTime to Instant using LocalDateTime.toInstant() method.
Instant instant = localDateTime.toInstant(ZoneOffset.UTC); 2. Convert Instant to LocalDateTime using LocalDateTime.ofInstant() method.
LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());

#### Scala Play Result Types
    [Types](https://alvinalexander.com/scala/play-framework-controller-action-results-list-types-ok/)
#### Scala Play with React

    [react](https://blog.usejournal.com/react-with-play-framework-2-6-x-a6e15c0b7bd)
    [angular](https://torre.me.uk/2019/03/06/scala-play-rest-and-angular/)

#### Play Filter token
    https://stackoverflow.com/questions/26675615/token-based-authentication-in-play-filter-passing-objects-along