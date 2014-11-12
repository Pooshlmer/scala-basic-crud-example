package models

import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._

case class Game(id: Long, title: String) {
}

object Game {
  
  val parser = {
    get[Int]("id") ~
    get[String]("title") map {
      case id ~ title => Game(id, title)
    }
  }
  
  def list: List[Game] = {
    DB.withConnection { implicit connection =>
      SQL("SELECT * FROM game").as(parser *)
    }
  }
}