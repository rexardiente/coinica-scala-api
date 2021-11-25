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
import models.service.PlatformConfigService
import utils.SystemConfig.{ NODE_SERVER_URI, DEFAULT_HOST }

@Singleton
class MahjongHiloEOSIO @Inject()(config: PlatformConfigService)(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = NODE_SERVER_URI

  def declareWinHand(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/declare-win-hand")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def resetBet(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/reset-bet")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def declareKong(id: Int, sets: Seq[Int]): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/declare-kong")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "idx" -> sets))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def discardTile(id: Int, index: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/discard-tile")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "idx" -> index))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def playHilo(id: Int, option: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/play-hilo")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "option" -> option))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def initialize(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/start")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def end(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/end")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def addBet(id: Int, quantity: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/addbet")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id, "quantity" -> quantity))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def start(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/bet-token")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def transfer(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/transfer-token")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def withdraw(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/withdraw-token")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
  def getUserData(id: Int): Future[Option[MahjongHiloGameData]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/get-table")
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
    } yield (response)
  }
  def riichiDiscard(id: Int): Future[Option[String]] =  {
    for {
      host <- config.getHostByName(nodeServerURI).map(_.map(_.getURL()).getOrElse(DEFAULT_HOST))
      response <- {
        val request: WSRequest = ws.url(host +  "/mahjong-hilo/riichi-discard")
        val complexRequest: WSRequest = request
          .addHttpHeaders("Accept" -> "application/json")
          .withRequestTimeout(10000.millis)

        complexRequest.post(Json.obj("id" -> id))
          .map { v =>
            if (!(v.json \ "error").asOpt[Boolean].getOrElse(true) && (v.json \ "code").asOpt[Int].getOrElse(0) == 200)
              (v.json \ "data" \ "transaction_id").asOpt[String]
            else None
          }.recover { case e: Exception => None }
      }
    } yield (response)
  }
}