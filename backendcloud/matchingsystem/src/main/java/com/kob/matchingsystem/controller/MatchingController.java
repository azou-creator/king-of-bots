package com.kob.matchingsystem.controller;


import com.kob.matchingsystem.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/matching")
public class MatchingController {

    @Autowired
    private MatchingService matchingService;


    @PostMapping("/player/add")
    public String addPlayer(@RequestParam MultiValueMap<String, String> map) {
        Integer userId = Integer.valueOf(Objects.requireNonNull(map.getFirst("userId")));
        Integer rating = Integer.valueOf(Objects.requireNonNull(map.getFirst("rating")));
        Integer botId = Integer.valueOf(Objects.requireNonNull(map.getFirst("botId")));
        return matchingService.addPlayer(userId, rating, botId);
    }


    // 调用方（WebSocketServer.stopMatching）发的是 POST，用 DELETE 会导致 403/405
    @PostMapping("/player/remove")
    public String removePlayer(@RequestParam MultiValueMap<String, String> map) {
        Integer userId = Integer.valueOf(Objects.requireNonNull(map.getFirst("userId")));
        return matchingService.removePlayer(userId);
    }

}
