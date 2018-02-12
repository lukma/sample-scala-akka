package persistence.dals

import persistence.entities.RolePermission
import persistence.entities.SlickTables.RolePermissionsTable
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

/**
  * Created by Gayo on 12/19/2016.
  */
trait RolePermissionsDal extends BaseDalImpl[RolePermissionsTable, RolePermission] {
  def delete(roleId: Option[Long], ids: Seq[Long]): Future[Int]
}

class RolePermissionsDalImpl()(implicit override val db: JdbcProfile#Backend#Database) extends RolePermissionsDal {
  override def delete(postId: Option[Long], ids: Seq[Long]): Future[Int] = {
    db.run(tableQ.filter(rolePermissions => rolePermissions.roleId === postId && rolePermissions.permissionId.inSet(ids)).delete)
  }
}
