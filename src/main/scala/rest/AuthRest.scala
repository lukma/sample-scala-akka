package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import persistence.entities.{AuthDataResponse, BaseResponse, OAuthAccessTokenItem}
import persistence.handlers.AuthDataHandler

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import scalaoauth2.provider._

/**
  * Created by Gayo on 10/27/2016.
  */
trait AuthRest extends Directives with BaseRest {
  val authDataHandler: AuthDataHandler

  val tokenEndpoint = new TokenEndpoint {
    override val handlers = Map(
      OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials,
      OAuthGrantType.PASSWORD -> new Password,
      OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode,
      OAuthGrantType.REFRESH_TOKEN -> new RefreshToken
    )
  }

  def accessTokenRoute: Route = pathPrefix("auth") {
    pathPrefix("login") {
      post {
        formFields('username, 'password) { (username, password) =>
          onComplete(authDataHandler.login(username, password)) {
            case Success(result) => complete(AuthDataResponse(1, result))
            case Failure(ex) => complete(ex.getMessage match {
              case message if message.equals("The user doesn't exist") => NotFound
              case message if message.equals("The password doesn't match") => Unauthorized
              case message if message.equals("The user doesn't have access") => Unauthorized
              case _ => InternalServerError
            }, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
    } ~ pathPrefix("forgotPassword") {
      post {
        formFields('user) { user =>
          onComplete(authDataHandler.forgotPassword(user)) {
            case Success(resultOptChild) => resultOptChild match {
              case 1 => complete(Accepted, BaseResponse(1, s"The object updated"))
              case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't updated"))
            }
            case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
    } ~ pathPrefix("access_token") {
      post {
        formFieldMap { fields =>
          onComplete(tokenEndpoint.handleRequest(new AuthorizationRequest(Map(), fields.map(m => m._1 -> Seq(m._2))), oauth2DataHandler)) {
            case Success(maybeGrantResponse) => maybeGrantResponse.fold(
              oauthError => complete(Unauthorized, s"An error occurred: ${oauthError.getMessage}"),
              grantResult => complete(OAuthAccessTokenItem(grantResult.tokenType, grantResult.accessToken, grantResult.expiresIn match {
                case Some(value) => (value * 1000) + System.currentTimeMillis
                case _ => 1L
              }, grantResult.refreshToken.getOrElse("")))
            )
            case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }
}

