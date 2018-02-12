package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 10/30/2016.
  */
case class Topic(id: Option[Long] = Some(0),
                 title: Option[String] = None,
                 slug: Option[String] = None,
                 ownerId: Option[Long],
                 createdAt: Option[Timestamp] = None) extends BaseEntity

object Topic {

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

  implicit val objectFormat: RootJsonFormat[Topic] = jsonFormat5(Topic.apply)
}

case class TopicsResponse(status: Int, data: Seq[Topic], recordsTotal: Int, recordsFiltered: Int)

object TopicsResponse {
  implicit val objectFormat: RootJsonFormat[TopicsResponse] = jsonFormat4(TopicsResponse.apply)
}

case class TopicChildItem(id: Long, title: String, slug: String)

object TopicChildItem {
  implicit val objectFormat: RootJsonFormat[TopicChildItem] = jsonFormat3(TopicChildItem.apply)
}
