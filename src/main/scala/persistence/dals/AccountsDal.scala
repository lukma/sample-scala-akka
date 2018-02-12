package persistence.dals

import java.security.MessageDigest
import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.SlickTables.AccountsTable
import persistence.entities._
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/27/2016.
  */
trait AccountsDal extends BaseDalImpl[AccountsTable, Account] {
  def authenticate(username: String, password: String): Future[Option[Account]]

  def find(user: Account, matcher: Any): Future[Option[AccountItem]]

  def finds(user: Account, filter: String, limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[AccountItem], Int, Int)]

  def insert(user: Account, objectToInsert: Account): Future[Long]

  def update(user: Account, objectId: Long, objectToUpdate: Account): Future[Long]

  def delete(user: Account, objectId: Long): Future[Int]
}

class AccountsDalImpl(modules: Configuration with PersistenceModule)(implicit override val db: JdbcProfile#Backend#Database) extends AccountsDal {
  private def digestString(s: String): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(s.getBytes)
    md.digest.foldLeft("") { (s, b) =>
      s + "%02x".format(if (b < 0) b + 256 else b)
    }
  }

  def authenticate(username: String, password: String): Future[Option[Account]] = {
    val hashedPassword = digestString(password)
    findByFilter(account => account.password === hashedPassword && account.username === username).map(_.headOption)
  }

  override def find(user: Account, matcher: Any): Future[Option[AccountItem]] = {
    val data = for {
      account <- tableQ withFilter (matcher match {
        case id: Int => _.id === id.toLong
        case username: String => _.username === username
      })
      role <- account.role
    } yield (account, role)

    db.run(data.to[List].result).map {
      _.map {
        case (account, role) =>
          val roleItem = RoleChildItem(role.id.get, role.title.get)
          val emailItem = if (account.id == user.id) account.email else None
          val phoneItem = if (account.id == user.id) account.phone else None

          AccountItem(
            account.id.get,
            account.username.get,
            roleItem,
            emailItem,
            phoneItem,
            account.firstName.get,
            account.lastName.get,
            account.avatar.get,
            account.birthDate.get,
            account.gender.get,
            account.about.get,
            account.isActive.get,
            account.isVerified.get,
            account.createdAt.get,
            account.updatedAt.orNull
          )
      }.headOption
    }
  }

  override def finds(user: Account, filter: String, limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[AccountItem], Int, Int)] = {
    val filterQuery: (AccountsTable) => Rep[Option[Boolean]] = account => (account.id =!= user.id) && ((account.username like s"%$filter%")
      || (account.email like s"%$filter%") || (account.phone like s"%$filter%") || (account.firstName like s"%$filter%")
      || (account.lastName like s"%$filter%"))

    for {
      findAccounts <- {
        val data = (for {
          account <- tableQ withFilter filterQuery drop offset take limit
          role <- account.role
        } yield (account, role)).sortBy(sortType match {
          case "asc" => sortBy match {
            case "username" => _._1.username.asc
            case "email" => _._1.email.asc
            case "phone" => _._1.phone.asc
            case "firstName" => _._1.firstName.asc
            case "lastName" => _._1.lastName.asc
            case "birthDate" => _._1.birthDate.asc
            case "createdAt" => _._1.createdAt.asc
            case "updatedAt" => _._1.updatedAt.asc
            case _ => _._1.id.asc
          }
          case "desc" => sortBy match {
            case "username" => _._1.username.desc
            case "email" => _._1.email.desc
            case "phone" => _._1.phone.desc
            case "firstName" => _._1.firstName.desc
            case "lastName" => _._1.lastName.desc
            case "birthDate" => _._1.birthDate.desc
            case "createdAt" => _._1.createdAt.desc
            case "updatedAt" => _._1.updatedAt.desc
            case _ => _._1.id.desc
          }
          case _ => _._1.id.asc
        })

        db.run(data.to[List].result).map {
          _.map {
            case (account, role) =>
              val roleItem = RoleChildItem(role.id.get, role.title.get)
              val emailItem = if (account.roleId.get == 1L) account.email else None
              val phoneItem = if (account.roleId.get == 1L) account.phone else None

              AccountItem(
                account.id.get,
                account.username.get,
                roleItem,
                emailItem,
                phoneItem,
                account.firstName.get,
                account.lastName.get,
                account.avatar.get,
                account.birthDate.get,
                account.gender.get,
                account.about.get,
                account.isActive.get,
                account.isVerified.get,
                account.createdAt.get,
                account.updatedAt.orNull
              )
          }.toSeq
        }
      }
      recordsTotal <- countFilteredRow(_.id =!= user.id)
      recordsFiltered <- if (!filter.isEmpty) {
        countFilteredRow(filterQuery)
      } else {
        Future {
          recordsTotal
        }
      }
    } yield (findAccounts, recordsTotal, recordsFiltered)
  }

  override def insert(user: Account, objectToInsert: Account): Future[Long] = {
    for {
      insertAccount <- insert(objectToInsert.copy(
        password = Some(digestString(objectToInsert.password.get)),
        avatar = Some("/assets/images/icon-default-account.png"),
        isActive = Some(true),
        isVerified = Some(false),
        createdAt = Some(new Timestamp(new DateTime().getMillis))))
      _ <- modules.oauthClientsDal.insert(OAuthClient(
        Some(0),
        insertAccount,
        "client_credentials",
        objectToInsert.username.get + "_client_id",
        objectToInsert.username.get + "_client_secret",
        Some("redirectUrl"),
        Some(new Timestamp(new DateTime().getMillis))))
    } yield insertAccount
  }

  override def update(user: Account, objectId: Long, objectToUpdate: Account): Future[Long] = {
    for {
      findAccount <- findByMatcher(account => account.id === user.id && account.id === objectId)
      updateAccount <- findAccount match {
        case Some(account) => update(account.copy(
          firstName = objectToUpdate.firstName.orElse(account.firstName),
          lastName = objectToUpdate.lastName.orElse(account.lastName),
          avatar = objectToUpdate.avatar.orElse(account.avatar),
          birthDate = objectToUpdate.birthDate.orElse(account.birthDate),
          gender = objectToUpdate.gender.orElse(account.gender),
          about = objectToUpdate.about.orElse(account.about),
          updatedAt = Some(new Timestamp(new DateTime().getMillis))
        ))
        case None => Future {
          0
        }
      }
    } yield updateAccount
  }

  override def delete(user: Account, objectId: Long): Future[Int] = {
    deleteByFilter(account => account.id === user.id && account.id === objectId)
  }
}