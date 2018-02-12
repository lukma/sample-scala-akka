package persistence.dals

import persistence.entities.PostTopic
import persistence.entities.SlickTables.PostTopicsTable
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

/**
  * Created by Gayo on 10/30/2016.
  */
trait PostTopicsDal extends BaseDalImpl[PostTopicsTable, PostTopic] {
  def delete(postId: Option[Long], ids: Seq[Long]): Future[Int]
}

class PostTopicsDalImpl()(implicit override val db: JdbcProfile#Backend#Database) extends PostTopicsDal {
  override def delete(postId: Option[Long], ids: Seq[Long]): Future[Int] = {
    db.run(tableQ.filter(postTopics => postTopics.postId === postId && postTopics.topicId.inSet(ids)).delete)
  }
}
