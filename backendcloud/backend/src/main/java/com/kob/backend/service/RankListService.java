package com.kob.backend.service;

import com.kob.backend.common.PageUtils;
import com.kob.backend.entity.User;

import java.util.Map;

public interface RankListService  {
    PageUtils<User> getRankList(Map<String, Object> params);
}
