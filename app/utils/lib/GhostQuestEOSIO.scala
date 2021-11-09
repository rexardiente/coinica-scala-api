package utils.lib

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import com.typesafe.config.{ Config, ConfigFactory}
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import models.domain.eosio._
// https://github.com/DonutFactory/eosjs-node-server/blob/master/docs/GHOSTQUEST_V2_API.md
@Singleton
class GhostQuestEOSIO @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = utils.SystemConfig.NODE_SERVER_URI
  def getUserData(id: Int): Future[Option[GhostQuestGameData]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/get_table")
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
  def getAllCharacters(): Future[Option[Seq[GhostQuestTableGameData]]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/query_table")
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
  def initialize(id: Int, username: String): Future[Option[String]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/initialize")
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
  def generateCharacter(id: Int, username: String, quantity: Int, limit: Int): Future[Option[String]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/summon_ghost")
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
  def addLife(id: Int, key: String): Future[Option[String]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/add_life")
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
  def eliminate(id: Int, key: String): Future[Option[String]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/eliminate")
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
  def withdraw(id: Int, key: String): Future[Option[String]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/ghostquest/withdraw")
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
  def battleResult(gameid: String, winner: (String, Int), loser: (String, Int)): Future[Option[String]] =  {
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
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
          (v.json \ "data" \ "transaction_id").asOpt[String]
        else None
      }.recover { case e: Exception => None }
  }
}