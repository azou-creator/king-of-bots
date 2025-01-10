package com.kob.backend.service.serviceImpl;

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
        int page = (int) params.getOrDefault("page", 1);
        int limit = (int) params.getOrDefault("limit", 10);
        PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(Sort.Order.desc("rating")));
        Page<User> all = userRepository.findAll(pageRequest);
        return new PageUtils<>(all);
    }

}
