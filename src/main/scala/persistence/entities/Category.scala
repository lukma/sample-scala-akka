package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 10/27/2016.
  */
case class Category(id: Option[Long] = Some(0),
                    title: Option[String] = None,
                    slug: Option[String] = None,
                    image: Option[String] = None,
                    parentId: Option[Long] = Some(0),
                    ownerId: Option[Long] = None,
                    createdAt: Option[Timestamp] = None,
                    updatedAt: Option[Timestamp] = None) extends BaseEntity

object Category {

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

  implicit val objectFormat: RootJsonFormat[Category] = jsonFormat8(Category.apply)
}

case class CategoriesResponse(status: Int, data: Seq[Category], recordsTotal: Int, recordsFiltered: Int)

object CategoriesResponse {
  implicit val objectFormat: RootJsonFormat[CategoriesResponse] = jsonFormat4(CategoriesResponse.apply)
}

case class CategoryChildItem(id: Long, title: String, slug: String, image: String)

object CategoryChildItem {
  implicit val objectFormat: RootJsonFormat[CategoryChildItem] = jsonFormat4(CategoryChildItem.apply)
}
