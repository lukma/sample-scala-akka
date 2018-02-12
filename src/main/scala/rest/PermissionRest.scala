package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.{Accepted, BadRequest, Created, InternalServerError}
import akka.http.scaladsl.server.{Directives, Route}
import persistence.dals.PermissionsDal
import persistence.entities.{BaseResponse, Permission, PermissionsResponse}

import scala.util.{Failure, Success}

/**
  * Created by Gayo on 12/26/2016.
  */
trait PermissionRest extends Directives with BaseRest {
  val permissionsDal: PermissionsDal

  def permissionRoute: Route = pathPrefix("permissions") {
    pathEndOrSingleSlash {
      get {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            parameters('filter ? "", 'limit.as[Int] ? 10, 'offset.as[Int] ? 0, 'sortBy ? "id", 'sortType ? "asc") {
              (filter, limit, offset, sortBy, sortType) =>
                onComplete(permissionsDal.finds(auth.user, filter, limit, offset, sortBy, sortType)) {
                  case Success(result) => complete(PermissionsResponse(1, result._1, result._2, result._3))
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
            }
        }
      } ~ post {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            entity(as[Permission]) { objectToInsert =>
              onComplete(permissionsDal.insert(auth.user, objectToInsert)) {
                case Success(_) => complete(Created, BaseResponse(1, s"The object inserted"))
                case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
              }
            }
        }
      }
    } ~ pathPrefix(IntNumber) { matcher =>
      pathEndOrSingleSlash {
        delete {
          authenticateOAuth2Async("realm", oauth2Authenticator) {
            auth =>
              onComplete(permissionsDal.delete(auth.user, matcher)) {
                case Success(resultOpt) => resultOpt match {
                  case 1 => complete(Accepted, BaseResponse(1, s"The object deleted"))
                  case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't exist"))
                }
                case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
          }
        }
      }
    }
  }
}
