package persistence.entities

import java.sql.Timestamp

case class OAuthAuthorizationCode(id: Option[Long] = Some(0),
                                  accountId: Long,
                                  oauthClientId: Long,
                                  code: String,
                                  redirectUri: Option[String],
                                  createdAt: Option[Timestamp]) extends BaseEntity

