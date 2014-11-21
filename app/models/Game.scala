package models

import org.joda.time.DateTime
import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._

// This class defines a Game, 1 or more of which are hosted by an Event

case class Game(id: Int, title: String) {
}

object Game {
  
  // Generic parser for getting row data from an SQL database
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
  
  def load(id: Int): Option[Game] = {
    DB.withConnection { implicit c =>
      SQL"""SELECT * FROM game WHERE id = $id""".as(parser.singleOpt)
    }
  }
}