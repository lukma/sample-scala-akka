package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 12/31/2016.
  */
case class AuthData(id: Long,
                    username: String,
                    email: String,
                    phone: String,
                    oAuth: OAuthClientItem,
                    accessToken: OAuthAccessTokenItem,
                    isActive: Boolean,
                    isVerified: Boolean,
                    createdAt: Timestamp,
                    updatedAt: Timestamp)

object AuthData {

  implicit object TimestampFormat extends RootJsonFormat[Timestamp] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def write(obj: Timestamp): JsValue = if (obj != null) {
      JsString(format.format(obj.getTime))
    } else {
      JsNull
    }

    def read(json: JsValue): Timestamp = json match {
      case JsString(s) => new Timestamp(format.parse(s).getTime)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val objectFormat: RootJsonFormat[AuthData] = jsonFormat10(AuthData.apply)
}

case class AuthDataResponse(status: Int, result: AuthData)

object AuthDataResponse {
  implicit val objectFormat: RootJsonFormat[AuthDataResponse] = jsonFormat2(AuthDataResponse.apply)
}
