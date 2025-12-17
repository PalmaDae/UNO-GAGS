package proto.common;

public enum Method {

    // LOBBY
    CREATE_ROOM,
    ROOM_CREATED_SUCCESS,
    ROOM_CREATED_ERROR,
    GET_ROOMS,
    ROOMS_LIST,
    JOIN_ROOM_REQUEST,
    JOIN_ROOM_RESPONSE,
    LEAVE_ROOM,
    KICK_PLAYER,
    LOBBY_UPDATE,
    LOBBY_CHAT,

    // GAME
    START_GAME,
    GAME_STATE,
    PLAY_CARD,
    DRAW_CARD,
    SAY_UNO,
    GAME_CHAT,
    CHOOSE_COLOR,

    // SYSTEM
    PING,
    PONG,
    OK,
    ERROR
}