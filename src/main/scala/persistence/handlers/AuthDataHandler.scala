package persistence.handlers

import java.security.MessageDigest
import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.{AuthData, OAuthAccessTokenItem, OAuthClientItem}
import slick.jdbc.PostgresProfile.api._
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import scalaoauth2.provider._

/**
  * Created by Gayo on 12/31/2016.
  */
trait AuthDataHandler {
  def login(username: String, password: String): Future[AuthData]

  def forgotPassword(user: String): Future[Int]
}

class AuthDataHandlerImpl(val modules: Configuration with PersistenceModule) extends AuthDataHandler {
  val tokenEndpoint = new TokenEndpoint {
    override val handlers = Map(
      OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials,
      OAuthGrantType.PASSWORD -> new Password,
      OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode,
      OAuthGrantType.REFRESH_TOKEN -> new RefreshToken
    )
  }

  private def digestString(s: String): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(s.getBytes)
    md.digest.foldLeft("") { (s, b) =>
      s + "%02x".format(if (b < 0) b + 256 else b)
    }
  }

  override def login(username: String, password: String): Future[AuthData] = {
    (for {
      findAccount <- modules.accountsDal.findByMatcher(_.username === username)
      findOauth <- findAccount match {
        case Some(account) if account.password.get == digestString(password) => modules.oauthClientsDal.findById(account.id.get)
        case Some(account) if account.password.get != digestString(password) => throw new Exception("The password doesn't match")
        case None => throw new Exception("The user doesn't exist")
      }
      findAccessToken <- tokenEndpoint.handleRequest(new AuthorizationRequest(Map(), Map(
        "client_id" -> Seq(findOauth.orNull.clientId),
        "client_secret" -> Seq(findOauth.orNull.clientSecret),
        "grant_type" -> Seq(findOauth.orNull.grantType)
      )), modules.oauth2DataHandler).map {
        maybeGrantResponse =>
          maybeGrantResponse.fold(
            oauthError => throw new Exception(s"The user doesn't have access cause ${oauthError.getMessage}"),
            grantResult => OAuthAccessTokenItem(grantResult.tokenType, grantResult.accessToken, grantResult.expiresIn match {
              case Some(value) => (value * 1000) + System.currentTimeMillis
              case _ => 1L
            }, grantResult.refreshToken.getOrElse(""))
          )
      }
    } yield (findAccount, findOauth, findAccessToken)).map {
      case (Some(account), Some(oauth), accessToken) => AuthData(
        account.id.get,
        account.username.get,
        account.email.get,
        account.phone.get,
        OAuthClientItem(
          oauth.grantType,
          oauth.clientId,
          oauth.clientSecret
        ),
        accessToken,
        account.isActive.get,
        account.isVerified.get,
        account.createdAt.get,
        account.updatedAt.orNull
      )
      case _ => throw new Exception("The user doesn't have access")
    }
  }

  override def forgotPassword(user: String): Future[Int] = {
    val newPassword = Random.alphanumeric.toString()

    for {
      findAccount <- modules.accountsDal.findByMatcher(account => account.username === user || account.email === user)
      updateAccount <- findAccount match {
        case Some(account) => modules.accountsDal.update(account.copy(
          password = Some(newPassword),
          updatedAt = Some(new Timestamp(new DateTime().getMillis))
        ))
        case None => Future {
          0
        }
      }
    } yield updateAccount
  }
}
