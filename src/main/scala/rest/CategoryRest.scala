package rest

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.FileIO
import persistence.dals.CategoriesDal
import persistence.entities.{CategoriesResponse, Category, BaseResponse}

import scala.util.{Failure, Success}

/**
  * Created by Gayo on 11/23/2016.
  */
trait CategoryRest extends Directives with BaseRest {
  val categoriesDal: CategoriesDal

  def categoryRoute: Route = pathPrefix("categories") {
    pathEndOrSingleSlash {
      get {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            parameters('filter ? "", 'parentId.as[Int].?, 'limit.as[Int] ? 10, 'offset.as[Int] ? 0, 'sortBy ? "id", 'sortType ? "asc") {
              (filter, parentId, limit, offset, sortBy, sortType) =>
                onComplete(categoriesDal.finds(auth.user, filter, parentId, limit, offset, sortBy, sortType)) {
                  case Success(result) => complete(CategoriesResponse(1, result._1, result._2, result._3))
                  case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
                }
            }
        }
      } ~ post {
        authenticateOAuth2Async("realm", oauth2Authenticator) {
          auth =>
            entity(as[Category]) { objectToInsert =>
              onComplete(categoriesDal.insert(auth.user, objectToInsert)) {
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
              entity(as[Category]) { objectToUpdate =>
                onComplete(categoriesDal.update(auth.user, matcher.toLong, objectToUpdate)) {
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
              onComplete(categoriesDal.delete(auth.user, matcher.toLong)) {
                case Success(resultOpt) => resultOpt match {
                  case 1 => complete(Accepted, BaseResponse(1, s"The object deleted"))
                  case 0 => complete(BadRequest, BaseResponse(0, s"The object doesn't deleted"))
                }
                case Failure(ex) => complete(InternalServerError, BaseResponse(0, s"An error occurred: ${ex.getMessage}"))
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
                    val dir = new File("D://akka-tmp/categories")
                    if (!dir.exists()) {
                      dir.mkdir()
                    }

                    val path = Paths.get("D://akka-tmp/categories") resolve metadata.fileName
                    val sink = FileIO.toPath(path)
                    val writeResult = byteSource.runWith(sink)
                    onSuccess(writeResult) { result =>
                      result.status match {
                        case Success(_) =>
                          onComplete(categoriesDal.update(auth.user, matcher.toLong, Category(image = Some(path.toString)))) {
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
