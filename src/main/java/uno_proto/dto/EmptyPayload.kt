package uno_proto.dto

import uno_proto.common.Payload

/**
 * Simple empty payload for requests that don't need parameters
 */
data class EmptyPayload(
    val dummy: String = ""
) : Payload {
    constructor() : this("")
}