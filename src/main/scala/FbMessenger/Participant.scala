package FbMessenger

import play.api.libs.json.JsArray

case class Participant(name: String, userId: String)

object Participant {
  def fromArray(in: JsArray): Set[Participant] = {
    in.value.map { v ⇒
      new Participant(
        (v \ "name").get.as[String],
        (v \ "userID").get.as[String]
      )
    }.toSet
  }
}
