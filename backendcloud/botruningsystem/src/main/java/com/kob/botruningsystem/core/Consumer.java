package com.kob.botruningsystem.core;

import cn.hutool.core.lang.UUID;
import cn.hutool.log.Log;
import com.kob.botruningsystem.model.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.function.Supplier;

@Component
public class Consumer extends Thread {

    private final Log log = Log.get();
    private Bot bot;

    private static RestTemplate restTemplate;

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        Consumer.restTemplate = restTemplate;
    }

    public void startTimeout(long timeout, Bot bot) {
        this.bot = bot;
        this.start();
        try {
            // 最多等待timeout毫秒
            this.join(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            this.interrupt();
        }
    }

    @Override
    public void run() {
        try {
            String uid = UUID.randomUUID(true).toString().substring(0, 8);
            // 内存编译(替代 joor: joor 0.9.14 在 JDK 16+ 强封装下静默失败), 编译错误会带诊断抛出
            BotCompiler.SupplierHolder holder = BotCompiler.compile(
                    "com.kob.botruningsystem.reflect.Bot" + uid,
                    addUid(bot.getBotCode(), uid));

            File file = new File("input.txt");
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.println(bot.getInput());
                pw.flush();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            Supplier<Integer> botInterface = holder.get();
            Integer direction = botInterface.get();
            log.info("bot " + bot.getUserId() + " -> direction " + direction);

            String URL = "http://127.0.0.1:8080/receiveBotMove/move";
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("userId", bot.getUserId().toString());
            map.add("direction", direction.toString());

            restTemplate.postForObject(URL, map, String.class);
        } catch (Exception e) {
            // 编译失败/运行超时等, 记录后本轮该玩家按超时判负, 不能让线程异常退出影响后续对局
            log.error("bot run failed: ", e);
        }
    }

    /**
     * 在code中类名后面加上uid，防止多个线程同时编译同一个类
     *
     * @param code
     * @param uid
     * @return
     */
    private String addUid(String code, String uid) {
        int k = code.indexOf(" implements Supplier<Integer>");
        return code.substring(0, k) + uid + code.substring(k);
    }

}
