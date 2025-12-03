package uno_proto.common;

public enum Method {

    // LOBBY
    CREATE_ROOM,
    ROOM_CREATED,
    GET_ROOMS,
    ROOMS_LIST,
    JOIN_ROOM,
    JOIN_ROOM_SUCCESS,
    JOIN_ROOM_ERROR,
    LEAVE_ROOM,
    KICK_PLAYER,
    START_GAME,
    LOBBY_UPDATE,
    LOBBY_CHAT,

    // GAME
    GAME_START,
    GAME_STATE,
    PLAY_CARD,
    DRAW_CARD,
    SAY_UNO,
    GAME_CHAT,

    // SYSTEM
    PING,
    PONG,
    OK,
    ERROR
}