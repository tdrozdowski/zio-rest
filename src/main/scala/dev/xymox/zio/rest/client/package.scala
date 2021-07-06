package dev.xymox.zio.rest

import zio.json.{DeriveJsonCodec, JsonCodec}

package object client {

  sealed trait RestError

  object RestError {
    case class NotFound(message: String)       extends RestError
    case class ClientError(message: String)    extends RestError
    case class ServerError(message: String)    extends RestError
    case class ExecutionError(message: String) extends RestError
    case class UnknownError(message: String)   extends RestError

    def fromThrowable(error: Throwable): RestError = ExecutionError(error.getMessage)
    def fromObject(error: Object): RestError       = ExecutionError(s"Unknown error: $error")
  }

  // Useful in cases where an endpoint wants a POST with no body; or a response is empty
  case class EmptyEntity()

  case object EmptyEntity {
    implicit val codec: JsonCodec[EmptyEntity] = DeriveJsonCodec.gen[EmptyEntity]
  }
}
