package utils.lib

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import models.service.PlatformConfigService
import models.domain.eosio._
import utils.SystemConfig.{ NODE_SERVER_URI, DEFAULT_HOST }
// https://github.com/DonutFactory/eosjs-node-server/blob/master/docs/GHOSTQUEST_V2_API.md
@Singleton
class GhostQuestEOSIO @Inject()(config: PlatformConfigService)(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = NODE_SERVER_URI
  def getUserData(id: Int): Future[Option[GhostQuestGameData]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/get_table")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "game_data").asOpt[GhostQuestGameData]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def getAllCharacters(): Future[Option[Seq[GhostQuestTableGameData]]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/query_table")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("options" -> JsNull))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data").asOpt[Seq[GhostQuestTableGameData]]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def initialize(id: Int, username: String): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/initialize")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "username" -> username))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def generateCharacter(id: Int, username: String, quantity: Int, limit: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/summon_ghost")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id,
                                    "username" -> username,
                                    "quantity" -> quantity,
                                    "limit" -> limit))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def addLife(id: Int, key: String): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/add_life")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "key" -> key))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def eliminate(id: Int, key: String): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/eliminate")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "key" -> key))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def withdraw(id: Int, key: String): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/withdraw")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "key" -> key))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def battleResult(gameid: String, winner: (String, Int), loser: (String, Int)): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/ghostquest/battle_result")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest
          .post(Json.obj(
            "gameid" -> gameid,
            "winner" -> Json.obj("first" -> winner._1, "second" -> winner._2),
            "loser" -> Json.obj("first" -> loser._1, "second" -> loser._2)
          ))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
}