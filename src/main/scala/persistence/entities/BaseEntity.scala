package persistence.entities

/**
  * Created by Gayo on 10/27/2016.
  */
trait BaseEntity {
  val id: Option[Long]

  def isValid: Boolean = true
}