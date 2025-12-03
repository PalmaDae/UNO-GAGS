package uno_proto.common

data class NetworkMessage(
    val id: Long, // id-шник сообщения
    val method: Method, // один из поддерживаемых методов
    val payload: Payload, // наши классы (дтошки) которые будем тащить, они будут имплементить Payload
    val timestamp: Long = System.currentTimeMillis() // время создания сообщения
)
