package mappings

import scala.Left
import models._
import play.api.data.format.Formatter
import play.api.data.Mapping
import play.api.data.Forms
import play.api.data.FormError


object CustomMappings {
  /*
  private def error(key: String, msg: String) = Left(List(new FormError(key, msg)))
  
  implicit val gameFormatter = new Formatter[Game] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Game] = {
      data.get(key).map { value =>
        Game.load(value.toInt)
          .map(Right(_))
          .getOrElse(error(key, "No Game with id " + value))
      }.getOrElse(error(key, "No Game provided."))
    }
    
    override def unbind(key: String, value: Game): Map[String, String] = {
      Map(key -> value.id.get.toString)
    }
  }
  
  def game: Mapping[Game] = Forms.of[Game]
  */
}