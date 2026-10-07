package com.kob.backend.service;

import com.kob.backend.dto.UserParam;
import com.kob.backend.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface IUserService{


    Map<String, String> login(UserParam userParam) ;

    Map<String, String> register(UserParam userParam) ;

    User info(Long id) ;

    User getById(Long id) ;

    String updateAvatar(Long userId, MultipartFile file) ;
}
