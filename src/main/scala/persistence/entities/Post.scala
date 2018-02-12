package persistence.entities

import java.sql.Timestamp

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 10/27/2016.
  */
case class Post(id: Option[Long] = Some(0),
                title: Option[String] = None,
                slug: Option[String] = None,
                thumbnail: Option[String] = None,
                categoryId: Option[Long] = None,
                content: Option[String] = None,
                isPrivate: Option[Boolean] = None,
                isPublish: Option[Boolean] = None,
                ownerId: Option[Long] = None,
                createdAt: Option[Timestamp] = None,
                updatedAt: Option[Timestamp] = None) extends BaseEntity

case class PostItem(id: Long,
                    title: String,
                    slug: String,
                    thumbnail: String,
                    category: CategoryChildItem,
                    content: String,
                    topics: Seq[TopicChildItem],
                    isPrivate: Boolean,
                    isPublish: Boolean,
                    owner: AccountChildItem,
                    createdAt: Timestamp,
                    updatedAt: Timestamp)

object PostItem {

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

  implicit val objectFormat: RootJsonFormat[PostItem] = jsonFormat12(PostItem.apply)
}

case class PostResponse(status: Int, data: PostItem)

object PostResponse {
  implicit val objectFormat: RootJsonFormat[PostResponse] = jsonFormat2(PostResponse.apply)
}

case class PostsResponse(status: Int, data: Seq[PostItem], recordsTotal: Int, recordsFiltered: Int)

object PostsResponse {
  implicit val objectFormat: RootJsonFormat[PostsResponse] = jsonFormat4(PostsResponse.apply)
}

case class PostForm(title: Option[String],
                    thumbnail: Option[String],
                    categoryId: Option[Long],
                    content: Option[String],
                    topicsToInsert: Option[Seq[Topic]],
                    topicsToDelete: Option[Seq[Topic]],
                    isPrivate: Option[Boolean],
                    isPublish: Option[Boolean])

object PostForm {
  implicit val objectFormat: RootJsonFormat[PostForm] = jsonFormat8(PostForm.apply)
}
