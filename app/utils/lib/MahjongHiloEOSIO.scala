package utils.lib

import javax.inject.{ Inject, Singleton }
import java.util.UUID
import com.typesafe.config.{ Config, ConfigFactory}
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._
import models.domain.eosio.MahjongHiloGameData

@Singleton
class MahjongHiloEOSIO @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = utils.Config.NODE_SERVER_URI

  def declareWinHand(id: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/declare-win-hand")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def declareKong(id: Int, sets: Seq[Int]): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/declare-kong")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "idx" -> sets))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def discardTile(id: Int, index: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/discard-tile")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "idx" -> index))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def playHilo(id: Int, option: Int): Future[Option[String]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/play-hilo")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "option" -> option))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
          Some((v.json \ "data" \ "transaction_id").asOpt[String].getOrElse(""))
        else None
      }.recover { case e: Exception => None }
  }
  def initialize(id: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/start")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def reset(id: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/reset")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def quit(id: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/end")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def addBet(id: Int, quantity: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/addbet")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id, "quantity" -> quantity))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def start(id: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/bet-token")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def transfer(id: Int): Future[Boolean] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/transfer-token")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200) true
        else false
      }.recover { case e: Exception => false }
  }
  def withdraw(id: Int): Future[(Boolean, String)] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/withdraw-token")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        val transactionID: String = (v.json \ "data" \ "transaction_id").asOpt[String].getOrElse("")
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
          (true, transactionID)
        else (false, null)
      }.recover { case e: Exception => (false, null) }
  }
  def getUserData(id: Int): Future[Option[MahjongHiloGameData]] =  {
    val request: WSRequest = ws.url(nodeServerURI +  "/mahjong-hilo/get-table")
    val complexRequest: WSRequest = request
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)

    complexRequest.post(Json.obj("id" -> id))
      .map { v =>
        if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
          (v.json \ "data" \ "game_data").asOpt[MahjongHiloGameData]
        else None
      }.recover { case e: Exception => None }
  }
}