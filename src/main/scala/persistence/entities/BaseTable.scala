package persistence.entities

import java.sql.Timestamp

import slick.driver.PostgresDriver.api._
import slick.lifted.Tag

/**
  * Created by Gayo on 10/27/2016.
  */
abstract class BaseTable[T](tag: Tag, name: String) extends Table[T](tag, name) {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def createdAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("created_at", O.SqlType("timestamp default now()"))
}