package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 12/19/2016.
  */
case class Permission(id: Option[Long] = Some(0),
                      title: Option[String],
                      privilege: Option[String],
                      parentId: Option[Long] = Some(0),
                      createdAt: Option[Timestamp],
                      updatedAt: Option[Timestamp]) extends BaseEntity

object Permission {

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

  implicit val objectFormat: RootJsonFormat[Permission] = jsonFormat6(Permission.apply)
}

case class PermissionsResponse(status: Int, data: Seq[Permission], recordsTotal: Int, recordsFiltered: Int)

object PermissionsResponse {
  implicit val objectFormat: RootJsonFormat[PermissionsResponse] = jsonFormat4(PermissionsResponse.apply)
}

case class PermissionChildItem(id: Long, title: String)

object PermissionChildItem {
  implicit val objectFormat: RootJsonFormat[PermissionChildItem] = jsonFormat2(PermissionChildItem.apply)
}
