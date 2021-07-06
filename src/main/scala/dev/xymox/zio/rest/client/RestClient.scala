package dev.xymox.zio.rest.client

import dev.xymox.zio.rest.client.RestError.ClientError
import zhttp.core.ByteBuf
import zhttp.http.HttpData.CompleteData
import zhttp.http.{Header, HttpData, Method, Request, UHttpResponse, URL}
import zhttp.service.{ChannelFactory, Client, EventLoopGroup}
import zio.{IO, Task, TaskLayer, ZIO}
import zio.json._

/** Defines the core operations for a RESTful client.
  *
  * This uses zio-http to perform the lower level client logic and will encode/decode request/responses to JSON using
  * zio-json.
  */
trait RestClient {

  val env: TaskLayer[ChannelFactory with EventLoopGroup]

  protected def doGet[A](url: URL, headers: Seq[Header])(implicit decoder: JsonDecoder[A]): IO[RestError, A] =
    doRequest[A, EmptyEntity](Method.GET, url, headers, EmptyEntity())
  protected def doPost[A, E](url: URL, headers: Seq[Header], bodyEntity: E)(implicit decoder: JsonDecoder[A], encoder: JsonEncoder[E]): IO[RestError, A]
  protected def doPut[A, E](url: URL, headers: Seq[Header], bodyEntity: E)(implicit decoder: JsonDecoder[A], encoder: JsonEncoder[E]): IO[RestError, A]
  protected def doPatch[A, E](url: URL, headers: Seq[Header], bodyEntity: E)(implicit decoder: JsonDecoder[A], encoder: JsonEncoder[E]): IO[RestError, A]
  protected def doDelete[A](url: URL, header: Seq[Header])(implicit decoder: JsonDecoder[A]): IO[RestError, A]
  protected def doOptions(url: URL, header: Seq[Header]): IO[RestError, Seq[Header]]

  private def doRequest[A, E](method: Method, url: URL, headers: Seq[Header], bodyEntity: E)(implicit
    decoder: JsonDecoder[A],
    encoder: JsonEncoder[E]
  ): IO[RestError, A] = {
    for {
      request      <- buildRequest(bodyEntity, method, url, headers.toList)
      httpResponse <- Client.request(request).mapError(RestError.fromThrowable)
      results      <- ZIO.fromEither(processResponse[A](httpResponse)).mapError(ClientError)
    } yield results
  }.provideLayer(env).mapError(RestError.fromObject)

  private def buildRequest[E](bodyEntity: E, method: Method, url: URL, headers: List[Header])(implicit jsonEncoder: JsonEncoder[E]): Task[Request] =
    bodyEntity match {
      case EmptyEntity() => ZIO.succeed(Request((method -> url), headers))
      case entity        =>
        for {
          entityAsHttpData <- buildHttpData(entity)
        } yield Request((method -> url), headers, entityAsHttpData)
    }

  private def buildHttpData[E](entity: E)(implicit jsonEncoder: JsonEncoder[E]): Task[HttpData[Any, Nothing]] =
    for {
      byteBuf <- ByteBuf.fromString(entity.toJson)
      data     = HttpData.fromByteBuf(byteBuf.asJava)
    } yield data

  // TODO - handle status responses; build helper for zHttp.Status that categorizes status codes (errors, client, server, etc)
  private def processResponse[A](response: UHttpResponse)(implicit decoder: JsonDecoder[A]): Either[String, A] = response.content match {
    case CompleteData(data) => data.map(_.toChar).mkString.fromJson[A]
    case data               => Left(s"Unsupported response: $data")
  }
}
