package servers;

import com.google.gson.JsonObject;
import dto.MessageType;
import dto.ServerGeneralMessage;
import entities.connection.WebConnection;
import entities.player.WebPlayerEntity;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import match.OfflineMatchManager;
import model.player.PlayerEntity;
import services.OfflineMatchService;
import utils.GlobalSettings;
import utils.json.JsonFormatter;
import utils.loggers.LoggerManager;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/")
public class WebSocketServer extends Thread
{
    private static final int PORT = 8081;
    private static final Map<String, PlayerEntity> s_WebSocketsPlayerMap = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session)
    {
        // Handle WebSocket connection open event
        System.out.println("WebSocket connection opened: " + session.getId());

        PlayerEntity webPlayer = new WebPlayerEntity(new WebConnection(session));
        s_WebSocketsPlayerMap.put(session.getId(), webPlayer);
    }

    @OnMessage
    public void onMessage(String message, Session session) throws SocketTimeoutException {
        WebPlayerEntity webPlayer = (WebPlayerEntity) getPlayerEntity(session); // Down casting to original object entity.

        if (webPlayer != null)
        {
            if(webPlayer.IsPlayerConnectedToAMatch())
                webPlayer.HandleMessageReceived(message);
            else
            {
                LoggerManager.info(message);

                JsonObject playerData = JsonFormatter.createJsonFromString(message);
                String messageType = playerData.get("messagetype").getAsString();

                if(messageType.equals(MessageType.CONFIGURATION))
                {
                    OfflineMatchService matchService = OfflineMatchManager.GetRoomById(playerData.get("roomid").getAsString());

                    if (matchService != null)
                    {
                        webPlayer.SetInitData(playerData);
                        matchService.AddPlayer(webPlayer);
                        webPlayer.MarkAsReady();
                        webPlayer.sendMessageToHost(new ServerGeneralMessage(ServerGeneralMessage.eActionType.PLAYER_READY, webPlayer.GetUserName()).toString());
                        LoggerManager.info(webPlayer.GetUserName() + " is Ready.");
                        webPlayer.SendMessage("OK");
                    }
                    else
                    {
                        onClose(session, new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Room doesn't exist."));
                    }
                }
                else
                {
                    onClose(session, new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Problem with initial data configuration."));
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason)
    {
        // Handle WebSocket connection close event
        PlayerEntity webPlayer = getPlayerEntity(session);

        if(webPlayer != null)
        {
            webPlayer.CloseConnection(GlobalSettings.CLIENT_CLOSED_CONNECTION);
        }

        s_WebSocketsPlayerMap.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable)
    {
        // Handle WebSocket error event
        onClose(session, new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, throwable.getMessage()));
    }

    @Override
    public void run()
    {
        Map<String, Object> map = new HashMap<>();

        org.glassfish.tyrus.server.Server server = new
                org.glassfish.tyrus.server.Server("localhost", PORT, "/",
                map, WebSocketServer.class);

        try {
            server.start();
            LoggerManager.info("WebSocket server started. Listening on: " + server.getPort()); //was written getURI()

            // Keep the server running until interrupted
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }

    private PlayerEntity getPlayerEntity(Session session)
    {
        return s_WebSocketsPlayerMap.get(session.getId());
    }

}