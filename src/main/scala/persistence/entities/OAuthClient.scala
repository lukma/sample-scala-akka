package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class OAuthClient(id: Option[Long] = Some(0),
                       ownerId: Long,
                       grantType: String,
                       clientId: String,
                       clientSecret: String,
                       redirectUri: Option[String],
                       createdAt: Option[Timestamp]) extends BaseEntity

case class OAuthClientItem(grantType: String, clientId: String, clientSecret: String)

object OAuthClientItem {
  implicit val objectFormat: RootJsonFormat[OAuthClientItem] = jsonFormat3(OAuthClientItem.apply)
}

