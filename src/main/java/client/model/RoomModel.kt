package client.model

import proto.dto.LobbyUpdate

class RoomModel {
    var currentRoomId: Long? = null
    var lobbyState: LobbyUpdate? = null

    fun joinRoom(roomId: Long) {
        currentRoomId = roomId
    }

    fun leaveRoom() {
        currentRoomId = null
        lobbyState = null
    }

    fun updateLobby(update: LobbyUpdate) {
        lobbyState = update
    }

    fun reset() {
        leaveRoom()
    }
}
