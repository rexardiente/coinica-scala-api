// package models.repo

// import java.util.UUID
// import java.time.Instant
// import javax.inject.{ Inject, Singleton }
// import scala.concurrent.Future
// import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
// import models.domain.UserToken

// @Singleton
// class UserTokenRepo @Inject()(
//     dao: models.dao.UserAccountDAO,
//     protected val dbConfigProvider: DatabaseConfigProvider
//   ) extends HasDatabaseConfigProvider[utils.db.PostgresDriver] {
//   import profile.api._

//   def add(user: UserToken): Future[Int] =
//     db.run(dao.UserTokenQuery += user)
//   def delete(id: UUID): Future[Int] =
//     db.run(dao.UserTokenQuery(id).delete)
//   def update(user: UserToken): Future[Int] =
//     db.run(dao.UserTokenQuery.filter(_.id === user.id).update(user))
//   def getByID(id: UUID): Future[Option[UserToken]] =
//     db.run(dao.UserTokenQuery(id).result.headOption)
//   def exists(id: UUID): Future[Boolean] =
//     db.run(dao.UserTokenQuery(id).exists.result)
//   def hasValidLoginToken(id: UUID, token: String, limit: Long): Future[Boolean] =
//     db.run(dao.UserTokenQuery.filter(x => x.id === id && x.token === token && x.login >= limit).exists.result)
//   def hasValidEmailToken(id: UUID, token: String, limit: Long): Future[Boolean] =
//     db.run(dao.UserTokenQuery.filter(x => x.id === id && x.token === token && x.emailLimit >= limit).exists.result)
//   def hasValidPasswordToken(id: UUID, token: String, limit: Long): Future[Boolean] =
//     db.run(dao.UserTokenQuery.filter(x => x.id === id && x.token === token && x.passwordLimit >= limit).exists.result)
//   def hasSession(id: UUID): Future[Boolean] =
//     db.run(dao.UserTokenQuery.filter(x => x.id === id && x.token.isEmpty.?).exists.result)
//   // def hasValidSession(id: UUID, token: String, currentTime: Long): Future[Boolean] =
//   //   db.run(dao.UserTokenQuery.filter(x =>
//   //       x.id === id &&
//   //       x.token === token &&
//   //       x.tokenLimit >= currentTime
//   //     ).exists.result)
//   // and make sure token is valid by checking if not expired..
//   def getLoginByIDAndToken(id: UUID, token: String): Future[Option[UserToken]] =
//     db.run(dao.UserTokenQuery.filter(x => x.id === id && x.token === token && x.login >= Instant.now.getEpochSecond).result.headOption)
//   // def getByNameAndSessionToken(username: String, token: String): Future[Option[UserAccount]] =
//   //   db.run(dao.UserTokenQuery.filter(x => x.username === username && x.token === token).result.headOption)
// }
