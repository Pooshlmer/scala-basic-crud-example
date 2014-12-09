package models

import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._

// This class defines the many to many relation between Event and Game

case class EventGame(id: Int, event_id: Int, game_id: Int, tier: Int = 0) {
}

object EventGame {
  
  val BOTTOMTIER = 4
  
  // Generic parser for getting row data from an SQL database
  val parser = {
    get[Int]("id") ~
    get[Int]("event_id") ~
    get[Int]("game_id") ~
    get[Int]("tier") map {
      case id ~ event_id ~ game_id ~ tier => EventGame(id, event_id, game_id, tier)
    }
  }
  
  def getGames(event_id: Int) {
    DB.withConnection { implicit c =>
      SQL"""SELECT * FROM event_game WHERE id = $event_id""".as(parser *)
    }
  } 
}