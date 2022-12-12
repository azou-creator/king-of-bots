package com.kob.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kob.backend.common.PageUtils;
import com.kob.backend.entity.User;

import java.util.Map;

public interface RankListService extends IService<User> {
    PageUtils<User> getRankList(Map<String, Object> params);
}
