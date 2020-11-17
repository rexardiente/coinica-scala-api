package utils

import java.time.Instant
import play.api.mvc.{ PathBindable, QueryStringBindable }

object Binders extends utils.CommonImplicits {
  implicit def queryStringInstantBinder(implicit  instantBinder: QueryStringBindable[String] ) = new QueryStringBindable[Instant] {
    override def bind(key: String, value: Map[String, Seq[String]]): Option[Either[String, Instant]] = {
      try {
        for {
          param   <- instantBinder.bind(key, value)
        } yield {
          (param) match {
            case (Right(param)) => Right(Instant.parse(param))
            case _ => Left("Invalid Time Format")
          }
        }
      } catch {
        case _:Throwable => Some(Left("Invalid Time Format"))
      }
    }

    override def unbind(key: String, value: Instant): String =
      instantBinder.unbind(key, value.toString)
  }

  implicit def pathBindLongToUnix2(implicit intBinder: PathBindable[String]) = new PathBindable[Instant] {
    override def bind(key: String, value: String): Either[String, Instant] = {
      for {
        index <- intBinder.bind(key, value)
        convert <- try {
                    // split by `=` just in case user provides key and value
                    Right(Instant.parse(index.split("=").last))
                  } catch {
                    case e: Throwable => Left("Invalid Time Format")
                  }
      } yield convert
    }
    override def unbind(key: String, value: Instant): String = value.getEpochSecond.toString
  }

  // case class TransactionPathBind(start: Instant, end: Instant, limit: Int, offset: Int)
  // implicit def queryStringBinder( 
  //         implicit  instantBinder: QueryStringBindable[Instant], 
  //         intBinder: QueryStringBindable[Int]
  //       ) = new QueryStringBindable[utils.TransactionPathBind] {
  //   private def subBind[T](key: String, subkey: String, params: Map[String, Seq[String]])(implicit b: QueryStringBindable[T]): Either.RightProjection[String, Option[T]] = {
  //     b.bind(s"$key.$subkey", params).map(_.right.map(r => Option(r))).getOrElse(Right(None)).right
  //   }

  //   override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, utils.TransactionPathBind]] = Some {
  //     def bnd[T](s: String)(implicit b: QueryStringBindable[T]) = subBind[T](key, s, params)
  //     for {
  //       start <- bnd[Instant]("start")
  //       end <- bnd[Instant]("end")
  //       limit <- bnd[Int]("limit")
  //       offset <- bnd[Int]("offset")
  //     } yield TransactionPathBind(start.getOrElse(Instant.now), end.getOrElse(Instant.now), limit.getOrElse(0), offset.getOrElse(5))
  //     // {
  //     //   (start, end, limit, offset) match {
  //     //     case (Right(start), Right(end), Right(limit), Right(offset)) => ???
  //     //      // Right(utils.TransactionPathBind(start, end, limit, offset))
  //     //     case _ =>  ???
  //     //       // Left("Unable to bind a Pager")
  //     //   }
  //     // }
  //   }
  //   override def unbind(key: String, pager: utils.TransactionPathBind): String =
  //     intBinder.unbind(key + ".limit", pager.limit)
  // }
}