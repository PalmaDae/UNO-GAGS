package proto.dto

import proto.common.Payload

data class OkMessage
@JvmOverloads
constructor(
    val message: String = "ok",
    val timestamp: Long = System.currentTimeMillis()
) : Payload

data class ErrorMessage
@JvmOverloads
constructor(
    val message: String = "error",
    val timestamp: Long = System.currentTimeMillis()
) : Payload

data class PingMessage
@JvmOverloads
constructor(
    val message: String = "ping",
    val timestamp: Long = System.currentTimeMillis()
) : Payload

data class PongMessage
@JvmOverloads
constructor(
    val message: String = "pong",
    val timestamp: Long = System.currentTimeMillis()
) : Payload

data class ErrorPayload(
    val message: String,
    val code: String
) : Payload

data class EmptyPayload(
    val dummy: String = ""
) : Payload