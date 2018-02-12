package rest

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import persistence.dals.TopicsDal
import persistence.entities.{BaseResponse, Topic, TopicsResponse}

import scala.util.{Failure, Success}

/**
  * Created by Gayo on 12/2/2016.
  */
trait TopicRest extends Directives with BaseRest {
  val topicsDal: TopicsDal

  def topicRoute: Route = pathPrefix("topics") {
    pathEndOrSingleSlash {
      get {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            parameters('filter ? "", 'limit.as[Int] ? 10, 'offset.as[Int] ? 0, 'sortBy ? "id", 'sortType ? "asc") {
              (filter, limit, offset, sortBy, sortType) =>
                onComplete(topicsDal.finds(auth.user, filter, limit, offset, sortBy, sortType)) {
                  case Success(result) => complete(TopicsResponse(1, result._1, result._2, result._3))
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
            }
        }
      } ~ post {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            entity(as[Topic]) { objectToInsert =>
              onComplete(topicsDal.insert(auth.user, objectToInsert)) {
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
              onComplete(topicsDal.delete(auth.user, matcher)) {
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
