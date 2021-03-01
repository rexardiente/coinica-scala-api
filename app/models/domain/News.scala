package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object News {
	val tupled = (apply: (UUID, String, String, String, String, List[String], Instant) => News).tupled
	def apply(id: UUID,
						title: String,
						subTitle: String,
						description: String,
						author: String,
						images: List[String],
						createdAt: Instant): News =
		new News(id, title, subTitle, description, author, images, createdAt)
	def apply(title: String, subTitle: String, description: String, author: String, images: List[String]): News =
		new News(UUID.randomUUID, title, subTitle, description, author, images, Instant.now)
	implicit def implNews = Json.format[News]
}
case class News(id: UUID,
								title: String,
								subTitle: String,
								description: String,
								author: String,
								images: List[String],
								createdAt: Instant)