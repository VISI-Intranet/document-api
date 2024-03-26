package domain

import domain.Usertype.Usertype

case class User(_id:Option[String] = None,
                userid:Int,
                name:String,
                lastname:String,
                usertype:Usertype
               )


case class UserUpdate(
                 userid: Option[Int] = None,
                 name: Option[String] = None,
                 lastname: Option[String] = None,
                 usertype: Option[Usertype] = None
               )
object Usertype extends Enumeration {
  type Usertype = Value
  val STUDENT,TEACHER,ADMIN = Value
}