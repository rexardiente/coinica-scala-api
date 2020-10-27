package controllers

import javax.inject._
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._

// model's import
import models.domain.ranking.Ranking
import models.domain.dailytask.Dailytask
import models.domain.dailychallenge.Dailychallenge
import models.domain.referral.Referral
import models.domain.game.{Game, Genre}
import models.repo.game.GameRepo
import models.repo.game.GenreRepo
import models.repo.dailytask.DailytaskRepo
import models.repo.referral.ReferralRepo
import models.repo.dailychallenge.DailychallengeRepo
import models.repo.ranking.RankingRepo

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
      rankingRepo: RankingRepo,
      gameRepo: GameRepo,
      genreRepo: GenreRepo,
      referralRepo: ReferralRepo,
      dailytaskRepo: DailytaskRepo,
      dailychallengeRepo: DailychallengeRepo,
      val controllerComponents: ControllerComponents
    ) extends BaseController {
  
  /*  CUSTOM FORM VALIDATION */
  private def gameForm = Form(tuple(
    "game" -> nonEmptyText,
    "imgURL" -> nonEmptyText,
    "genre" -> uuid,
    "description" -> optional(text),
  ))
  private def referralForm = Form(tuple(
    "name" -> nonEmptyText,
    "imgURL" -> nonEmptyText,
    "genre" -> uuid,
    "description" -> optional(text),
  ))
private def dailytaskForm = Form(tuple(
    "gamename" -> nonEmptyText,
    "taskdescription" -> optional(text),
  ))
  private def dailychallengeForm = Form(tuple(
     "gamename" -> nonEmptyText,
    "rankname" -> optional(text),
    "rankreward" -> nonEmptyText,
  ))
  private def rankingForm = Form(tuple(
    "rankname" -> optional(text),
    "rankdesc" -> nonEmptyText,
  ))
  private def genreForm = Form(tuple(
    "name" -> nonEmptyText,
    "description" -> optional(text),
  ))

  /* MAIN API */
  def index() = Action.async { implicit request: Request[AnyContent] =>
    Future.successful(Ok(views.html.index()))
  }
  /* Dailychallenge API */
   
    def dailychallenges() = Action.async { implicit request: Request[AnyContent] =>
    dailychallengeRepo.all().map(dailychallenge => Ok(Json.toJson(dailychallenge)))
  }
  
   def addDailychallenge() = Action.async { implicit request: Request[AnyContent] =>
    dailychallengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (gamename, rankname, rankreward)  =>
        dailychallengeRepo
          .add(Dailychallenge(UUID.randomUUID, gamename, rankname,  rankreward))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }
 
  def findDailychallengeByID(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    dailychallengeRepo.findByID(id).map(dailychallenge => Ok(Json.toJson(dailychallenge)))
  }

  def updateDailychallenge(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    dailychallengeForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case ( gamename, rankname,  rankreward) =>
        dailychallengeRepo
          .update(Dailychallenge(id, gamename, rankname,  rankreward))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }

def removeDailychallenge(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    dailychallengeRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }
/* Dailytask API */
   
    def dailytasks() = Action.async { implicit request: Request[AnyContent] =>
    dailytaskRepo.all().map(dailytask => Ok(Json.toJson(dailytask)))
  }
  
   def addDailytask() = Action.async { implicit request: Request[AnyContent] =>
    dailytaskForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (gamename, taskdescription)  =>
        dailytaskRepo
          .add(Dailytask(UUID.randomUUID, gamename,  taskdescription))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }
 
  def findDailytaskByID(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    dailytaskRepo.findByID(id).map(dailytask => Ok(Json.toJson(dailytask)))
  }

  def updateDailytask(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    dailytaskForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case ( gamename,  taskdescription) =>
        dailytaskRepo
          .update(Dailytask(id, gamename,  taskdescription))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }

def removeDailytask(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    dailytaskRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }
   /* REFERRAL API */
   
    def referrals() = Action.async { implicit request: Request[AnyContent] =>
    referralRepo.all().map(referral => Ok(Json.toJson(referral)))
  }
 
  def addReferral() = Action.async { implicit request: Request[AnyContent] =>
    referralForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name,imgURL, genre, description)  =>
        referralRepo
          .add(Referral(UUID.randomUUID, name, imgURL, genre, description))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }
  def findReferralByID(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    referralRepo.findByID(id).map(referral => Ok(Json.toJson(referral)))
  }
  def updateReferral(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    referralForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name,  imgURL, genre, description) =>
        referralRepo
          .update(Referral(id, name, imgURL, genre, description))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }

def removeReferral(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    referralRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }
 

  /* GAME API */
  def games() = Action.async { implicit request: Request[AnyContent] =>
    gameRepo.all().map(game => Ok(Json.toJson(game)))
  }

  def addGame() = Action.async { implicit request: Request[AnyContent] =>
    gameForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (game, imgURL, genre, description)  =>
        for {
          isExist <- genreRepo.exist(genre)
          result  <- {
            if (isExist)
              gameRepo
                .add(Game(UUID.randomUUID, game, imgURL, genre, description))
                .map(r => if(r < 0) InternalServerError else Created )
            else Future.successful(InternalServerError)
          }
        } yield(result)
      }
    )
  }

  def findGameByID(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    gameRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def updateGame(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    gameForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (game, imgURL, genre, description) =>
        for {
          isExist <- genreRepo.exist(genre)
           result <- {
              if (isExist)
                gameRepo
                  .update(Game(id, game, imgURL, genre, description))
                  .map(r => if(r < 0) NotFound else Ok)
              else Future.successful(InternalServerError)
           }
        } yield(result)
        
      }
    )
  }

  def removeGame(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    gameRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }

  /* GENRE API */
  def genres() = Action.async { implicit request: Request[AnyContent] =>
    genreRepo.all().map(genre => Ok(Json.toJson(genre)))
  }

  def findGenreByID(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    genreRepo.findByID(id).map(game => Ok(Json.toJson(game)))
  }

  def addGenre() = Action.async { implicit request: Request[AnyContent] =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description)  =>
        genreRepo
          .add(Genre(UUID.randomUUID, name, description))
          .map(r => if(r < 0) InternalServerError else Created )
      }
    )
  }

  def updateGenre(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    genreForm.bindFromRequest.fold(
      formErr => Future.successful(BadRequest("Form Validation Error.")),
      { case (name, description) =>
        genreRepo
          .update(Genre(id, name, description))
          .map(r => if(r < 0) NotFound else Ok)
      }
    )
  }

  def removeGenre(id: UUID) = Action.async { implicit request: Request[AnyContent] =>
    genreRepo
      .delete(id)
      .map(r => if(r < 0) NotFound else Ok)
  }
  
}
