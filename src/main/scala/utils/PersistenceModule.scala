package utils

import java.sql.{Date, Timestamp}

import org.joda.time.DateTime
import persistence.dals._
import persistence.entities._
import persistence.handlers.{AuthDataHandler, AuthDataHandlerImpl, OAuth2DataHandler}
import persistence.types.GenderEnum
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scalaoauth2.provider.DataHandler

/**
  * Created by Gayo on 10/27/2016.
  */
trait Profile {
  val profile: JdbcProfile
}

trait DbModule extends Profile {
  val db: JdbcProfile#Backend#Database
}

trait PersistenceModule {
  val permissionsDal: PermissionsDal
  val rolesDal: RolesDal
  val rolePermissionsDal: RolePermissionsDal
  val accountsDal: AccountsDal
  val oauthClientsDal: OAuthClientsDal
  val oauthAuthorizationCodesDal: OAuthAuthorizationCodesDal
  val oauthAccessTokensDal: OAuthAccessTokensDal
  val oauth2DataHandler: DataHandler[Account]
  val authDataHandler: AuthDataHandler
  val categoriesDal: CategoriesDal
  val postsDal: PostsDal
  val topicsDal: TopicsDal
  val postTopicsDal: PostTopicsDal
  val commentsDal: CommentsDal

  def generateDDL(): Unit

  def generateData(): Unit
}

trait PersistenceModuleImpl extends PersistenceModule with DbModule {
  this: Configuration =>

  private val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("pgdb")

  override implicit val profile: JdbcProfile = dbConfig.profile
  override implicit val db: JdbcProfile#Backend#Database = dbConfig.db

  override val permissionsDal = new PermissionsDalImpl(this)
  override val rolesDal = new RolesDalImpl(this)
  override val rolePermissionsDal = new RolePermissionsDalImpl
  override val accountsDal = new AccountsDalImpl(this)
  override val oauthClientsDal = new OAuthClientsDalImpl(this)
  override val oauthAuthorizationCodesDal = new OAuthAuthorizationCodesDalImpl
  override val oauthAccessTokensDal = new OAuthAccessTokensDalImpl(this)
  override val oauth2DataHandler = new OAuth2DataHandler(this)
  override val authDataHandler = new AuthDataHandlerImpl(this)
  override val categoriesDal = new CategoriesDalImpl
  override val postsDal = new PostsDalImpl(this)
  override val topicsDal = new TopicsDalImpl(this)
  override val postTopicsDal = new PostTopicsDalImpl
  override val commentsDal = new CommentsDalImpl

  override def generateDDL(): Unit = {
    for {
      _ <- permissionsDal.createTable()
      _ <- rolesDal.createTable()
      _ <- rolePermissionsDal.createTable()
      _ <- accountsDal.createTable()
      _ <- oauthClientsDal.createTable()
      _ <- oauthAuthorizationCodesDal.createTable()
      _ <- oauthAccessTokensDal.createTable()
      _ <- categoriesDal.createTable()
      _ <- postsDal.createTable()
      _ <- topicsDal.createTable()
      _ <- postTopicsDal.createTable()
      _ <- commentsDal.createTable()
    } yield {
      println(s"Database initialized with default schema for developer")
    }
  }

  override def generateData(): Unit = {
    for {
      _ <- permissionsDal.insert(Seq(
        Permission(Some(1), Some("Master"), Some("master"), Some(0), Some(new Timestamp(new DateTime().getMillis)), None)
      ))

      _ <- rolesDal.insert(Seq(
        Role(Some(1), Some("Master"), Some(0), Some(new Timestamp(new DateTime().getMillis)), None),
        Role(Some(1), Some("Admin"), Some(1), Some(new Timestamp(new DateTime().getMillis)), None),
        Role(Some(1), Some("Member"), Some(1), Some(new Timestamp(new DateTime().getMillis)), None)
      ))

      _ <- rolePermissionsDal.insert(Seq(
        RolePermission(Some(0), Some(1), Some(1), Some(new Timestamp(new DateTime().getMillis)))
      ))

      _ <- accountsDal.insert(Seq(
        Account(Some(1), Some("root"), Some("48181acd22b3edaebc8a447868a7df7ce629920a"), Some(1), Some("root@rockid.io"), Some("+6287700568354"), Some("rockid"), Some("root"), Some("https://yt3.ggpht.com/-Fq_uiT7y10k/AAAAAAAAAAI/AAAAAAAAAAA/YzIf8sfr7sI/s88-c-k-no-mo-rj-c0xffffff/photo.jpg"), Some(new Date(new DateTime().getMillis)), Some(GenderEnum.male), Some("lorem ipsum"), Some(true), Some(true), Some(new Timestamp(new DateTime().getMillis)), None),
        Account(Some(2), Some("developer"), Some("48181acd22b3edaebc8a447868a7df7ce629920a"), Some(2), Some("developer@rockid.io"), Some("+6287700568355"), Some("rockid"), Some("developer"), Some("https://yt3.ggpht.com/-Fq_uiT7y10k/AAAAAAAAAAI/AAAAAAAAAAA/YzIf8sfr7sI/s88-c-k-no-mo-rj-c0xffffff/photo.jpg"), Some(new Date(new DateTime().getMillis)), Some(GenderEnum.female), Some("lorem ipsum"), Some(true), Some(true), Some(new Timestamp(new DateTime().getMillis)), None)
      ))

      _ <- oauthClientsDal.insert(Seq(
        OAuthClient(Some(1), 1, "client_credentials", "root_client_id", "root_client_secret", Some("redirectUrl"), Some(new Timestamp(new DateTime().getMillis))),
        OAuthClient(Some(2), 2, "client_credentials", "developer_client_id", "developer_client_secret", Some("redirectUrl"), Some(new Timestamp(new DateTime().getMillis)))
      ))

      _ <- categoriesDal.insert(Seq(
        Category(Some(1), Some("Master"), Some("master"), Some("https://yt3.ggpht.com/-Fq_uiT7y10k/AAAAAAAAAAI/AAAAAAAAAAA/YzIf8sfr7sI/s88-c-k-no-mo-rj-c0xffffff/photo.jpg"), Some(0), Some(1), Some(new Timestamp(new DateTime().getMillis)), None),
        Category(Some(2), Some("General"), Some("general"), Some("https://yt3.ggpht.com/-Fq_uiT7y10k/AAAAAAAAAAI/AAAAAAAAAAA/YzIf8sfr7sI/s88-c-k-no-mo-rj-c0xffffff/photo.jpg"), Some(1), Some(1), Some(new Timestamp(new DateTime().getMillis)), None)
      ))

      _ <- postsDal.insert(Seq(
        Post(Some(0), Some("First Post"), Some("first-post"), Some("https://yt3.ggpht.com/-Fq_uiT7y10k/AAAAAAAAAAI/AAAAAAAAAAA/YzIf8sfr7sI/s88-c-k-no-mo-rj-c0xffffff/photo.jpg"), Some(1), Some("Hello World"), Some(false), Some(true), Some(1), Some(new Timestamp(new DateTime().getMillis)), None),
        Post(Some(0), Some("Second Post"), Some("second-post"), Some("https://yt3.ggpht.com/-Fq_uiT7y10k/AAAAAAAAAAI/AAAAAAAAAAA/YzIf8sfr7sI/s88-c-k-no-mo-rj-c0xffffff/photo.jpg"), Some(1), Some("Hello World"), Some(false), Some(true), Some(1), Some(new Timestamp(new DateTime().getMillis)), None),
        Post(Some(0), Some("Third Post"), Some("third-post"), Some("https://yt3.ggpht.com/-Fq_uiT7y10k/AAAAAAAAAAI/AAAAAAAAAAA/YzIf8sfr7sI/s88-c-k-no-mo-rj-c0xffffff/photo.jpg"), Some(1), Some("Hello World"), Some(false), Some(true), Some(1), Some(new Timestamp(new DateTime().getMillis)), None)
      ))

      _ <- topicsDal.insert(Seq(
        Topic(Some(0), Some("Sample"), Some("sample"), Some(1), Some(new Timestamp(new DateTime().getMillis))),
        Topic(Some(0), Some("Test"), Some("test"), Some(1), Some(new Timestamp(new DateTime().getMillis)))
      ))

      _ <- postTopicsDal.insert(Seq(
        PostTopic(Some(0), Some(1), Some(1), Some(new Timestamp(new DateTime().getMillis))),
        PostTopic(Some(0), Some(1), Some(2), Some(new Timestamp(new DateTime().getMillis)))
      ))

      _ <- commentsDal.insert(Seq(
        Comment(Some(0), Some("Hello World"), Some(0), Some(1), Some(1), Some(new Timestamp(new DateTime().getMillis)), None)
      ))
    } yield {
      println(s"Database initialized with default values for developer")
    }
  }
}