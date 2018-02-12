package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 10/27/2016.
  */
case class Role(id: Option[Long] = Some(0),
                title: Option[String] = None,
                parentId: Option[Long] = Some(0),
                createdAt: Option[Timestamp] = None,
                updatedAt: Option[Timestamp] = None) extends BaseEntity

case class RoleItem(id: Long,
                    title: String,
                    parentId: Long,
                    permissions: Seq[PermissionChildItem],
                    createdAt: Timestamp,
                    updatedAt: Timestamp)

object RoleItem {

  implicit object TimestampFormat extends RootJsonFormat[Timestamp] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def write(obj: Timestamp): JsValue = if (obj != null) {
      JsString(format.format(obj.getTime))
    } else {
      JsNull
    }

    def read(json: JsValue): Timestamp = json match {
      case JsString(s) => new Timestamp(format.parse(s).getTime)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val objectFormat: RootJsonFormat[RoleItem] = jsonFormat6(RoleItem.apply)
}

case class RolesResponse(status: Int, data: Seq[RoleItem], recordsTotal: Int, recordsFiltered: Int)

object RolesResponse {
  implicit val objectFormat: RootJsonFormat[RolesResponse] = jsonFormat4(RolesResponse.apply)
}

case class RoleChildItem(id: Long, title: String)

object RoleChildItem {
  implicit val objectFormat: RootJsonFormat[RoleChildItem] = jsonFormat2(RoleChildItem.apply)
}

case class RoleForm(title: Option[String],
                    parentId: Option[Long],
                    permissionsToInsert: Option[Seq[Permission]],
                    permissionsToDelete: Option[Seq[Permission]])

object RoleForm {
  implicit val objectFormat: RootJsonFormat[RoleForm] = jsonFormat4(RoleForm.apply)
}
