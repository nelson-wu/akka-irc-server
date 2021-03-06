package Actors

import Messages.Implicits.ImplicitConversions.{ThreadId, UserId, UserName}
import Messages._
import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.collection.mutable

class Channels(writer: ActorRef) extends Actor with ActorLogging {
  case class Channel (topic: Option[String] = None, users: mutable.Buffer[UserName] = mutable.Buffer(), log: mutable.Buffer[String] = mutable.Buffer())
  private val channels = collection.mutable.Map[String, Channel]()
  private val channelIdMap = collection.mutable.Map[ThreadId, String]()
  private val userIdMap = collection.mutable.Map[UserId, UserName]()

  def receive = {
    case Message(JoinCommand, Prefix(user), Middle(channel), _) if !isUserInChannel(user, channel) ⇒ {
      val factory = new MessageFactory(user)
      import factory._
      channels(channel).users += user

      val joinMessages = channels(channel).users.map { u ⇒
        val userFactory = new MessageFactory(u.value)
        userFactory.JOIN(user, channel)
      }
      joinMessages.foreach(writer ! _)

      val topicReply = channels(channel).topic match {
        case Some(setTopic) ⇒ RPL_TOPIC(user, channel, setTopic)
        case None ⇒ RPL_NOTOPIC(user, channel)
      }

      val channelInfo = Seq(
        topicReply,
        RPL_NAMREPLY(user, channel, channels(channel).users),
        RPL_ENDOFNAMES(user, channel)
      )
      channelInfo foreach(writer ! _)
    }


    case Message(PrivmsgCommand, Prefix(user), Compound(list, message), _) ⇒ {
      val channel = list.head.underlying
      channels(channel).log += message.text
      val messages = channels(channel).users.filter(_ != UserName(user)).map { u ⇒
        val factory = new MessageFactory(u.value)
        factory.PRIVMSG(user, channel, message.text)
      }
      messages.foreach(writer ! _)
    }

    case Message(PartCommand, Prefix(user), Middle(channel), _) if isUserInChannel(user, channel)⇒ {
      channels(channel).users -= user
    }

    case NewFbThread(channelName, threadId) => channelIdMap += (threadId -> channelName.value)

    case FbUserJoin(userName, userId, threadId) => {
      val factory = new MessageFactory("self")
      val channel = channelIdMap(threadId)
      userIdMap += (userId → userName)
      self ! factory.JOIN(userName.value, channel)
    }

    case NewFbMessage(userId, threadId, text) => {
      val factory = new MessageFactory("self")
      val channel = channelIdMap(threadId)
      val userName = userIdMap(userId).value
      self ! factory.PRIVMSG(userName, channel, text)
    }

  }
  def isUserInChannel(user: String, channel: String): Boolean = {
    // TODO: Deprecate this, since users don't need to create chanels
    if (!channels.contains(channel)) channels += (channel → Channel(None))
    channels(channel).users.contains(user)
  }

}