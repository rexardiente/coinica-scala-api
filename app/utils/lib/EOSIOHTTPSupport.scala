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
import models.domain.eosio.TableRowsRequest

@Singleton
class EOSIOHTTPSupport @Inject()(implicit ws: WSClient, ec: ExecutionContext) {
  val nodeServerURI: String = utils.Config.NODE_SERVER_URI

  def getTableRows(req: TableRowsRequest, sender: Option[String]): Future[Option[GQRowsResponse]] =  {
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

  def battleResult(gameid: String, winner: (String, String), loser: (String, String)): Future[Option[String]] =  {
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
      // return transaction ID
  }

  def eliminate(user: String, characterID: String): Future[Option[String]] = {
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
      // return transaction ID
  }
}