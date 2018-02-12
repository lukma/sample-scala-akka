package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.SlickTables.{PermissionsTable, RolePermissionsTable, RolesTable}
import persistence.entities._
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep
import utils.PimpedSeq._
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/29/2016.
  */
trait RolesDal extends BaseDalImpl[RolesTable, Role] {
  def finds(user: Account, filter: String, parentId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[RoleItem], Int, Int)]

  def insert(user: Account, objectToInsert: Role, permissionsToInsert: Seq[Permission]): Future[Long]

  def update(user: Account, objectId: Long, objectToUpdate: Role, permissionsToInsert: Seq[Permission], permissionsToDelete: Seq[Permission]): Future[Long]

  def delete(user: Account, objectId: Long): Future[Int]
}

class RolesDalImpl(modules: Configuration with PersistenceModule)(implicit override val db: JdbcProfile#Backend#Database) extends RolesDal {
  override def finds(user: Account, filter: String, parentId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[RoleItem], Int, Int)] = {
    val filterQuery: (RolesTable) => Rep[Option[Boolean]] = role => parentId match {
      case Some(value) => (role.title like s"%$filter%") && (role.parentId === value.toLong)
      case _ => role.title like s"%$filter%"
    }

    for {
      findRoles <- {
        val data = (for {
          ((role, _), permission) <- {
            tableQ withFilter filterQuery drop offset take limit joinLeft TableQuery[RolePermissionsTable] on (_.id === _.roleId) joinLeft TableQuery[PermissionsTable] on (_._2.map(_.permissionId) === _.id)
          }
        } yield (role, permission)).sortBy(sortType match {
          case "asc" => sortBy match {
            case "title" => _._1.title.asc
            case "createdAt" => _._1.createdAt.asc
            case "updatedAt" => _._1.updatedAt.asc
            case _ => _._1.id.asc
          }
          case "desc" => sortBy match {
            case "title" => _._1.title.desc
            case "createdAt" => _._1.createdAt.desc
            case "updatedAt" => _._1.updatedAt.desc
            case _ => _._1.id.desc
          }
          case _ => _._1.id.asc
        })

        db.run(data.to[List].result).map {
          _.groupConsecutiveKeys(_._1)
            .map {
              case (role, combination) =>
                val permissionItems = combination.map(_._2.orNull).filter(r => r != null).map {
                  topic => PermissionChildItem(topic.id.get, topic.title.get)
                }

                RoleItem(
                  role.id.get,
                  role.title.get,
                  role.parentId.get,
                  permissionItems,
                  role.createdAt.get,
                  role.updatedAt.orNull
                )
            }.toSeq
        }
      }
      recordsTotal <- countAllRow()
      recordsFiltered <- if (!filter.isEmpty) {
        countFilteredRow(filterQuery)
      } else {
        Future {
          recordsTotal
        }
      }
    } yield (findRoles, recordsTotal, recordsFiltered)
  }

  override def insert(user: Account, objectToInsert: Role, permissionsToInsert: Seq[Permission]): Future[Long] = {
    for {
      createRole <- insert(objectToInsert.copy(createdAt = Some(new Timestamp(new DateTime().getMillis))))
      _ <- modules.permissionsDal.insert(user, Some(createRole), permissionsToInsert)
    } yield createRole
  }

  override def update(user: Account, objectId: Long, objectToUpdate: Role, permissionsToInsert: Seq[Permission], permissionsToDelete: Seq[Permission]): Future[Long] = {
    for {
      findRole <- findById(objectId)
      updateRole <- findRole match {
        case Some(role) => update(role.copy(
          title = objectToUpdate.title.orElse(role.title),
          updatedAt = Some(new Timestamp(new DateTime().getMillis))
        ))
        case None => Future {
          0
        }
      }
      _ <- modules.permissionsDal.insert(user, findRole.get.id, permissionsToInsert)
      _ <- modules.postTopicsDal.delete(findRole.get.id, permissionsToDelete.map {
        permission => permission.id.get
      })
    } yield updateRole
  }

  override def delete(user: Account, objectId: Long): Future[Int] = {
    deleteById(objectId)
  }
}
