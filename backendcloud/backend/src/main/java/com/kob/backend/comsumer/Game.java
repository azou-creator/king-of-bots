package com.kob.backend.comsumer;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import com.kob.backend.common.Constants;
import com.kob.backend.entity.Bot;
import com.kob.backend.entity.Record;
import com.kob.backend.entity.User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 为了实现两名玩家有一个共同的地图，所以需要一个地图类
 * 在后端生成地图后，将地图的信息传给前端
 */
public class Game extends Thread {

    private final Log log = Log.get();

    private int rows = 13;

    private int cols = 14;

    private int[][] g;

    private Player playerA, playerB;

    private Integer nextStepA = null;

    private Integer nextStepB = null;

    private String status = "playing"; // 游戏状态 finished, playing

    private String loser = ""; // all: 平局, A: 玩家1输, B: 玩家2输

    private final ReentrantLock lock = new ReentrantLock();

    public Game() {
        g = new int[rows][cols];
        initMap();
    }

    public Game(int rows, int cols, Long player1Id, Bot aBot, Long player2Id, Bot bBot) {
        this.rows = rows;
        this.cols = cols;
        g = new int[rows][cols];
        initMap();

        Long aBotId = aBot == null ? -1 : aBot.getId();
        Long bBotId = bBot == null ? -1 : bBot.getId();

        String aBotCode = aBot == null ? "" : aBot.getContent();
        String bBotCode = bBot == null ? "" : bBot.getContent();

        playerA = new Player(player1Id, rows - 2, 1, aBotId, aBotCode, new ArrayList<>());
        playerB = new Player(player2Id, 1, cols - 2, bBotId, bBotCode, new ArrayList<>());
    }

    /**
     * 将当前局面的信息编码成字符串
     * 地图 + # + 自己的起始坐标 + # + 我的操作 + # + 对方的起始坐标 + # + 对方的操作
     *
     * @param player
     * @return
     */
    private String getInput(Player player) {
        Player me, you;
        if (player.getId().equals(playerA.getId())) {
            me = playerA;
            you = playerB;
        } else {
            me = playerB;
            you = playerA;
        }
        return getMapString() + "#" + me.getSx() + "#" + me.getSy() + "#(" +
                me.getStepsString() + ")#" +
                you.getSx() + "#" + you.getSy() + "#(" +
                you.getStepsString() + ")";
    }


    private void sendBotCode(Player player) {
        if (player.getBotId() == -1) {
            return;
        }
        String url = "http://localhost:8082/bot/add";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("userId", player.getId().toString());
        params.add("botCode", player.getBotCode());
        params.add("input", getInput(player));
        WebSocketServer.restTemplate.postForObject(url, params, String.class);
    }

    /**
     * 等待获取两名玩家的下一步操作
     * 在5秒钟内，如果两名玩家都没有操作，则游戏结束
     *
     * @return true: 两名玩家都已经下完一步, false: 两名玩家还没有都下完一步
     */
    private boolean nextStep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        sendBotCode(playerA);
        sendBotCode(playerB);

        for (int i = 0; i < 25; i++) {
            try {
                Thread.sleep(200);
                lock.lock();
                try {
                    if (ObjectUtil.isNotNull(nextStepA) && ObjectUtil.isNotNull(nextStepB)) {
                        playerA.getSteps().add(nextStepA);
                        playerB.getSteps().add(nextStepB);
                        return true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private boolean check_valid(List<Cell> cellsA, List<Cell> cellsB) {
        int n = cellsA.size();
        Cell cell = cellsA.get(n - 1);
        if (g[cell.getX()][cell.getY()] == 1) return false;

        for (int i = 0; i < n - 1; i++) {
            if (cellsA.get(i).getX() == cell.getX() && cellsA.get(i).getY() == cell.getY()) return false;
        }
        for (int i = 0; i < n - 1; i++) {
            if (cellsB.get(i).getX() == cell.getX() && cellsB.get(i).getY() == cell.getY()) return false;
        }
        return true;
    }

    /**
     * 判断两名玩家操作是否合法
     */
    private void judge() {
        List<Cell> cellsA = playerA.getCells();
        List<Cell> cellsB = playerB.getCells();

        boolean validA = check_valid(cellsA, cellsB);
        boolean validB = check_valid(cellsB, cellsA);

        if (!validA || !validB) {
            status = Constants.FINISH;
            if (!validA && !validB) {
                loser = Constants.ALL;
            } else if (!validA) {
                loser = Constants.PLAYER_A;
            } else {
                loser = Constants.PLAYER_B;
            }
        }


    }

    public void sendAllMessage(String message) {
        if (ObjectUtil.isNotNull(WebSocketServer.users.get(playerA.getId())))
            WebSocketServer.users.get(playerA.getId()).sendMessage(message);
        if (ObjectUtil.isNotNull(WebSocketServer.users.get(playerB.getId())))
            WebSocketServer.users.get(playerB.getId()).sendMessage(message);
    }

    /**
     * 向两名玩家广播下一步操作
     */
    private void sendMove() {
        lock.lock();
        try {
            JSONObject jsonObject = JSONUtil.createObj()
                    .putOnce("event", "move")
                    .putOnce("a_dir", nextStepA)
                    .putOnce("b_dir", nextStepB);
            nextStepA = nextStepB = null;
            sendAllMessage(jsonObject.toJSONString(4));
        } finally {
            lock.unlock();
        }
    }

    private void saveToDB() {
        User userA = WebSocketServer.userRepository.findById(playerA.getId()).orElse(null);
        User userB = WebSocketServer.userRepository.findById(playerB.getId()).orElse(null);

        Integer ratingA = userA != null ? userA.getRating() : 0;
        Integer ratingB = userB != null ? userB.getRating() : 0;
        // 平局分数不变
        if (Constants.PLAYER_A.equals(loser)) {
            ratingA -= 2;
            ratingB += 5;
        } else if (Constants.PLAYER_B.equals(loser)) {
            ratingA += 5;
            ratingB -= 2;
        }
        updateRating(playerA, ratingA);
        updateRating(playerB, ratingB);

        Record record = new Record(
                null,
                playerA.getId(),
                playerA.getSx(),
                playerA.getSy(),
                playerB.getId(),
                playerB.getSx(),
                playerB.getSy(),
                playerA.getStepsString(),
                playerB.getStepsString(),
                getMapString(),
                loser,
                new Date()
        );
        WebSocketServer.recordRepository.save(record);
    }

    private void updateRating(Player player, Integer rating) {
        User user = WebSocketServer.userRepository.findById(player.getId()).orElse(null);
        if (null != user) {
            user.setRating(rating);
            WebSocketServer.userRepository.save(user);
        }
    }

    private String getMapString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                sb.append(g[i][j]);
            }
        }
        return sb.toString();
    }

    /**
     * 向两个玩家返回结果
     */
    private void sendResult() {
        JSONObject jsonObject = JSONUtil.createObj()
                .putOnce("event", "result")
                .putOnce("loser", loser);
        sendAllMessage(jsonObject.toJSONString(4));

        saveToDB();
    }


    @Override
    public void run() {
        for (int i = 0; i < 1000; i++) {
            if (nextStep()) {
                judge();
                if (Constants.PLAYING.equals(status)) {
                    sendMove();
                } else {
                    sendResult();
                    break;
                }
            } else {
                lock.lock();
                try {
                    status = Constants.FINISH;
                    if (ObjectUtil.isNull(nextStepA) && ObjectUtil.isNull(nextStepB)) {
                        loser = Constants.ALL;
                    } else if (ObjectUtil.isNull(nextStepA)) {
                        loser = Constants.PLAYER_A;
                    } else {
                        loser = Constants.PLAYER_B;
                    }
                } finally {
                    lock.unlock();
                }
                sendResult();
                break;
            }
        }
    }


    public int[][] generate() {
        for (int i = 0; i < 1000; i++) {
            if (createWall()) break;
        }
        return g;
    }

    private void initMap() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                g[i][j] = 0;
            }
        }
    }

    private boolean createWall() {
        // 0 代表没有墙，1 代表有墙
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                g[i][j] = 0;

        for (int i = 0; i < rows; i++)
            g[i][0] = g[i][cols - 1] = 1;
        for (int i = 0; i < cols; i++)
            g[0][i] = g[rows - 1][i] = 1;


        int innerWalls = 20;
        for (int i = 0; i < innerWalls / 2; i++) {
            for (int j = 0; j < 1000; j++) {
                int x = RandomUtil.randomInt(0, rows);
                int y = RandomUtil.randomInt(0, cols);
                if (g[x][y] == 1 || g[rows - 1 - x][cols - 1 - y] == 1) continue;
                if ((x == rows - 2 && y == 1) || (x == 1 && y == cols - 2)) continue;
                g[x][y] = g[rows - 1 - x][cols - 1 - y] = 1;
                break;
            }
        }

        return check_available(rows - 2, 1, 1, cols - 2);
    }

    // flood fill 检查连通性
    private boolean check_available(int sx, int sy, int tx, int ty) {
        if (sx == tx && sy == ty) return true;
        int[] dx = {-1, 0, 1, 0}, dy = {0, 1, 0, -1};
        g[sx][sy] = 1;
        for (int i = 0; i < 4; i++) {
            int x = sx + dx[i], y = sy + dy[i];
            if (x >= 0 && x < this.rows && y >= 0 && y < this.cols && g[x][y] == 0) {
                if (check_available(x, y, tx, ty)) {
                    g[x][y] = 0; // 全局变量, 恢复现场
                    return true;
                }
            }
        }
        g[sx][sy] = 0;
        return false;
    }

    public void setNextStepA(Integer nextStepA) {
        lock.lock();
        try {
            this.nextStepA = nextStepA;
        } finally {
            lock.unlock();
        }
    }

    public void setNextStepB(Integer nextStepB) {
        lock.lock();
        try {
            this.nextStepB = nextStepB;
        } finally {
            lock.unlock();
        }
    }

    public Player getPlayerA() {
        return playerA;
    }

    public void setPlayerA(Player playerA) {
        this.playerA = playerA;
    }

    public Player getPlayerB() {
        return playerB;
    }

    public void setPlayerB(Player playerB) {
        this.playerB = playerB;
    }
}
