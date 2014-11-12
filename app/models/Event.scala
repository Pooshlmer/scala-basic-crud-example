package models

import org.joda.time.DateTime
import anorm._
import anorm.SqlParser._
import util.AnormExtension._

case class Event(id: Int, title: String, startTime: DateTime, endTime: DateTime, streamLink: String) {
}

object Event {
  def parser(timezone: Int) = {
    get[Int]("id") ~
    get[String]("title") ~
    get[DateTime]("start_time") ~
    get[DateTime]("end_time") ~
    get[String]("stream_link") map {
      case id~title~startTime~endTime~streamLink => 
        Event(id, title, startTime.plusHours(timezone), endTime.plusHours(timezone), streamLink)
    }
  }
}