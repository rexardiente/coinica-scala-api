package utils.lib

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import com.typesafe.config.{ Config, ConfigFactory}
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import models.domain.eosio.GQ.v2._
import models.domain.eosio.TreasureHuntGameData
import models.domain.eosio.TableRowsRequest

@Singleton
class EOSIOHTTPSupport @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = utils.Config.NODE_SERVER_URI

  def treasureHuntAutoPlay(id: Int, username: String, sets: Seq[Int]): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/autoplay")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "username" -> username, "panelset" -> sets))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def treasureHuntOpenTile(id: Int, username: String, index: Int): Future[(Boolean, String)] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/opentile")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "username" -> username, "index" -> index))
      .map { v =>
        val transactionID: String = (v.json \ "data" \ "transaction_id").as[String]
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
          (true, transactionID)
        else (false, null)
      }.recover { case e: Exception => (false, null) }
  }
  def treasureHuntSetEnemy(id: Int, username: String, count: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/setenemy")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "username" -> username, "enemy_count" -> count))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def treasureHuntSetDestination(id: Int, username: String, destination: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/destination")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "username" -> username, "destination" -> destination))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def treasureHuntSetGamePanel(id: Int, username: String): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/setpanel")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "username" -> username))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def treasureHuntQuit(id: Int, username: String): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/end")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "username" -> username))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def treasureHuntInitialize(id: Int, username: String): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/initialize")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "username" -> username))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def treasureHuntGetUserData(id: Int): Future[Option[TreasureHuntGameData]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/get_data")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
          (v.json \ "data" \ "game_data").asOpt[TreasureHuntGameData]
        else None
      }.recover { case e: Exception => None }
  }
  def treasureHuntGameStart(id: Int, quantity: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/gamestart")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post(Json.obj("id" -> id, "quantity" -> quantity))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def treasureHuntWithdraw(id: Int): Future[(Boolean, String)] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/treasurehunt/withdraw")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post(Json.obj("id" -> id))
      .map { v =>
        val transactionID: String = (v.json \ "data" \ "transaction_id").as[String]
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
          (true, transactionID)
        else (false, null)
      }.recover { case e: Exception => (false, null) }
  }

  def getGhostQuestTableRows(req: TableRowsRequest, sender: Option[String]): Future[Option[GQRowsResponse]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/get_table")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post(Json.obj(
        "code" -> req.code,
        "table" -> req.table,
        "scope" -> req.scope,
        "index_position" -> req.index_position.getOrElse(null),
        "key_type" -> req.key_type.getOrElse(null),
        "encode_type" -> req.encode_type.getOrElse(null),
        "upper_bound" -> req.upper_bound.getOrElse(null),
        "lower_bound" -> req.lower_bound.getOrElse(null),
        "json" -> true, // add this to format result into JSON
        "limit" -> 10, // set max result to 250 active users per request
        "show_payer" -> false
      ))
      .map { response =>
        if ((response.json \ "code").as[Int] == 200) {
          val table: Option[GQRowsResponse] = (response.json \ "data" \ "table").asOpt[GQRowsResponse]
          table.map(_.copy(sender = sender))
        }
        else None
      }
      .recover { case e: Exception => None }
  }

  def ghostQuestBattleResult(gameid: String, winner: (String, String), loser: (String, String)): Future[Option[String]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/battle_result")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post(Json.obj(
        "gameid" -> gameid,
        "winner" -> Json.obj("first" -> winner._1, "second" -> winner._2),
        "loser" -> Json.obj("first" -> loser._1, "second" -> loser._2)
      ))
      .map(v => (v.json \ "data" \ "transaction").asOpt[String])
      .recover { case e: Exception => None }
  }

  def ghostQuestEliminate(user: String, characterID: String): Future[Option[String]] = {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/eliminate")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest
      .post(Json.obj(
        "username" -> user,
        "ghost_id" -> characterID
      ))
      .map(v => (v.json \ "data" \ "transaction").asOpt[String])
      .recover { case e: Exception => None }
  }
}