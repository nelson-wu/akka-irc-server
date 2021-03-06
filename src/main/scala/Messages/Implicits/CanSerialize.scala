package Messages.Implicits

import Messages._

trait CanSerialize[-B <: Params] {
  def serialize(params: B): String
}

object CanSerialize {
  implicit object CanSerializeMiddle extends CanSerialize[Middle] {
    override def serialize(params: Middle): String = params.underlying
  }
  implicit object CanSerializeTrailing extends CanSerialize[Trailing] {
    def serialize(params: Trailing): String = ":" + params.text
  }
  implicit object CanSerializeNone extends CanSerialize[NoParams.type] {
    def serialize(params: NoParams.type): String = ""
  }
  implicit object CanSerializeCompound extends CanSerialize[Compound] {
    def serialize(params: Compound): String = {
      val targets = params.targets.map(implicitly[CanSerialize[Middle]].serialize).mkString(" ")
      val special = implicitly[CanSerialize[Trailing]].serialize(params.special)
      targets + " " + special
    }
  }
}