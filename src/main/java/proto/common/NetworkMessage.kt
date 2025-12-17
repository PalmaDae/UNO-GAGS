package proto.common

import proto.dto.Payload
import java.io.Serializable

data class NetworkMessage
@JvmOverloads
constructor (
    val id: Long, // id-шник сообщения,
    val version: Version = Version.V1, // версия протокола
    val method: Method, // один из поддерживаемых методов
    val payload: Payload, // наши классы (дтошки) которые будем тащить, они будут имплементить Payload
    val timestamp: Long = System.currentTimeMillis() // время создания сообщения
) : Serializable
