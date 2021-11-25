package utils.lib

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import models.domain.eosio.TreasureHuntGameData
import models.service.PlatformConfigService
import utils.SystemConfig.{ NODE_SERVER_URI, DEFAULT_HOST }

@Singleton
class TreasureHuntEOSIO @Inject()(config: PlatformConfigService)(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = NODE_SERVER_URI

  def treasureHuntAutoPlay(id: Int, username: String, sets: Seq[Int]): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/autoplay")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id, "username" -> username, "panelset" -> sets))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntOpenTile(id: Int, username: String, index: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/opentile")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id, "username" -> username, "index" -> index))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntSetEnemy(id: Int, username: String, count: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/setenemy")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id, "username" -> username, "enemy_count" -> count))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntSetDestination(id: Int, username: String, destination: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/destination")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id, "username" -> username, "destination" -> destination))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntSetGamePanel(id: Int, username: String): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/setpanel")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id, "username" -> username))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntQuit(id: Int, username: String): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/end")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id, "username" -> username))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntInitialize(gameID: Int, quantity: Int, destination: Int, enemy: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/initialize")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> gameID, "destination" -> destination, "enemy_count" -> enemy, "quantity" -> quantity))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntGetUserData(id: Int): Future[Option[TreasureHuntGameData]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/get_data")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "game_data").asOpt[TreasureHuntGameData]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntGameStart(id: Int, quantity: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/gamestart")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id, "quantity" -> quantity))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def treasureHuntWithdraw(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/treasurehunt/withdraw")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
}