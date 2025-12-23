package proto.common

enum class Method {
    // LOBBY
    CREATE_ROOM,
    ROOM_CREATED_SUCCESS,
    ROOM_CREATED_ERROR,
    JOIN_ROOM_REQUEST,
    JOIN_ROOM_RESPONSE,
    LEAVE_ROOM,
    KICK_PLAYER,
    LOBBY_UPDATE,

    // GAME
    START_GAME,
    GAME_STATE,
    PLAY_CARD,
    DRAW_CARD,
    SAY_UNO,
    CHOOSE_COLOR,
    FINISH_DRAWING,

    // SYSTEM
    PING,
    PONG,
    OK,
    ERROR
}