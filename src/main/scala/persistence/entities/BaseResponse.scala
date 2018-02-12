package persistence.entities

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by Gayo on 11/23/2016.
  */
case class BaseResponse(status: Int, message: String)

object BaseResponse extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val objectFormat: RootJsonFormat[BaseResponse] = jsonFormat2(BaseResponse.apply)
}
