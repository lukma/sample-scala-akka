package persistence.dals

import java.sql.Timestamp

import org.joda.time.DateTime
import persistence.entities.SlickTables.CategoriesTable
import persistence.entities.{Account, Category}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/29/2016.
  */
trait CategoriesDal extends BaseDalImpl[CategoriesTable, Category] {
  def finds(user: Account, filter: String, parentId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[Category], Int, Int)]

  def insert(user: Account, objectToInsert: Category): Future[Long]

  def update(user: Account, objectId: Long, objectToUpdate: Category): Future[Long]

  def delete(user: Account, objectId: Long): Future[Int]
}

class CategoriesDalImpl()(implicit override val db: JdbcProfile#Backend#Database) extends CategoriesDal {
  override def finds(user: Account, filter: String, parentId: Option[Int], limit: Int, offset: Int, sortBy: String, sortType: String): Future[(Seq[Category], Int, Int)] = {
    val filterQuery: (CategoriesTable) => Rep[Boolean] = category => parentId match {
      case Some(value) => (category.title like s"%$filter%") && (category.parentId === value.toLong)
      case _ => category.title like s"%$filter%"
    }

    for {
      findCategories <- findByFilter(filterQuery, limit, offset, category => sortType match {
        case "asc" => sortBy match {
          case "title" => category.title.asc
          case "createdAt" => category.createdAt.asc
          case "updatedAt" => category.updatedAt.asc
          case _ => category.id.asc
        }
        case "desc" => sortBy match {
          case "title" => category.title.desc
          case "createdAt" => category.createdAt.desc
          case "updatedAt" => category.updatedAt.asc
          case _ => category.id.desc
        }
        case _ => category.id.asc
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
    } yield (findCategories, recordsTotal, recordsFiltered)
  }

  override def insert(user: Account, objectToInsert: Category): Future[Long] = {
    insert(objectToInsert.copy(
      slug = Some(objectToInsert.title.get.toLowerCase.replace(" ", "-")),
      image = Some("/assets/images/icon-default-category.png"),
      ownerId = user.id,
      createdAt = Some(new Timestamp(new DateTime().getMillis))))
  }

  override def update(user: Account, objectId: Long, objectToUpdate: Category): Future[Long] = {
    for {
      findCategory <- findByMatcher(category => category.ownerId === user.id && category.id === objectId)
      updateCategory <- findCategory match {
        case Some(category) => update(category.copy(
          title = objectToUpdate.title.orElse(category.title),
          image = objectToUpdate.image.orElse(category.image),
          parentId = objectToUpdate.parentId.orElse(category.parentId),
          updatedAt = Some(new Timestamp(new DateTime().getMillis))
        ))
        case None => Future {
          0
        }
      }
    } yield updateCategory
  }

  override def delete(user: Account, objectId: Long): Future[Int] = {
    deleteByFilter(category => category.ownerId === user.id && category.id === objectId)
  }
}
