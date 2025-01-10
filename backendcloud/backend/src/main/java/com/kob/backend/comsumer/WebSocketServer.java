package com.kob.backend.comsumer;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.kob.backend.common.Constants;
import com.kob.backend.common.JwtUtil;
import com.kob.backend.entity.Bot;
import com.kob.backend.entity.User;
import com.kob.backend.repository.BotRepository;
import com.kob.backend.repository.RecordRepository;
import com.kob.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/websocket/{token}")  // 注意不要以'/'结尾
public class WebSocketServer {

    private final Log log = Log.get();

    public static ConcurrentHashMap<Long, WebSocketServer> users = new ConcurrentHashMap<>();

    private Session session = null;

    private User user;

    public static UserRepository userRepository;

    public Game game = null;

    @Autowired
    public void setUserMapper(UserRepository userRepository) {
        WebSocketServer.userRepository = userRepository;
    }

    public static RecordRepository recordRepository;

    @Autowired
    public void setRecordMapper(RecordRepository recordRepository) {
        WebSocketServer.recordRepository = recordRepository;
    }

    private static BotRepository botRepository;

    @Autowired
    public void setBotMapper(BotRepository botRepository) {
        WebSocketServer.botRepository = botRepository;
    }

    public static RestTemplate restTemplate;

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        WebSocketServer.restTemplate = restTemplate;
    }

    private final String url = "http://localhost:8081";

    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) throws IOException {
        // 建立连接
        this.session = session;
        System.out.println("建立连接");
        Long userId = JwtUtil.getSubject(token);
        this.user = userRepository.findById(userId).orElse(null);

        if (ObjectUtil.isNotNull(this.user)) {
            users.put(userId, this);
        } else {
            this.session.close();
        }
    }

    @OnClose
    public void onClose() {
        // 关闭链接
        System.out.println("关闭连接");
        if (this.user != null) {
            users.remove(this.user.getId());
        }
    }


    void move(int direction) {
        if (game == null) {
            return;
        }
        if (game.getPlayerA().getId().equals(this.user.getId())) {
            if (game.getPlayerA().getBotId().equals(-1L))
                game.setNextStepA(direction);
        } else if (game.getPlayerB().getId().equals(this.user.getId())) {
            if (game.getPlayerB().getBotId().equals(-1L))
                game.setNextStepB(direction);
        }
    }


    @OnMessage
    public void onMessage(String message, Session session) {
        // 从Client接收消息
        JSONObject data = JSONUtil.toBean(message, JSONObject.class);
        String event = data.getStr("event");
        Integer botId = data.getInt("botId");

        if (Constants.START_MATCH.equals(event)) {
            startMatching(botId);
        } else if (Constants.STOP_MATCH.equals(event)) {
            stopMatching();
        } else if (Constants.MOVE.equals(event)) {
            move(data.getInt("direction"));
        }

    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }


    public void sendMessage(String message) {
        synchronized (this.session) {
            // 发送消息到Client
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void startGame(Long aId, Long aBotId, Long bId, Long bBotId) {
        User user1 = userRepository.findById(aId).orElse(null), user2 = userRepository.findById(bId).orElse(null);

        Bot aBot = botRepository.findById(aBotId).orElse(null), bBot = botRepository.findById(bBotId).orElse(null);

        Game game = new Game(13, 14, user1.getId(), aBot, user2.getId(), bBot);
        int[][] map = game.generate();
        if (ObjectUtil.isNotNull(users.get(user1.getId())))
            users.get(user1.getId()).game = game;
        if (ObjectUtil.isNotNull(users.get(user2.getId())))
            users.get(user2.getId()).game = game;

        game.start();

        JSONObject respGame = JSONUtil.createObj()
                .putOnce("a_id", game.getPlayerA().getId())
                .putOnce("b_id", game.getPlayerB().getId())
                .putOnce("a_sx", game.getPlayerA().getSx())
                .putOnce("a_sy", game.getPlayerA().getSy())
                .putOnce("b_sx", game.getPlayerB().getSx())
                .putOnce("b_sy", game.getPlayerB().getSy())
                .putOnce("map", map);

        WebSocketServer server1;
        if (ObjectUtil.isNotNull(users.get(user1.getId()))) {
            server1 = users.get(user1.getId());
            server1.sendMessage(
                    JSONUtil.toJsonStr(
                            new JSONObject()
                                    .set("event", Constants.MATCH_SUCCESS)
                                    .set("data", user2)
                                    .set("game", respGame)
                    )
            );
        }
        WebSocketServer server2;
        if (ObjectUtil.isNotNull(users.get(user2.getId()))) {
            server2 = users.get(user2.getId());
            server2.sendMessage(
                    JSONUtil.toJsonStr(
                            new JSONObject()
                                    .set("event", Constants.MATCH_SUCCESS)
                                    .set("data", user1)
                                    .set("game", respGame)
                    )
            );
        }
    }

    /**
     * 向匹配服务器发送匹配请求，添加一名玩家
     */
    private void startMatching(Integer botId) {
        String route = "/matching/player/add";
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("userId", this.user.getId().toString());
        data.add("rating", this.user.getRating().toString());
        data.add("botId", botId.toString());
        String resp = restTemplate.postForObject(url + route, data, String.class);
        JSONObject respData = JSONUtil.toBean(resp, JSONObject.class);
    }

    /**
     * 向匹配服务器发送匹配请求，删除一名玩家
     */
    private void stopMatching() {
        String route = "/matching/player/remove";
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("userId", this.user.getId().toString());
        String resp = restTemplate.postForObject(url + route, data, String.class);

        JSONObject respData = JSONUtil.toBean(resp, JSONObject.class);

    }

}
