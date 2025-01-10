package com.kob.backend.service;


import com.kob.backend.entity.Bot;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface IBotService{

     Bot save(Bot bot);

     Bot getById(Long id);

     boolean deleteById(Long id);

     boolean updateById(Bot bot);

     Page<Bot> page(Map<String, Object> params);
}
