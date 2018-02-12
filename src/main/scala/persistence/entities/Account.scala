package persistence.entities

import java.sql.{Date, Timestamp}

import persistence.types.GenderEnum
import persistence.types.GenderEnum.GenderEnum
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsNull, JsString, JsValue, RootJsonFormat}

/**
  * Created by Gayo on 10/27/2016.
  */
case class Account(id: Option[Long] = Some(0),
                   username: Option[String] = None,
                   password: Option[String] = None,
                   roleId: Option[Long] = None,
                   email: Option[String] = None,
                   phone: Option[String] = None,
                   firstName: Option[String] = None,
                   lastName: Option[String] = None,
                   avatar: Option[String] = None,
                   birthDate: Option[Date] = None,
                   gender: Option[GenderEnum] = None,
                   about: Option[String] = None,
                   isActive: Option[Boolean] = None,
                   isVerified: Option[Boolean] = None,
                   createdAt: Option[Timestamp] = None,
                   updatedAt: Option[Timestamp] = None) extends BaseEntity

object Account {

  implicit object GenderFormat extends RootJsonFormat[GenderEnum] {
    def write(obj: GenderEnum) = JsString(obj.toString)

    def read(json: JsValue): GenderEnum.Value = json match {
      case JsString(s) => GenderEnum.withName(s)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit object DateFormat extends RootJsonFormat[Date] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd")

    def write(obj: Date) = JsString(format.format(obj.getTime))

    def read(json: JsValue): Date = json match {
      case JsString(s) => new Date(format.parse(s).getTime)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit object TimestampFormat extends RootJsonFormat[Timestamp] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    def write(obj: Timestamp) = JsString(format.format(obj.getTime))

    def read(json: JsValue): Timestamp = json match {
      case JsString(s) => new Timestamp(format.parse(s).getTime)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit val objectFormat: RootJsonFormat[Account] = jsonFormat16(Account.apply)
}

case class AccountItem(id: Long,
                       username: String,
                       role: RoleChildItem,
                       email: Option[String] = None,
                       phone: Option[String] = None,
                       firstName: String,
                       lastName: String,
                       avatar: String,
                       birthDate: Date,
                       gender: GenderEnum,
                       about: String,
                       isActive: Boolean,
                       isVerified: Boolean,
                       createdAt: Timestamp,
                       updatedAt: Timestamp)

object AccountItem {

  implicit object GenderFormat extends RootJsonFormat[GenderEnum] {
    def write(obj: GenderEnum) = JsString(obj.toString)

    def read(json: JsValue): GenderEnum.Value = json match {
      case JsString(s) => GenderEnum.withName(s)
      case _ => throw DeserializationException("Date expected")
    }
  }

  implicit object DateFormat extends RootJsonFormat[Date] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd")

    def write(obj: Date) = JsString(format.format(obj.getTime))

    def read(json: JsValue): Date = json match {
      case JsString(s) => new Date(format.parse(s).getTime)
      case _ => throw DeserializationException("Date expected")
    }
  }

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

  implicit val objectFormat: RootJsonFormat[AccountItem] = jsonFormat15(AccountItem.apply)
}

case class AccountResponse(status: Int, data: AccountItem)

object AccountResponse {
  implicit val objectFormat: RootJsonFormat[AccountResponse] = jsonFormat2(AccountResponse.apply)
}

case class AccountsResponse(status: Int, data: Seq[AccountItem], recordsTotal: Int, recordsFiltered: Int)

object AccountsResponse {
  implicit val objectFormat: RootJsonFormat[AccountsResponse] = jsonFormat4(AccountsResponse.apply)
}

case class AccountChildItem(id: Long, username: String, firstName: String, lastName: String, avatar: String)

object AccountChildItem {
  implicit val objectFormat: RootJsonFormat[AccountChildItem] = jsonFormat5(AccountChildItem.apply)
}
