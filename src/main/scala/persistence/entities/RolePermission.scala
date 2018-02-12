package persistence.entities

import java.sql.Timestamp

/**
  * Created by Gayo on 12/19/2016.
  */
case class RolePermission(id: Option[Long] = Some(0),
                          roleId: Option[Long],
                          permissionId: Option[Long],
                          createdAt: Option[Timestamp]) extends BaseEntity
