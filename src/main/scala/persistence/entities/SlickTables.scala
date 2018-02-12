package persistence.entities

import java.sql.{Date, Timestamp}

import persistence.types.GenderEnum
import persistence.types.GenderEnum.GenderEnum
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ForeignKeyQuery, Index, ProvenShape}

/**
  * Created by Gayo on 10/27/2016.
  */
object SlickTables {

  class PermissionsTable(tag: Tag) extends BaseTable[Permission](tag, "permissions") {
    def title: Rep[String] = column[String]("title", O.SqlType("VARCHAR(65535)"))

    def privilege: Rep[String] = column[String]("privilege")

    def parentId: Rep[Long] = column[Long]("parent_id", O.Default(0L))

    def updatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("updated_at", O.Default(None))

    def * : ProvenShape[Permission] = (id.?, title.?, privilege.?, parentId.?, createdAt, updatedAt) <> ((Permission.apply _).tupled, Permission.unapply)

    def idxTitlePermissions: Index = index("permission_idx_title", title, unique = true)
  }

  implicit val permissionsTableQ: TableQuery[PermissionsTable] = TableQuery[PermissionsTable]

  class RolesTable(tag: Tag) extends BaseTable[Role](tag, "roles") {
    def title: Rep[String] = column[String]("title", O.SqlType("VARCHAR(65535)"))

    def parentId: Rep[Long] = column[Long]("parent_id", O.Default(0L))

    def updatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("updated_at", O.Default(None))

    def * : ProvenShape[Role] = (id.?, title.?, parentId.?, createdAt, updatedAt) <> (Role.tupled, Role.unapply)

    def idxTitleRoles: Index = index("role_idx_title", title, unique = true)
  }

  implicit val rolesTableQ: TableQuery[RolesTable] = TableQuery[RolesTable]

  class RolePermissionsTable(tag: Tag) extends BaseTable[RolePermission](tag, "roles_permissions") {
    def roleId: Rep[Long] = column[Long]("role_id")

    def permissionId: Rep[Long] = column[Long]("permission_id")

    def * : ProvenShape[RolePermission] = (id.?, roleId.?, permissionId.?, createdAt) <> (RolePermission.tupled, RolePermission.unapply)

    def role: ForeignKeyQuery[RolesTable, Role] = foreignKey(
      "permission_role_fk",
      roleId,
      rolesTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def permission: ForeignKeyQuery[PermissionsTable, Permission] = foreignKey(
      "role_permission_fk",
      permissionId,
      permissionsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  }

  implicit val rolePermissionsTableQ: TableQuery[RolePermissionsTable] = TableQuery[RolePermissionsTable]

  class AccountsTable(tag: Tag) extends BaseTable[Account](tag, "accounts") {
    def username: Rep[String] = column[String]("username", O.SqlType("VARCHAR(65535)"))

    def password: Rep[String] = column[String]("password")

    def roleId: Rep[Long] = column[Long]("role_id")

    def email: Rep[String] = column[String]("email", O.SqlType("VARCHAR(65535)"))

    def phone: Rep[String] = column[String]("phone", O.SqlType("VARCHAR(65535)"))

    def firstName: Rep[String] = column[String]("first_name")

    def lastName: Rep[String] = column[String]("last_name")

    def avatar: Rep[String] = column[String]("avatar")

    def birthDate: Rep[Date] = column[Date]("birth_date")

    implicit val genderMapper: BaseColumnType[GenderEnum.GenderEnum] = MappedColumnType.base[GenderEnum, String](
      e => e.toString,
      s => GenderEnum.withName(s)
    )

    def gender: Rep[GenderEnum] = column[GenderEnum]("gender")

    def about: Rep[String] = column[String]("about")

    def isActive: Rep[Boolean] = column[Boolean]("is_active")

    def isVerified: Rep[Boolean] = column[Boolean]("is_verified")

    def updatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("updated_at", O.Default(None))

    def * : ProvenShape[Account] = (id.?, username.?, password.?, roleId.?, email.?, phone.?, firstName.?, lastName.?, avatar.?, birthDate.?, gender.?, about.?, isActive.?, isVerified.?, createdAt, updatedAt) <> ((Account.apply _).tupled, Account.unapply)

    def role: ForeignKeyQuery[RolesTable, Role] = foreignKey(
      "account_role_fk",
      roleId,
      rolesTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def idxUsernameAccounts: Index = index("account_idx_username", username, unique = true)

    def idxEmailAccounts: Index = index("account_idx_email", email, unique = true)

    def idxPhoneAccounts: Index = index("account_idx_phone", phone, unique = true)
  }

  implicit val accountsTableQ: TableQuery[AccountsTable] = TableQuery[AccountsTable]

  class OauthClientTable(tag: Tag) extends BaseTable[OAuthClient](tag, "oauth_clients") {
    def ownerId: Rep[Long] = column[Long]("owner_id")

    def grantType: Rep[String] = column[String]("grant_type")

    def clientId: Rep[String] = column[String]("client_id")

    def clientSecret: Rep[String] = column[String]("client_secret")

    def redirectUri: Rep[Option[String]] = column[Option[String]]("redirect_uri")

    def * : ProvenShape[OAuthClient] = (id.?, ownerId, grantType, clientId, clientSecret, redirectUri, createdAt) <> (OAuthClient.tupled, OAuthClient.unapply)

    def owner: ForeignKeyQuery[AccountsTable, Account] = foreignKey(
      "oauth_client_account_fk",
      ownerId,
      accountsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  }

  implicit val OauthClientTableQ: TableQuery[OauthClientTable] = TableQuery[OauthClientTable]

  class OauthAuthorizationCodeTable(tag: Tag) extends BaseTable[OAuthAuthorizationCode](tag, "oauth_authorization_codes") {
    def accountId: Rep[Long] = column[Long]("account_id")

    def oauthClientId: Rep[Long] = column[Long]("oauth_client_id")

    def code: Rep[String] = column[String]("code")

    def redirectUri: Rep[Option[String]] = column[Option[String]]("redirect_uri")

    def * : ProvenShape[OAuthAuthorizationCode] = (id.?, accountId, oauthClientId, code, redirectUri, createdAt) <> (OAuthAuthorizationCode.tupled, OAuthAuthorizationCode.unapply)

    def account: ForeignKeyQuery[AccountsTable, Account] = foreignKey(
      "oauth_authorization_code_account_fk",
      accountId,
      accountsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def oauthClient: ForeignKeyQuery[OauthClientTable, OAuthClient] = foreignKey(
      "oauth_authorization_code_client_fk",
      oauthClientId,
      OauthClientTableQ)(_.id)
  }

  implicit val OauthAuthorizationCodeTableQ: TableQuery[OauthAuthorizationCodeTable] = TableQuery[OauthAuthorizationCodeTable]

  class OauthAccessTokenTable(tag: Tag) extends BaseTable[OAuthAccessToken](tag, "oauth_access_tokens") {
    def accountId: Rep[Long] = column[Long]("account_id")

    def oauthClientId: Rep[Long] = column[Long]("oauth_client_id")

    def accessToken: Rep[String] = column[String]("access_token")

    def refreshToken: Rep[String] = column[String]("refresh_token")

    def * : ProvenShape[OAuthAccessToken] = (id.?, accountId, oauthClientId, accessToken, refreshToken, createdAt) <> (OAuthAccessToken.tupled, OAuthAccessToken.unapply)

    def account: ForeignKeyQuery[AccountsTable, Account] = foreignKey(
      "oauth_access_token_account_fk",
      accountId,
      accountsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def oauthClient: ForeignKeyQuery[OauthClientTable, OAuthClient] = foreignKey(
      "oauth_access_token_client_fk",
      oauthClientId,
      OauthClientTableQ)(_.id)
  }

  implicit val OauthAccessTokenTableQ: TableQuery[OauthAccessTokenTable] = TableQuery[OauthAccessTokenTable]

  class CategoriesTable(tag: Tag) extends BaseTable[Category](tag, "categories") {
    def title: Rep[String] = column[String]("title")

    def slug: Rep[String] = column[String]("slug", O.SqlType("VARCHAR(65535)"))

    def image: Rep[String] = column[String]("image")

    def parentId: Rep[Long] = column[Long]("parent_id", O.Default(0L))

    def ownerId: Rep[Long] = column[Long]("owner_id")

    def updatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("updated_at", O.Default(None))

    def * : ProvenShape[Category] = (id.?, title.?, slug.?, image.?, parentId.?, ownerId.?, createdAt, updatedAt) <> ((Category.apply _).tupled, Category.unapply)

    def owner: ForeignKeyQuery[AccountsTable, Account] = foreignKey(
      "category_account_fk",
      ownerId,
      accountsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def idxSlugCategory: Index = index("category_idx_slug", slug, unique = true)
  }

  implicit val categoriesTableQ: TableQuery[CategoriesTable] = TableQuery[CategoriesTable]

  class PostsTable(tag: Tag) extends BaseTable[Post](tag, "posts") {
    def title: Rep[String] = column[String]("title")

    def slug: Rep[String] = column[String]("slug", O.SqlType("VARCHAR(65535)"))

    def thumbnail: Rep[String] = column[String]("thumbnail")

    def categoryId: Rep[Long] = column[Long]("category_id")

    def content: Rep[String] = column[String]("content")

    def isPrivate: Rep[Boolean] = column[Boolean]("is_private")

    def isPublish: Rep[Boolean] = column[Boolean]("is_publish")

    def ownerId: Rep[Long] = column[Long]("owner_id")

    def updatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("updated_at", O.Default(None))

    def * : ProvenShape[Post] = (id.?, title.?, slug.?, thumbnail.?, categoryId.?, content.?, isPrivate.?, isPublish.?, ownerId.?, createdAt, updatedAt) <> (Post.tupled, Post.unapply)

    def category: ForeignKeyQuery[CategoriesTable, Category] = foreignKey(
      "post_category_fk",
      categoryId,
      categoriesTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def owner: ForeignKeyQuery[AccountsTable, Account] = foreignKey(
      "post_account_fk",
      ownerId,
      accountsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def idxSlugPost: Index = index("post_idx_slug", slug, unique = true)
  }

  implicit val postsTableQ: TableQuery[PostsTable] = TableQuery[PostsTable]

  class TopicsTable(tag: Tag) extends BaseTable[Topic](tag, "topics") {
    def title: Rep[String] = column[String]("title")

    def slug: Rep[String] = column[String]("slug", O.SqlType("VARCHAR(65535)"))

    def ownerId: Rep[Long] = column[Long]("owner_id")

    def * : ProvenShape[Topic] = (id.?, title.?, slug.?, ownerId.?, createdAt) <> ((Topic.apply _).tupled, Topic.unapply)

    def owner: ForeignKeyQuery[AccountsTable, Account] = foreignKey(
      "topic_account_fk",
      ownerId,
      accountsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def idxTitleTopic: Index = index("topic_idx_slug", slug, unique = true)
  }

  implicit val topicsTableQ: TableQuery[TopicsTable] = TableQuery[TopicsTable]

  class PostTopicsTable(tag: Tag) extends BaseTable[PostTopic](tag, "posts_topics") {
    def postId: Rep[Long] = column[Long]("post_id")

    def topicId: Rep[Long] = column[Long]("topic_id")

    def * : ProvenShape[PostTopic] = (id.?, postId.?, topicId.?, createdAt) <> (PostTopic.tupled, PostTopic.unapply)

    def post: ForeignKeyQuery[PostsTable, Post] = foreignKey(
      "topic_post_fk",
      postId,
      postsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def topic: ForeignKeyQuery[TopicsTable, Topic] = foreignKey(
      "post_topic_fk",
      topicId,
      topicsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  }

  implicit val postTopicsTableQ: TableQuery[PostTopicsTable] = TableQuery[PostTopicsTable]

  class CommentsTable(tag: Tag) extends BaseTable[Comment](tag, "comments") {
    def content: Rep[String] = column[String]("content")

    def parentId: Rep[Long] = column[Long]("parent_id", O.Default(0L))

    def postId: Rep[Long] = column[Long]("post_id")

    def ownerId: Rep[Long] = column[Long]("owner_id")

    def updatedAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("updated_at", O.Default(None))

    def * : ProvenShape[Comment] = (id.?, content.?, parentId.?, postId.?, ownerId.?, createdAt, updatedAt) <> ((Comment.apply _).tupled, Comment.unapply)

    def post: ForeignKeyQuery[PostsTable, Post] = foreignKey(
      "comment_post_fk",
      postId,
      postsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)

    def owner: ForeignKeyQuery[AccountsTable, Account] = foreignKey(
      "comment_account_fk",
      ownerId,
      accountsTableQ)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  }

  implicit val commentsTableQ: TableQuery[CommentsTable] = TableQuery[CommentsTable]
}