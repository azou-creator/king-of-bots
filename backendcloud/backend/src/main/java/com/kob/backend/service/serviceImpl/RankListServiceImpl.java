package com.kob.backend.service.serviceImpl;

import cn.hutool.core.convert.Convert;
import com.kob.backend.common.PageUtils;
import com.kob.backend.entity.User;
import com.kob.backend.repository.UserRepository;
import com.kob.backend.service.RankListService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;


@Service
public class RankListServiceImpl implements RankListService {

    @Resource
    private UserRepository userRepository;

    @Override
    public  PageUtils<User> getRankList(Map<String, Object> params) {
        // @RequestParam Map 的值全是 String，不能用强转；Convert 兼容 String/Integer
        int page = Math.max(Convert.toInt(params.get("page"), 1) - 1, 0);
        int limit = Math.max(Convert.toInt(params.get("limit"), 10), 1);
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Order.desc("rating")));
        Page<User> all = userRepository.findAll(pageRequest);
        return new PageUtils<>(all);
    }

}
