package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 10/27/2016.
  */
case class Comment(id: Option[Long] = Some(0),
                   content: Option[String] = None,
                   parentId: Option[Long] = Some(0),
                   postId: Option[Long] = None,
                   ownerId: Option[Long] = None,
                   createdAt: Option[Timestamp] = None,
                   updatedAt: Option[Timestamp] = None) extends BaseEntity

object Comment {

  implicit object TimestampFormat extends RootJsonFormat[Timestamp] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def write(obj: Timestamp) = JsString(format.format(obj.getTime))

    def read(json: JsValue): Timestamp = json match {
      case JsString(s) => new Timestamp(format.parse(s).getTime)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val objectFormat: RootJsonFormat[Comment] = jsonFormat7(Comment.apply)
}

case class CommentItem(id: Long,
                       content: String,
                       parentId: Long,
                       owner: AccountChildItem,
                       createdAt: Timestamp,
                       updatedAt: Timestamp)

object CommentItem {

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

  implicit val objectFormat: RootJsonFormat[CommentItem] = jsonFormat6(CommentItem.apply)
}

case class CommentsResponse(status: Int, data: Seq[CommentItem], recordsTotal: Int, recordsFiltered: Int)

object CommentsResponse {
  implicit val objectFormat: RootJsonFormat[CommentsResponse] = jsonFormat4(CommentsResponse.apply)
}
