package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.{Account, Permission, RolePermission}
import persistence.entities.SlickTables.PermissionsTable
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 12/19/2016.
  */
trait PermissionsDal extends BaseDalImpl[PermissionsTable, Permission] {
  def finds(user: Account, filter: String, limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[Permission], Int, Int)]

  def insert(user: Account, objectToInsert: Permission): Future[Long]

  def insert(user: Account, roleId: Option[Long], objectToInsert: Seq[Permission]): Future[Unit]

  def delete(user: Account, objectId: Long): Future[Int]
}

class PermissionsDalImpl(modules: Configuration with PersistenceModule)(implicit override val db: JdbcProfile#Backend#Database) extends PermissionsDal {
  override def finds(user: Account, filter: String, limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[Permission], Int, Int)] = {
    val filterQuery: (PermissionsTable) => Rep[Option[Boolean]] = _.title like s"%$filter%"

    for {
      findPermissions <- findByFilter(filterQuery, limit, offset, permission => sortType match {
        case "asc" => sortBy match {
          case "title" => permission.title.asc
          case "createdAt" => permission.createdAt.asc
          case "updatedAt" => permission.updatedAt.asc
          case _ => permission.id.asc
        }
        case "desc" => sortBy match {
          case "title" => permission.title.desc
          case "createdAt" => permission.createdAt.desc
          case "updatedAt" => permission.updatedAt.desc
          case _ => permission.id.desc
        }
        case _ => permission.id.asc
      }
      )
      recordsTotal <- countAllRow()
      recordsFiltered <- if (!filter.isEmpty) {
        countFilteredRow(filterQuery)
      } else {
        Future {
          recordsTotal
        }
      }
    } yield (findPermissions, recordsTotal, recordsFiltered)
  }

  override def insert(user: Account, objectToInsert: Permission): Future[Long] = {
    insert(Seq(objectToInsert.copy(
      privilege = Some(objectToInsert.title.get.toLowerCase.replace(" ", "-")),
      createdAt = Some(new Timestamp(new DateTime().getMillis)))
    )).map(_.head)
  }

  override def insert(user: Account, roleId: Option[Long], objectToInserts: Seq[Permission]): Future[Unit] = {
    val rows = objectToInserts.map {
      permission =>
        permission.copy(
          privilege = Some(permission.title.get.toLowerCase.replace(" ", "-")),
          createdAt = Some(new Timestamp(new DateTime().getMillis)))
    }

    db.run(
      DBIO.seq(rows.filter(_.isValid).map {
        row =>
          tableQ.filter(permission => permission.title === row.title && permission.title === row.title).result.headOption
            .map {
              case Some(permission) => modules.rolePermissionsDal.insert(RolePermission(Some(0), roleId, permission.id, Some(new Timestamp(new DateTime().getMillis))))
              case None => (tableQ returning tableQ.map(_.id) += row)
                .map(id => modules.rolePermissionsDal.insert(RolePermission(Some(0), roleId, Some(id), Some(new Timestamp(new DateTime().getMillis)))))
            }
      }: _*)
    )
  }

  override def delete(user: Account, objectId: Long): Future[Int] = {
    deleteById(objectId)
  }
}
