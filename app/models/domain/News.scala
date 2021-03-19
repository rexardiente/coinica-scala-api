package models.domain

import java.util.UUID
import java.time.Instant
import play.api.libs.json._
import play.api.libs.functional.syntax._

object News {
	val tupled = (apply: (UUID, String, String, String, String, String, Instant) => News).tupled
	def apply(id: UUID,
						title: String,
						subTitle: String,
						description: String,
						author: String,
						url: String,
						createdAt: Instant): News =
		new News(id, title, subTitle, description, author, url, createdAt)
	def apply(title: String, subTitle: String, description: String, author: String, url: String): News =
		new News(UUID.randomUUID, title, subTitle, description, author, url, Instant.now)
	implicit def implNews = Json.format[News]
}
case class News(id: UUID,
								title: String,
								subTitle: String,
								description: String,
								author: String,
								url: String,
								createdAt: Instant)