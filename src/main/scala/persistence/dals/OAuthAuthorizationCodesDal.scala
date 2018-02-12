package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.OAuthAuthorizationCode
import persistence.entities.SlickTables.OauthAuthorizationCodeTable
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/27/2016.
  */
trait OAuthAuthorizationCodesDal extends BaseDalImpl[OauthAuthorizationCodeTable, OAuthAuthorizationCode] {
  def findByCode(code: String): Future[Option[OAuthAuthorizationCode]]

  def delete(code: String): Future[Int]
}

class OAuthAuthorizationCodesDalImpl()(implicit override val db: JdbcProfile#Backend#Database) extends OAuthAuthorizationCodesDal {
  override def findByCode(code: String): Future[Option[OAuthAuthorizationCode]] = {
    val expireAt = new Timestamp(new DateTime().minusHours(1).getMillis)
    findByFilter(authCode => authCode.code === code && authCode.createdAt > expireAt).map(_.headOption)
  }

  override def delete(code: String): Future[Int] = deleteByFilter(_.code === code)
}