package mappings

import models._
import play.api.data.format.Formatter
import play.api.data.Mapping
import play.api.data.Forms
import play.api.data.FormError


object CustomMappings {
  /*
  private def error(key: String, msg: String) = Left(List(new FormError(key, msg)))
  
  implicit val EventGameFormatter = new Formatter[EventGame] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], EventGame] = {
      data.get(key).map { value =>
        EventGame.load(value.toInt)
          .map(Right(_))
          .getOrElse(error(key, "No EventGame with id " + value))
      }.getOrElse(error(key, "No EventGame provided."))
    }
    
    override def unbind(key: String, value: EventGame): Map[String, String] = {
      Map(key -> value.toString())
    }
  }
  
  def EventGame: Mapping[EventGame] = Forms.of[EventGame]
 */ 
}