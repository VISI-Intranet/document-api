package domain

import domain.Documenttype.Documenttype

import java.util.Date

case class Documentation(_id:Option[String] = None,
                         docid:Int,
                         title: String,
                         userid: Int,
                         courseid: Int,
                         dateuploaded:Date,
                         documenttype:Documenttype,
                         tags:String,
                         fileURL:String)

case class DocumentationUpdate(
                          docid: Option[Int] = None,
                          title: Option[String] = None,
                          userid: Option[Int] = None,
                          courseid: Option[Int] = None,
                          dateuploaded: Option[Date] = None,
                          documenttype: Option[Documenttype] = None,
                          tags: Option[String] = None,
                          fileURL: Option[String] = None
                        )
  object Documenttype extends Enumeration {
    type Documenttype = Value
    val PPTX,DOCX,ZIP,PDF = Value
  }