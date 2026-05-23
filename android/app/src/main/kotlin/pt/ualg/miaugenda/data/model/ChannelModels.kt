package pt.ualg.miaugenda.data.model

data class Channel(
    val id: Int,
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val type: String = "GROUP",
    val createdById: Int = 0,
    val createdAt: String,
    val updatedAt: String,
    val messages: List<ChannelMessage> = emptyList()
)

data class ChannelMessage(
    val id: Int,
    val channelId: Int,
    val userId: Int,
    val content: String,
    val createdAt: String,
    val user: MessageSender
)

data class MessageSender(
    val id: Int,
    val name: String
)

data class CreateChannelRequest(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = true,
    val type: String = "GROUP",
    val memberIds: List<Int> = emptyList()
)

data class SendMessageRequest(
    val content: String
)
