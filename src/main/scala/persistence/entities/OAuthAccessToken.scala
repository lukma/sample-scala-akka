package persistence.entities

import java.sql.Timestamp

import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

case class OAuthAccessToken(id: Option[Long] = Some(0),
                            accountId: Long,
                            oauthClientId: Long,
                            accessToken: String,
                            refreshToken: String,
                            createdAt: Option[Timestamp]
                           ) extends BaseEntity

case class OAuthAccessTokenItem(tokenType: String, accessToken: String, expiresIn: Long, refreshToken: String)

object OAuthAccessTokenItem extends DefaultJsonProtocol {

  implicit object LongTimeFormat extends RootJsonFormat[Long] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def write(obj: Long) = JsString(format.format(obj))

    def read(json: JsValue): Long = json match {
      case JsString(s) => format.parse(s).getTime
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val objectFormat: RootJsonFormat[OAuthAccessTokenItem] = jsonFormat4(OAuthAccessTokenItem.apply)
}