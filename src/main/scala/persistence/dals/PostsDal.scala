package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.SlickTables.{PostTopicsTable, PostsTable, TopicsTable}
import persistence.entities.{AccountChildItem, CategoryChildItem, _}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep
import utils.PimpedSeq._
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/29/2016.
  */
trait PostsDal extends BaseDalImpl[PostsTable, Post] {
  def find(user: Account, matcher: Any): Future[Option[PostItem]]

  def finds(user: Account, filter: String, categoryId: Option[Int], accountId: Option[Int], lastId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[PostItem], Int, Int)]

  def insert(user: Account, objectToInsert: Post, topicsToInsert: Seq[Topic]): Future[Long]

  def update(user: Account, objectId: Long, objectToUpdate: Post, topicsToInsert: Seq[Topic], topicsToDelete: Seq[Topic]): Future[Long]

  def delete(user: Account, objectId: Long): Future[Int]
}

class PostsDalImpl(modules: Configuration with PersistenceModule)(implicit override val db: JdbcProfile#Backend#Database) extends PostsDal {
  override def find(user: Account, matcher: Any): Future[Option[PostItem]] = {
    val data = for {
      ((post, _), topic) <- tableQ withFilter (matcher match {
        case id: Int => _.id === id.toLong
        case slug: String => _.slug === slug
      }) joinLeft TableQuery[PostTopicsTable] on (_.id === _.postId) joinLeft TableQuery[TopicsTable] on (_._2.map(_.topicId) === _.id)
      category <- post.category
      owner <- post.owner
    } yield (post, category, owner, topic)

    db.run(data.to[List].result).map {
      _.groupBy(_._1)
        .map {
          case (post, combination) =>
            val categoryItem = CategoryChildItem(
              combination.head._2.id.get,
              combination.head._2.title.get,
              combination.head._2.slug.get,
              combination.head._2.image.get)
            val ownerItem = AccountChildItem(
              combination.head._3.id.get,
              combination.head._3.username.get,
              combination.head._3.firstName.get,
              combination.head._3.lastName.get,
              combination.head._3.avatar.get)
            val topicItems = combination.map(_._4.orNull).filter(r => r != null).map {
              topic => TopicChildItem(topic.id.get, topic.title.get, topic.slug.get)
            }

            PostItem(
              post.id.get,
              post.title.get,
              post.slug.get,
              post.thumbnail.get,
              categoryItem,
              post.content.get,
              topicItems,
              post.isPrivate.get,
              post.isPublish.get,
              ownerItem,
              post.createdAt.get,
              post.updatedAt.orNull
            )
        }.headOption
    }
  }

  override def finds(user: Account, filter: String, categoryId: Option[Int], accountId: Option[Int], lastId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[PostItem], Int, Int)] = {
    val filterQuery: (PostsTable) => Rep[Option[Boolean]] = post => ((post.title like s"%$filter%") || (post.content like s"%$filter%")) && (categoryId match {
      case Some(value) => post.categoryId === value.toLong
      case _ => post.categoryId =!= 0L
    }) && (accountId match {
      case Some(value) => post.ownerId === value.toLong
      case _ => post.ownerId =!= 0L
    })

    val filterQueryWithLastId: (PostsTable) => Rep[Option[Boolean]] = post => ((post.title like s"%$filter%") || (post.content like s"%$filter%")) && (categoryId match {
      case Some(value) => post.categoryId === value.toLong
      case _ => post.categoryId =!= 0L
    }) && (accountId match {
      case Some(value) => post.ownerId === value.toLong
      case _ => post.ownerId =!= 0L
    }) && post.id > lastId.get.toLong

    for {
      findPosts <- {
        val data = (for {
          ((post, _), topic) <- lastId match {
            case None => tableQ withFilter filterQuery drop offset take limit joinLeft TableQuery[PostTopicsTable] on (_.id === _.postId) joinLeft TableQuery[TopicsTable] on (_._2.map(_.topicId) === _.id)
            case Some(_) => tableQ withFilter filterQueryWithLastId take limit joinLeft TableQuery[PostTopicsTable] on (_.id === _.postId) joinLeft TableQuery[TopicsTable] on (_._2.map(_.topicId) === _.id)
          }
          category <- post.category
          owner <- post.owner
        } yield (post, category, owner, topic)).sortBy(sortType match {
          case "asc" => sortBy match {
            case "title" => _._1.title.asc
            case "createdAt" => _._1.createdAt.asc
            case "updatedAt" => _._1.updatedAt.asc
            case _ => _._1.id.asc
          }
          case "desc" => sortBy match {
            case "title" => _._1.title.desc
            case "createdAt" => _._1.createdAt.desc
            case "updatedAt" => _._1.updatedAt.desc
            case _ => _._1.id.desc
          }
          case _ => _._1.id.asc
        })

        db.run(data.to[List].result).map {
          _.groupConsecutiveKeys(_._1)
            .map {
              case (post, combination) =>
                val categoryItem = CategoryChildItem(
                  combination.head._2.id.get,
                  combination.head._2.title.get,
                  combination.head._2.slug.get,
                  combination.head._2.image.get)
                val ownerItem = AccountChildItem(
                  combination.head._3.id.get,
                  combination.head._3.username.get,
                  combination.head._3.firstName.get,
                  combination.head._3.lastName.get,
                  combination.head._3.avatar.get)
                val topicItems = combination.map(_._4.orNull).filter(r => r != null).map {
                  topic => TopicChildItem(topic.id.get, topic.title.get, topic.slug.get)
                }

                PostItem(
                  post.id.get,
                  post.title.get,
                  post.slug.get,
                  post.thumbnail.get,
                  categoryItem,
                  post.content.get,
                  topicItems,
                  post.isPrivate.get,
                  post.isPublish.get,
                  ownerItem,
                  post.createdAt.get,
                  post.updatedAt.orNull
                )
            }.toSeq
        }
      }
      recordsTotal <- countAllRow()
      recordsFiltered <- if (!filter.isEmpty) {
        countFilteredRow(filterQuery)
      } else {
        Future {
          recordsTotal
        }
      }
    } yield (findPosts, recordsTotal, recordsFiltered)
  }

  override def insert(user: Account, objectToInsert: Post, topicsToInsert: Seq[Topic]): Future[Long] = {
    for {
      createPost <- insert(objectToInsert.copy(
        title = objectToInsert.title,
        slug = Some(objectToInsert.title.get.toLowerCase.replace(" ", "-")),
        thumbnail = Some("/assets/images/icon-default-post.png"),
        categoryId = objectToInsert.categoryId,
        content = objectToInsert.content,
        isPrivate = objectToInsert.isPrivate,
        isPublish = objectToInsert.isPublish,
        ownerId = user.id,
        createdAt = Some(new Timestamp(new DateTime().getMillis))))
      _ <- modules.topicsDal.insert(user, Some(createPost), topicsToInsert)
    } yield createPost
  }

  override def update(user: Account, objectId: Long, objectToUpdate: Post, topicsToInsert: Seq[Topic], topicsToDelete: Seq[Topic]): Future[Long] = {
    for {
      findPost <- findByMatcher(post => post.ownerId === user.id && post.id === objectId)
      updatePost <- findPost match {
        case Some(post) => update(post.copy(
          title = objectToUpdate.title.orElse(post.title),
          thumbnail = objectToUpdate.thumbnail.orElse(post.thumbnail),
          categoryId = objectToUpdate.categoryId.orElse(post.categoryId),
          content = objectToUpdate.content.orElse(post.content),
          isPrivate = objectToUpdate.isPrivate.orElse(post.isPrivate),
          isPublish = objectToUpdate.isPublish.orElse(post.isPublish),
          updatedAt = Some(new Timestamp(new DateTime().getMillis))
        ))
        case None => Future {
          0
        }
      }
      _ <- modules.topicsDal.insert(user, findPost.get.id, topicsToInsert)
      _ <- modules.postTopicsDal.delete(findPost.get.id, topicsToDelete.map {
        topic => topic.id.get
      })
    } yield updatePost
  }

  override def delete(user: Account, objectId: Long): Future[Int] = {
    deleteByFilter(post => post.ownerId === user.id && post.id === objectId)
  }
}
