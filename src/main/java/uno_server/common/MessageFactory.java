package uno_server.common;

import uno_proto.common.Method;
import uno_proto.common.NetworkMessage;
import uno_proto.common.Version;
import uno_proto.dto.*;
import java.util.concurrent.atomic.AtomicLong;

public class MessageFactory {
    private static final AtomicLong messageIdCounter = new AtomicLong(0);

    public static NetworkMessage createRoomRequest(String roomName, String password, int maxPlayers) {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.CREATE_ROOM,
                new CreateRoomRequest(roomName, password, maxPlayers, false)
        );
    }

    public static NetworkMessage createRoomResponse(long roomId, String roomName, boolean success) {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                success ? Method.ROOM_CREATED_SUCCESS : Method.ROOM_CREATED_ERROR,
                new CreateRoomResponse(roomId, roomName, success)
        );
    }

    public static NetworkMessage lobbyChatMessage(long senderId, String senderName, String content) {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.LOBBY_CHAT,
                new ChatMessage(senderId, senderName, content)
        );
    }

    public static NetworkMessage okMessage() {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.OK,
                new OkMessage()
        );
    }

    public static NetworkMessage errorMessage() {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.ERROR,
                new ErrorMessage()
        );
    }

    // Методы для системных сообщений
    public static NetworkMessage pingMessage() {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.PING,
                new PingMessage()
        );
    }

    public static NetworkMessage pongMessage() {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.PONG,
                new PongMessage()
        );
    }

    // Методы для игрового процесса
    public static NetworkMessage gameStateMessage(GameState gameState) {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.GAME_STATE,
                gameState
        );
    }

    public static NetworkMessage joinRoomRequest(long roomId, String password) {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                Method.JOIN_ROOM,
                new JoinRoomRequest(roomId, password)
        );
    }

    public static NetworkMessage joinRoomResponse(long roomId, boolean success) {
        return new NetworkMessage(
                messageIdCounter.incrementAndGet(),
                Version.V1,
                success ? Method.JOIN_ROOM_SUCCESS : Method.JOIN_ROOM_ERROR,
                new JoinRoomResponse(roomId, success)
        );
    }
}