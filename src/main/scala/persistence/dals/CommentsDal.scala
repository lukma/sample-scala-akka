package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.SlickTables.CommentsTable
import persistence.entities.{Account, AccountChildItem, Comment, CommentItem}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/29/2016.
  */
trait CommentsDal extends BaseDalImpl[CommentsTable, Comment] {
  def finds(user: Account, postId: Int, filter: String, parentId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[CommentItem], Int, Int)]

  def insert(user: Account, objectToInsert: Comment): Future[Long]

  def update(user: Account, objectId: Long, objectToUpdate: Comment): Future[Long]

  def delete(user: Account, objectId: Long): Future[Int]
}

class CommentsDalImpl()(implicit override val db: JdbcProfile#Backend#Database) extends CommentsDal {
  override def finds(user: Account, postId: Int, filter: String, parentId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[CommentItem], Int, Int)] = {
    val filterQuery: (CommentsTable) => Rep[Option[Boolean]] = comment =>
      comment.postId === postId.toLong && (comment.content like s"%$filter%") && (parentId match {
        case Some(value) => comment.parentId === value.toLong
        case _ => comment.parentId === 0L
      })

    for {
      findComments <- {
        val data = for {
          comment <- tableQ withFilter filterQuery sortBy (comment => sortType match {
            case "asc" => sortBy match {
              case "createdAt" => comment.createdAt.asc
              case "updatedAt" => comment.updatedAt.asc
              case _ => comment.id.asc
            }
            case "desc" => sortBy match {
              case "createdAt" => comment.createdAt.desc
              case "updatedAt" => comment.updatedAt.desc
              case _ => comment.id.desc
            }
            case _ => comment.id.asc
          }) drop offset take limit
          owner <- comment.owner
        } yield (comment, owner)

        db.run(data.to[List].result).map {
          _.map {
            case (comment, owner) =>
              val accountItem = AccountChildItem(
                owner.id.get,
                owner.username.get,
                owner.firstName.get,
                owner.lastName.get,
                owner.avatar.get)

              CommentItem(
                comment.id.get,
                comment.content.get,
                comment.parentId.get,
                accountItem,
                comment.createdAt.get,
                comment.updatedAt.orNull
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
    } yield (findComments, recordsTotal, recordsFiltered)
  }

  override def insert(user: Account, objectToInsert: Comment): Future[Long] = {
    insert(objectToInsert.copy(
      ownerId = user.id,
      createdAt = Some(new Timestamp(new DateTime().getMillis))))
  }

  override def update(user: Account, objectId: Long, objectToUpdate: Comment): Future[Long] = {
    for {
      findComment <- findByMatcher(comment => comment.ownerId === user.id && comment.id === objectId)
      updateComment <- findComment match {
        case Some(comment) => update(comment.copy(
          content = objectToUpdate.content.orElse(comment.content),
          updatedAt = Some(new Timestamp(new DateTime().getMillis))
        ))
        case None => Future {
          0
        }
      }
    } yield updateComment
  }

  override def delete(user: Account, objectId: Long): Future[Int] = {
    deleteByFilter(comment => comment.ownerId === user.id && comment.id === objectId)
  }
}
