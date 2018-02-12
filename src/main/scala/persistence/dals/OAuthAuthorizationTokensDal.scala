package persistence.dals

import java.security.SecureRandom
import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.SlickTables.OauthAccessTokenTable
import persistence.entities.{Account, OAuthAccessToken, OAuthClient}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

/**
  * Created by Gayo on 10/27/2016.
  */
trait OAuthAccessTokensDal extends BaseDalImpl[OauthAccessTokenTable, OAuthAccessToken] {
  def create(account: Account, client: OAuthClient): Future[OAuthAccessToken]

  def delete(account: Account, client: OAuthClient): Future[Int]

  def refresh(account: Account, client: OAuthClient): Future[OAuthAccessToken]

  def findByAccessToken(accessToken: String): Future[Option[OAuthAccessToken]]

  def findByAuthorized(account: Account, clientId: String): Future[Option[OAuthAccessToken]]

  def findByRefreshToken(refreshToken: String): Future[Option[OAuthAccessToken]]
}

class OAuthAccessTokensDalImpl(modules: Configuration with PersistenceModule)(implicit override val db: JdbcProfile#Backend#Database) extends OAuthAccessTokensDal {
  override def create(account: Account, client: OAuthClient): Future[OAuthAccessToken] = {
    def randomString(length: Int) = new Random(new SecureRandom()).alphanumeric.take(length).mkString
    val accessToken = randomString(40)
    val refreshToken = randomString(40)
    val createdAt = new Timestamp(new DateTime().getMillis)
    val oauthAccessToken = OAuthAccessToken(
      id = Some(0),
      accountId = account.id.get,
      oauthClientId = client.id.get,
      accessToken = accessToken,
      refreshToken = refreshToken,
      createdAt = Some(createdAt)
    )
    insert(oauthAccessToken).map(id => oauthAccessToken.copy(id = Some(id)))
  }

  override def delete(account: Account, client: OAuthClient): Future[Int] = {
    deleteByFilter(oauthToken => oauthToken.accountId === account.id && oauthToken.oauthClientId === client.id)
  }

  override def refresh(account: Account, client: OAuthClient): Future[OAuthAccessToken] = {
    delete(account, client)
    create(account, client)
  }

  override def findByAuthorized(account: Account, clientId: String): Future[Option[OAuthAccessToken]] = {
    val query = for {
      oauthClient <- modules.oauthClientsDal.tableQ
      token <- tableQ if oauthClient.id === token.oauthClientId && oauthClient.clientId === clientId && token.accountId === account.id
    } yield token
    db.run(query.result).map(_.headOption)
  }

  override def findByAccessToken(accessToken: String): Future[Option[OAuthAccessToken]] = {
    findByFilter(_.accessToken === accessToken).map(_.headOption)
  }

  override def findByRefreshToken(refreshToken: String): Future[Option[OAuthAccessToken]] = {
    val expireAt = new Timestamp(new DateTime().minusMonths(1).getMillis)
    findByFilter(token => token.refreshToken === refreshToken && token.createdAt > expireAt).map(_.headOption)
  }
}