package com.kob.matchingsystem.core;


import cn.hutool.log.Log;
import com.kob.matchingsystem.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 匹配池
 */
@Component
public class MatchingPool extends Thread {

    private final Log log = Log.get();

    private static List<Player> players = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();

    // 显式 IPv4：localhost 在部分环境解析为 ::1，会被主服务的 IP 白名单拒绝
    private static final String STARTGAMEURL = "http://127.0.0.1:8080/startGame/start";

    private static RestTemplate restTemplate;

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        MatchingPool.restTemplate = restTemplate;
    }

    public void addPlayer(Integer userId, Integer rating, Integer botId) {
        lock.lock();
        try {
            // 同一玩家重复请求只保留一条，防止"自己和自己"匹配
            players.removeIf(player -> player.getUserId().equals(userId));
            players.add(new Player(userId, rating, 0, botId));
        } finally {
            lock.unlock();
        }
    }

    public void removePlayer(Integer userId) {
        lock.lock();
        try {
            players.removeIf(player -> player.getUserId().equals(userId));
        } finally {
            lock.unlock();
        }
    }

    /**
     * 每隔一秒，等待时间加一
     */
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                lock.lock();
                try {
                    increaseWaitingTime();
                    matchPlayers();
                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * 判断两名玩家是否匹配
     *
     * @param player1
     * @param player2
     * @return
     */
    private boolean checkMatched(Player player1, Player player2) {
        int ratingGap = Math.abs(player1.getRating() - player2.getRating());
        int waitingTimeGap = Math.min(player1.getWaitingTime(), player2.getWaitingTime());
        return ratingGap <= waitingTimeGap * 10;
    }

    private void sendResult(Player player1, Player player2) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("aId", player1.getUserId().toString());
        data.add("aBotId", player1.getBotId().toString());
        data.add("bId", player2.getUserId().toString());
        data.add("bBotId", player2.getBotId().toString());
        restTemplate.postForObject(STARTGAMEURL, data, String.class);
    }

    /**
     * 尝试匹配所有玩家
     */
    private void matchPlayers() {
        boolean[] matched = new boolean[players.size()];
        for (int i = 0; i < players.size(); i++) {
            if (matched[i]) continue;
            for (int j = i + 1; j < players.size(); j++) {
                if (matched[j]) continue;
                Player player1 = players.get(i), player2 = players.get(j);
                if (checkMatched(player1, player2)) {
                    matched[i] = matched[j] = true;
                    sendResult(player1, player2);
                    break;
                }
            }
        }

        List<Player> newList = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (!matched[i]) newList.add(players.get(i));
        }
        players = newList;
    }

    private void increaseWaitingTime() {
        lock.lock();
        try {
            players.forEach(player -> player.setWaitingTime(player.getWaitingTime() + 1));
        } finally {
            lock.unlock();
        }
    }

}
