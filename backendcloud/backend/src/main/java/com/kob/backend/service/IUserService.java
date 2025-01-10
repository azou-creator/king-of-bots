package com.kob.backend.service;

import com.kob.backend.dto.UserParam;
import com.kob.backend.entity.User;

import java.util.List;
import java.util.Map;

public interface IUserService{


    Map<String, String> login(UserParam userParam) ;

    Map<String, String> register(UserParam userParam) ;

    User info(Long id) ;

    User getById(Long id) ;
}
