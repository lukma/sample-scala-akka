package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.SlickTables.TopicsTable
import persistence.entities.{Account, PostTopic, Topic}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/30/2016.
  */
trait TopicsDal extends BaseDalImpl[TopicsTable, Topic] {
  def finds(user: Account, filter: String, limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[Topic], Int, Int)]

  def insert(user: Account, objectToInsert: Topic): Future[Long]

  def insert(user: Account, postId: Option[Long], objectToInsert: Seq[Topic]): Future[Unit]

  def delete(user: Account, objectId: Long): Future[Int]
}

class TopicsDalImpl(modules: Configuration with PersistenceModule)(implicit override val db: JdbcProfile#Backend#Database) extends TopicsDal {
  override def finds(user: Account, filter: String, limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[Topic], Int, Int)] = {
    val filterQuery: (TopicsTable) => Rep[Option[Boolean]] = _.title like s"%$filter%"

    for {
      findTopics <- findByFilter(filterQuery, limit, offset, topic => sortType match {
        case "asc" => sortBy match {
          case "title" => topic.title.asc
          case "createdAt" => topic.createdAt.asc
          case _ => topic.id.asc
        }
        case "desc" => sortBy match {
          case "title" => topic.title.desc
          case "createdAt" => topic.createdAt.desc
          case _ => topic.id.desc
        }
        case _ => topic.id.asc
      }
      )
      recordsTotal <- countAllRow()
      recordsFiltered <- if (!filter.isEmpty) {
        countFilteredRow(filterQuery)
      } else {
        Future {
          recordsTotal
        }
      }
    } yield (findTopics, recordsTotal, recordsFiltered)
  }

  override def insert(user: Account, objectToInsert: Topic): Future[Long] = {
    insert(Seq(objectToInsert.copy(
      slug = Some(objectToInsert.title.get.toLowerCase.replace(" ", "-")),
      ownerId = user.id,
      createdAt = Some(new Timestamp(new DateTime().getMillis)))
    )).map(_.head)
  }

  override def insert(user: Account, postId: Option[Long], objectToInserts: Seq[Topic]): Future[Unit] = {
    val rows = objectToInserts.map {
      topic =>
        topic.copy(
          slug = Some(topic.title.get.toLowerCase.replace(" ", "-")),
          ownerId = user.id,
          createdAt = Some(new Timestamp(new DateTime().getMillis)))
    }

    db.run(
      DBIO.seq(rows.filter(_.isValid).map {
        row =>
          tableQ.filter(topic => topic.title === row.title && topic.title === row.title).result.headOption
            .map {
              case Some(topic) => modules.postTopicsDal.insert(PostTopic(Some(0), postId, topic.id, Some(new Timestamp(new DateTime().getMillis))))
              case None => (tableQ returning tableQ.map(_.id) += row)
                .map(id => modules.postTopicsDal.insert(PostTopic(Some(0), postId, Some(id), Some(new Timestamp(new DateTime().getMillis)))))
            }
      }: _*)
    )
  }

  override def delete(user: Account, objectId: Long): Future[Int] = {
    deleteByFilter(topic => topic.ownerId === user.id && topic.id === objectId)
  }
}
