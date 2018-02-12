package rest

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.Credentials
import persistence.entities.Account
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaoauth2.provider._

/**
  * Created by Gayo on 10/27/2016.
  */
trait BaseRest extends Directives with DefaultJsonProtocol {
  val oauth2DataHandler: DataHandler[Account]

  def oauth2Authenticator(credentials: Credentials): Future[Option[AuthInfo[Account]]] =
    credentials match {
      case Credentials.Provided(token) =>
        oauth2DataHandler.findAccessToken(token).flatMap {
          case Some(result) => oauth2DataHandler.findAuthInfoByAccessToken(result)
          case None => Future.successful(None)
        }
      case _ => Future.successful(None)
    }
}
