package persistence.handlers

import persistence.entities.{Account, OAuthAccessToken}
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaoauth2.provider.{ClientCredentialsRequest, InvalidClient, PasswordRequest, _}

/**
  * Created by Gayo on 10/27/2016.
  */
class OAuth2DataHandler(val modules: Configuration with PersistenceModule) extends DataHandler[Account] {
  override def validateClient(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Boolean] = {
    request.parseClientCredential.fold(Future.successful(false)) {
      case Right(clientCredential) => modules.oauthClientsDal.validate(clientCredential.clientId, clientCredential.clientSecret.getOrElse(""), request.grantType)
      case Left(_) => Future.successful(false)
    }
  }

  override def getStoredAccessToken(authInfo: AuthInfo[Account]): Future[Option[AccessToken]] = {
    modules.oauthAccessTokensDal.findByAuthorized(authInfo.user, authInfo.clientId.getOrElse("")).map(_.map(toAccessToken))
  }

  private val accessTokenExpireSeconds = 3600

  private def toAccessToken(accessToken: OAuthAccessToken) = {
    AccessToken(
      accessToken.accessToken,
      Some(accessToken.refreshToken),
      None,
      Some(accessTokenExpireSeconds),
      accessToken.createdAt.get
    )
  }

  override def createAccessToken(authInfo: AuthInfo[Account]): Future[AccessToken] = {
    authInfo.clientId.fold(Future.failed[AccessToken](new InvalidRequest())) { clientId =>
      (for {
        clientOpt <- modules.oauthClientsDal.findByClientId(clientId)
        toAccessToken <- modules.oauthAccessTokensDal.create(authInfo.user, clientOpt.get).map(toAccessToken) if clientOpt.isDefined
      } yield toAccessToken).recover { case _ => throw new InvalidRequest() }
    }
  }

  override def findUser(maybeCredential: Option[ClientCredential], request: AuthorizationRequest): Future[Option[Account]] =
    request match {
      case request: PasswordRequest =>
        modules.accountsDal.authenticate(request.username, request.password)
      case request: ClientCredentialsRequest =>
        request.parseClientCredential.fold(Future.failed[Option[Account]](new InvalidRequest())) {
          case Right(clientCredential) => for {
            maybeAccount <- modules.oauthClientsDal.findClientCredentials(
              clientCredential.clientId,
              clientCredential.clientSecret.getOrElse("")
            )
          } yield maybeAccount
          case Left(_) => Future.successful(None)
        }
      case _ =>
        Future.successful(None)
    }

  override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[Account]]] = {
    modules.oauthAccessTokensDal.findByRefreshToken(refreshToken).flatMap {
      case Some(accessToken) =>
        for {
          account <- modules.accountsDal.findById(accessToken.accountId)
          client <- modules.oauthClientsDal.findByClientId(accessToken.oauthClientId)
        } yield {
          Some(AuthInfo(
            user = account.get,
            clientId = Some(client.get.clientId),
            scope = None,
            redirectUri = client.get.redirectUri
          ))
        }
      case None => Future.failed(new InvalidRequest())
    }
  }

  override def refreshAccessToken(authInfo: AuthInfo[Account], refreshToken: String): Future[AccessToken] = {
    authInfo.clientId.fold(Future.failed[AccessToken](new InvalidRequest())) { clientId =>
      (for {
        clientOpt <- modules.oauthClientsDal.findByClientId(clientId)
        toAccessToken <- modules.oauthAccessTokensDal.refresh(authInfo.user, clientOpt.get).map(toAccessToken) if clientOpt.isDefined
      } yield toAccessToken).recover { case _ => throw new InvalidClient() }
    }
  }

  override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[Account]]] = {
    modules.oauthAuthorizationCodesDal.findByCode(code).flatMap {
      case Some(result) =>
        for {
          account <- modules.accountsDal.findById(result.accountId)
          client <- modules.oauthClientsDal.findByClientId(result.oauthClientId)
        } yield {
          Some(AuthInfo(
            user = account.get,
            clientId = Some(client.get.clientId),
            scope = None,
            redirectUri = client.get.redirectUri
          ))
        }
      case None => Future.failed(new InvalidRequest())
    }
  }

  override def deleteAuthCode(code: String): Future[Unit] = modules.oauthAuthorizationCodesDal.delete(code).map(_ => {})

  override def findAccessToken(token: String): Future[Option[AccessToken]] =
    modules.oauthAccessTokensDal.findByAccessToken(token).map(_.map(toAccessToken))

  override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[Account]]] = {
    modules.oauthAccessTokensDal.findByAccessToken(accessToken.token).flatMap {
      case Some(result) =>
        for {
          account <- modules.accountsDal.findById(result.accountId)
          client <- modules.oauthClientsDal.findByClientId(result.oauthClientId)
        } yield {
          Some(AuthInfo(
            user = account.get,
            clientId = Some(client.get.clientId),
            scope = None,
            redirectUri = client.get.redirectUri
          ))
        }
      case None => Future.failed(new InvalidRequest())
    }
  }
}