package utils.lib

import javax.inject.{ Inject, Singleton }
import com.typesafe.config.{ Config, ConfigFactory}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import models.domain.eosio.TreasureHuntGameData

@Singleton
class TreasureHuntEOSIO @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
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
}