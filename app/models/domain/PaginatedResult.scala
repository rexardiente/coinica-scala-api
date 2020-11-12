package models.domain

import play.api.libs.json._
import play.api.libs.functional.syntax._

object PaginatedResult {
    implicit def paginatedResultReads[T](implicit fmt: Reads[T]): Reads[PaginatedResult[T]] = new Reads[PaginatedResult[T]] {
      override def reads(json: JsValue): JsResult[PaginatedResult[T]] = json match {
        case js: JsValue => 
          try {
            JsSuccess(PaginatedResult(
              (json \ "total_count").as[Int],
              (json \ "entities").as[List[T]],
              (json \ "has_next_page").as[Boolean]))
          } catch {
              case e: Throwable => JsError(Seq(JsPath() -> Seq(JsonValidationError(e.toString))))
          }
        case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsobject"))))
      }
    }
  
  implicit def paginatedResultWrites[T](implicit fmt: Writes[T]): Writes[PaginatedResult[T]] = new Writes[PaginatedResult[T]] {
    def writes(pgRes: PaginatedResult[T]) = Json.obj(
      "total_count" -> pgRes.totalCount,
      "entities" -> pgRes.entities,
      "has_next_page" -> pgRes.hasNextPage)
  }
}

case class PaginatedResult[T](totalCount: Int, entities: List[T], hasNextPage: Boolean)