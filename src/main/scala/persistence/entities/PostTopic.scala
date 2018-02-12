package persistence.entities

import java.sql.Timestamp

/**
  * Created by Gayo on 10/30/2016.
  */
case class PostTopic(id: Option[Long] = Some(0),
                     postId: Option[Long],
                     topicId: Option[Long],
                     createdAt: Option[Timestamp]) extends BaseEntity
