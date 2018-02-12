package rest

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.FileIO
import persistence.dals.AccountsDal
import persistence.entities._

import scala.util.{Failure, Success}

/**
  * Created by Gayo on 11/24/2016.
  */
trait AccountRest extends Directives with BaseRest {
  val accountsDal: AccountsDal

  def accountRoute: Route = pathPrefix("accounts") {
    pathEndOrSingleSlash {
      get {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            parameters('filter ? "", 'limit.as[Int] ? 10, 'offset.as[Int] ? 0, 'sortBy ? "id", 'sortType ? "asc") {
              (filter, limit, offset, sortBy, sortType) =>
                onComplete(accountsDal.finds(auth.user, filter, limit, offset, sortBy, sortType)) {
                  case Success(result) => complete(AccountsResponse(1, result._1, result._2, result._3))
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
            }
        }
      } ~ post {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            entity(as[Account]) { objectToInsert =>
              onComplete(accountsDal.insert(auth.user, objectToInsert)) {
                case Success(_) => complete(Created, BaseResponse(1, s"The object inserted"))
                case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
              }
            }
        }
      }
    } ~ pathPrefix(IntNumber | Segment) { matcher =>
      pathEndOrSingleSlash {
        get {
          authenticateOAuth2Async("realm", oauth2Authenticator) {
            auth =>
              onComplete(accountsDal.find(auth.user, matcher)) {
                case Success(resultOpt) => resultOpt match {
                  case Some(result) => complete(AccountResponse(1, result))
                  case None => complete(NotFound, BaseResponse(0, s"The object doesn't exist"))
                }
                case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
              }
          }
        } ~ patch {
          validate(matcher.isInstanceOf[Int], BaseResponse.objectFormat.write(BaseResponse(0, "The object id should be integer")).prettyPrint) {
            authenticateOAuth2Async("realm", oauth2Authenticator) {
              auth =>
                entity(as[Account]) { objectToUpdate =>
                  onComplete(accountsDal.update(auth.user, matcher.asInstanceOf[Int].toLong, objectToUpdate)) {
                    case Success(resultOptChild) => resultOptChild match {
                      case 1 => complete(Accepted, BaseResponse(1, s"The object updated"))
                      case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't updated"))
                    }
                    case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                  }
                }
            }
          }
        } ~ delete {
          validate(matcher.isInstanceOf[Int], BaseResponse.objectFormat.write(BaseResponse(0, "The object id should be integer")).prettyPrint) {
            authenticateOAuth2Async("realm", oauth2Authenticator) {
              auth =>
                onComplete(accountsDal.delete(auth.user, matcher.asInstanceOf[Int].toLong)) {
                  case Success(resultOpt) => resultOpt match {
                    case 1 => complete(Accepted, BaseResponse(1, s"The object deleted"))
                    case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't deleted"))
                  }
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
            }
          }
        } ~ post {
          authenticateOAuth2Async("realm", oauth2Authenticator) {
            auth =>
              extractRequestContext { ctx =>
                implicit val materializer = ctx.materializer
                implicit val ec = ctx.executionContext

                fileUpload("file") {
                  case (metadata, byteSource) =>
                    val dir = new File("D://akka-tmp/accounts")
                    if (!dir.exists()) {
                      dir.mkdir()
                    }

                    val path = Paths.get("D://akka-tmp/accounts") resolve metadata.fileName
                    val sink = FileIO.toPath(path)
                    val writeResult = byteSource.runWith(sink)
                    onSuccess(writeResult) { result =>
                      result.status match {
                        case Success(_) => onComplete(accountsDal.update(
                          auth.user,
                          matcher.asInstanceOf[Int].toLong,
                          Account(avatar = Some(path.toString)))) {
                          case Success(resultOptChild) => resultOptChild match {
                            case 1 => complete(Accepted, BaseResponse(1, s"The object updated"))
                            case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't updated"))
                          }
                          case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                        }
                        case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                      }
                    }
                }
              }
          }
        }
      }
    }
  }
}
