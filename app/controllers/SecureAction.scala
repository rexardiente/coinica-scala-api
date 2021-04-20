package controllers

import javax.inject.{ Inject, Named, Singleton }
import java.util.UUID
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import akka.actor._
import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json._
import models.domain._
import models.repo._
import models.repo.eosio._
import models.service._
// import utils.lib.EOSIOSupport
import akka.WebSocketActor

@Singleton
class SecureAction {


}