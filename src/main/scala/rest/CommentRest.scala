package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import persistence.dals.CommentsDal
import persistence.entities.{Comment, CommentsResponse, BaseResponse}

import scala.util.{Failure, Success}

/**
  * Created by Gayo on 12/1/2016.
  */
trait CommentRest extends Directives with BaseRest {
  val commentsDal: CommentsDal

  def commentRoute: Route = pathPrefix("comments") {
    pathEndOrSingleSlash {
      get {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            parameters('postId.as[Int], 'filter ? "", 'parentId.as[Int].?, 'limit.as[Int] ? 10, 'offset.as[Int] ? 0, 'sortBy ? "id", 'sortType ? "asc") {
              (postId, filter, parentId, limit, offset, sortBy, sortType) =>
                onComplete(commentsDal.finds(auth.user, postId, filter, parentId, limit, offset, sortBy, sortType)) {
                  case Success(result) => complete(CommentsResponse(1, result._1, result._2, result._3))
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
            }
        }
      } ~ post {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            entity(as[Comment]) { objectToInsert =>
              onComplete(commentsDal.insert(auth.user, objectToInsert)) {
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
              entity(as[Comment]) { objectToUpdate =>
                onComplete(commentsDal.update(auth.user, matcher.toLong, objectToUpdate)) {
                  case Success(resultOptChild) => resultOptChild match {
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
              onComplete(commentsDal.delete(auth.user, matcher.toLong)) {
                case Success(resultOpt) => resultOpt match {
                  case 1 => complete(Accepted, BaseResponse(1, s"The object deleted"))
                  case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't exist"))
                }
                case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
              }
          }
        }
      }
    }
  }
}
