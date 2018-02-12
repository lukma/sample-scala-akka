package persistence.dals

import persistence.entities.{BaseEntity, BaseTable}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{CanBeQueryCondition, Ordered}
import utils.{DbModule, Profile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Gayo on 10/27/2016.
  */
trait BaseDal[T, A] {
  def insert(row: A): Future[Long]

  def insert(rows: Seq[A]): Future[Seq[Long]]

  def update(row: A): Future[Int]

  def update(rows: Seq[A]): Future[Unit]

  def findById(id: Long): Future[Option[A]]

  def findByMatcher[C: CanBeQueryCondition](filter: (T) => C): Future[Option[A]]

  def findByFilter[C: CanBeQueryCondition](filter: (T) => C): Future[Seq[A]]

  def findByFilter[C: CanBeQueryCondition, O <: Ordered](filter: (T) => C, limit: Int, offset: Int, sort: (T) => O): Future[Seq[A]]

  def deleteById(id: Long): Future[Int]

  def deleteById(ids: Seq[Long]): Future[Int]

  def deleteByFilter[C: CanBeQueryCondition](filter: (T) => C): Future[Int]

  def countAllRow(): Future[Int]

  def countFilteredRow[C: CanBeQueryCondition](filter: (T) => C): Future[Int]

  def createTable(): Future[Unit]
}

class BaseDalImpl[T <: BaseTable[A], A <: BaseEntity]()(implicit val tableQ: TableQuery[T], implicit val db: JdbcProfile#Backend#Database, implicit val profile: JdbcProfile) extends BaseDal[T, A] with Profile with DbModule {

  import profile.api._

  override def insert(row: A): Future[Long] = {
    insert(Seq(row)).map(_.head)
  }

  override def insert(rows: Seq[A]): Future[Seq[Long]] = {
    db.run(tableQ returning tableQ.map(_.id) ++= rows.filter(_.isValid))
  }

  override def update(row: A): Future[Int] = {
    if (row.isValid)
      db.run(tableQ.filter(_.id === row.id).update(row))
    else
      Future {
        0
      }
  }

  override def update(rows: Seq[A]): Future[Unit] = {
    db.run(DBIO.seq(rows.filter(_.isValid).map(r => tableQ.filter(_.id === r.id).update(r)): _*))
  }

  override def findById(id: Long): Future[Option[A]] = {
    db.run(tableQ.filter(_.id === id).result.headOption)
  }

  override def findByMatcher[C: CanBeQueryCondition](filter: (T) => C): Future[Option[A]] = {
    db.run(tableQ.withFilter(filter).result.headOption)
  }

  override def findByFilter[C: CanBeQueryCondition](filter: (T) => C): Future[Seq[A]] = {
    db.run(tableQ.withFilter(filter).result)
  }

  override def findByFilter[C: CanBeQueryCondition, O <: Ordered](filter: (T) => C, limit: Int, offset: Int, sort: (T) => O): Future[Seq[A]] = {
    db.run(tableQ.withFilter(filter).sortBy(sort).drop(offset).take(limit).result)
  }

  override def deleteById(id: Long): Future[Int] = {
    deleteById(Seq(id))
  }

  override def deleteById(ids: Seq[Long]): Future[Int] = {
    db.run(tableQ.filter(_.id.inSet(ids)).delete)
  }

  override def deleteByFilter[C: CanBeQueryCondition](filter: (T) => C): Future[Int] = {
    db.run(tableQ.withFilter(filter).delete)
  }

  override def countAllRow(): Future[Int] = {
    db.run(tableQ.length.result)
  }

  override def countFilteredRow[C: CanBeQueryCondition](filter: (T) => C): Future[Int] = {
    db.run(tableQ.withFilter(filter).length.result)
  }

  override def createTable(): Future[Unit] = {
    val dbRun = db.run(DBIO.seq(tableQ.schema.create))

    dbRun.onSuccess { case _ => println(getClass.getSimpleName + "->onSuccess") }
    dbRun.onFailure { case _ => println(getClass.getSimpleName + "->onFailure") }

    dbRun
  }
}