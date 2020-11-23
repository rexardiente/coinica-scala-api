package utils

import play.api.libs.json._
import play.api.mvc.WebSocket.MessageFlowTransformer
import akka.stream.scaladsl.Flow
import play.api.libs.streams.AkkaStreams
import play.api.http.websocket._

import scala.util.control.NonFatal

object MessageTransformer {
	val customMessageFlowTransformer: MessageFlowTransformer[JsValue, JsValue] = {
    def closeOnException[T](block: => T) = try {
      Left(block)
    } catch {
      case NonFatal(e) => Right(CloseMessage(Some(CloseCodes.Unacceptable),
        "Unable to parse json message"))
    }

    new MessageFlowTransformer[JsValue, JsValue] {
      def transform(flow: Flow[JsValue, JsValue, _]) = {
        AkkaStreams.bypassWith[Message, JsValue, Message](Flow[Message].collect {
          case BinaryMessage(data) => closeOnException(Json.parse(data.iterator.asInputStream))
          case TextMessage(text) => closeOnException(Json.parse(text))
        })(flow map { json => TextMessage(Json.stringify(Json.toJson(json))) })
      }
    }
  }

  def jsonMessageFlowTransformer[In: Reads, Out: Writes]: MessageFlowTransformer[In, Out] = {
      customMessageFlowTransformer.map(json => Json.fromJson[In](json).fold({ errors =>
        throw WebSocketCloseException(CloseMessage(Some(0), Json.stringify(JsError.toJson(errors))))
      }, identity), out => Json.toJson(out))
  }
}