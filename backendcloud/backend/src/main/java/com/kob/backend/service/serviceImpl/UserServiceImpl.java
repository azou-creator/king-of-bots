package com.kob.backend.service.serviceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.kob.backend.common.JwtUtil;
import com.kob.backend.dto.UserParam;
import com.kob.backend.entity.User;
import com.kob.backend.entity.UserDetailsImpl;
import com.kob.backend.repository.UserRepository;
import com.kob.backend.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements IUserService {

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserRepository userRepository;

    private final Log log = Log.get(this.getClass());

    public Map<String, String> login(UserParam userParam) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(userParam.getUsername(), userParam.getPassword());
        // 登录失败，会自动抛出异常
        Authentication authenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken);

        UserDetailsImpl userDetails = (UserDetailsImpl) authenticate.getPrincipal();
        User user = userDetails.getUser();
        String jwt = JwtUtil.createJWT(user.getId().toString());
        return MapUtil.builder("token", jwt)
                .put("username", user.getUsername())
                .put("id", user.getId().toString())
                .put("avatar", user.getAvatar())
                .build();
    }


    public Map<String, String> register(UserParam userParam) {
        if (StrUtil.hasBlank(userParam.getUsername(), userParam.getPassword(), userParam.getConfirmPassword())) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }
        if (!StrUtil.equals(userParam.getPassword(), userParam.getConfirmPassword())) {
            throw new IllegalArgumentException("两次密码不一致");
        }
        // 判断用户名是否存在
        List<User> users = userRepository.findByUsername(userParam.getUsername());

        HashMap<String, String> res = new HashMap<>();
        if (!users.isEmpty()) {
            res.put("msg", "用户名已存在");
            throw new RuntimeException("用户名已存在");
        }
        User user = BeanUtil.copyProperties(userParam, User.class);
        String encode = passwordEncoder.encode(userParam.getPassword());
        user.setPassword(encode);
        log.info(user.toString());
        try {
            userRepository.saveAndFlush(user);
            res.put("msg", "注册成功");
        } catch (DataAccessException e) {
            log.error(e);
            res.put("msg", "注册失败");
        }
        return res;
    }


    public User info(Long id) {
        UsernamePasswordAuthenticationToken authenticationToken =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authenticationToken.getPrincipal();

        return userDetails.getUser();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }


}
