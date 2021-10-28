# ![](http://3.34.146.80:5000/static/media/referral.3c489497.png) project-name SERVER API
  Backend Server for [EGS WEB](https://github.com/DonutFactory/eos-web) and resposible for the ff.
- [x] HTTP and WS Security
- [x] Account Management
- [x] Accounts & Games History
- [x] Tx Event Listener
- [X] Referral and VIP Accounts
- [X] Daily and Monthly Tasks
- [X] Daily Challenge

##### Note: development branch before merging into the master branch.
### PostgreSQL DB command

#### Creating user
> createuser project-name

#### Creating Database
> createdb project-name

#### PSQL login user

> psql -U project-name

#### Giving the user a password

> Alter user "project-name" with encrypted password 'project-name';

#### Granting privileges on database

> Grant all privileges on database "project-name" to "project-name";

#### PSQL View Active Connections

> SELECT sum(numbackends) FROM pg_stat_database;

### ![how-to-increase-max-connections-in-postgresql](https://ubiq.co/database-blog/how-to-increase-max-connections-in-postgresql/)
### ![SBT](https://www.scala-sbt.org/assets/sbt-logo.svg) The Interactive Build Tool
  For Scala and Java - sbt is built for Scala and Java projects. It is the build tool of choice for 93.6% of the Scala developers (2019). One of the examples of Scala-specific feature is the ability to cross build your project against multiple Scala versions.

#### SBT Play Docker
- sbt stage
- sbt docker:stage
- sbt docker:publishLocal
- docker run -p 9000:9000 eos-game-s tore-api:1.0-SNAPSHOT

#### Run Production with HTTP Idle Timeout
sbt runProd 9000 -Dplay.server.http.idleTimeout=180s -Dconfig.file=conf/production.conf

#### Play 2.x - Prod Mode
Start in Prod mode: sbt "start -Dhttp.port=8080"

### Reminder:
> WebSocket protocol does not implement Same Origin Policy, and so does not protect against Cross-Site WebSocket Hijacking. To secure a websocket against hijacking, the Origin header in the request must be checked against the server’s origin, and manual authentication (including CSRF tokens) should be implemented. If a WebSocket request does not pass the security checks, then acceptOrResult should reject the request by returning a Forbidden result.
