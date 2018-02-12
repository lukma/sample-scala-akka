package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import persistence.dals.RolesDal
import persistence.entities.{BaseResponse, Role, RoleForm, RolesResponse}

import scala.util.{Failure, Success}

/**
  * Created by Gayo on 11/21/2016.
  */
trait RoleRest extends Directives with BaseRest {
  val rolesDal: RolesDal

  def roleRoute: Route = pathPrefix("roles") {
    pathEndOrSingleSlash {
      get {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            parameters('filter ? "", 'parentId.as[Int].?, 'limit.as[Int] ? 10, 'offset.as[Int] ? 0, 'sortBy ? "id", 'sortType ? "asc") {
              (filter, parentId, limit, offset, sortBy, sortType) =>
                onComplete(rolesDal.finds(auth.user, filter, parentId, limit, offset, sortBy, sortType)) {
                  case Success(result) => complete(RolesResponse(1, result._1, result._2, result._3))
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
            }
        }
      } ~ post {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            entity(as[RoleForm]) { objectToInsert =>
              onComplete(rolesDal.insert(auth.user,
                Role(title = objectToInsert.title, parentId = objectToInsert.parentId),
                objectToInsert.permissionsToInsert.getOrElse(Seq()))) {
                case Success(_) => complete(Created, BaseResponse(1, s"The object inserted"))
                case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
              }
            }
        }
      }
    } ~ pathPrefix(IntNumber) { matcher =>
      pathEndOrSingleSlash {
        patch {
          authenticateOAuth2Async("realm", oauth2Authenticator) {
            auth =>
              entity(as[RoleForm]) { objectToUpdate =>
                onComplete(rolesDal.update(auth.user, matcher.toLong,
                  Role(title = objectToUpdate.title, parentId = objectToUpdate.parentId),
                  objectToUpdate.permissionsToInsert.getOrElse(Seq()),
                  objectToUpdate.permissionsToDelete.getOrElse(Seq()))) {
                  case Success(resultOpt) => resultOpt match {
                    case 1 => complete(Accepted, BaseResponse(1, s"The object updated"))
                    case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't updated"))
                  }
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
              }
          }
        } ~ delete {
          authenticateOAuth2Async("realm", oauth2Authenticator) {
            auth =>
              onComplete(rolesDal.delete(auth.user, matcher)) {
                case Success(resultOpt) => resultOpt match {
                  case 1 => complete(Accepted, BaseResponse(1, s"The object deleted"))
                  case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't deleted"))
                }
                case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
          }
        }
      }
    }
  }
}
