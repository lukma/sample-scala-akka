package rest

import akka.http.scaladsl.server._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import persistence.dals._
import persistence.entities.Account
import persistence.handlers.AuthDataHandler
import utils.{Configuration, PersistenceModule}

import scalaoauth2.provider.DataHandler

/**
  * Created by Gayo on 11/23/2016.
  */
class MainRest(val modules: Configuration with PersistenceModule) extends Directives
  with BaseRest
  with AuthRest
  with AccountRest
  with PermissionRest
  with RoleRest
  with CategoryRest
  with PostRest
  with TopicRest
  with CommentRest {

  override val oauth2DataHandler: DataHandler[Account] = modules.oauth2DataHandler
  override val authDataHandler: AuthDataHandler = modules.authDataHandler
  override val permissionsDal: PermissionsDal = modules.permissionsDal
  override val rolesDal: RolesDal = modules.rolesDal
  override val accountsDal: AccountsDal = modules.accountsDal
  override val categoriesDal: CategoriesDal = modules.categoriesDal
  override val postsDal: PostsDal = modules.postsDal
  override val topicsDal: TopicsDal = modules.topicsDal
  override val commentsDal: CommentsDal = modules.commentsDal

  val routes: Route = cors() {
    pathPrefix("api") {
      accessTokenRoute ~ permissionRoute ~ roleRoute ~ accountRoute ~ categoryRoute ~ postRoute ~ topicRoute ~ commentRoute
    } ~ pathPrefix("tmp") {
      getFromBrowseableDirectory("D://akka-tmp")
    }
  }
}
