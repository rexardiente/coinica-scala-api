package utils.auth

import java.util.UUID
import scala.collection.mutable.HashMap

object AccountTokenSession {
    // account id ~> [token, Expiration time]
    val LOGIN = HashMap.empty[UUID, (String, Long)]
    // tokens on this lists are only used for email request to bypass checking login sessions
    val ADD_OR_RESET_EMAIL = HashMap.empty[UUID, (String, Long)]
    val RESET_PASSWORD = HashMap.empty[UUID, (String, Long)]
}
