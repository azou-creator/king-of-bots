package com.kob.backend.service.serviceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.kob.backend.common.Constants;
import com.kob.backend.common.JwtUtil;
import com.kob.backend.dto.UserParam;
import com.kob.backend.entity.User;
import com.kob.backend.entity.UserDetailsImpl;
import com.kob.backend.repository.UserRepository;
import com.kob.backend.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        // 双保险：除 @Validated 校验外，service 层对空值再做防御
        if (StrUtil.hasBlank(userParam.getUsername(), userParam.getPassword())) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(userParam.getUsername(), userParam.getPassword());
        // 登录失败会抛 AuthenticationException，由 GlobalExceptionHandler 统一转换为错误响应
        Authentication authenticate = authenticationManager.authenticate(usernamePasswordAuthenticationToken);

        UserDetailsImpl userDetails = (UserDetailsImpl) authenticate.getPrincipal();
        User user = userDetails.getUser();
        String jwt = JwtUtil.createJWT(user.getId().toString());
        return MapUtil.builder("token", jwt)
                .put("username", user.getUsername())
                .put("id", user.getId().toString())
                .put("avatar", StrUtil.nullToEmpty(user.getAvatar()))
                .build();
    }


    public Map<String, String> register(UserParam userParam) {
        if (StrUtil.hasBlank(userParam.getUsername(), userParam.getPassword(), userParam.getConfirmPassword())) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }
        if (!StrUtil.equals(userParam.getPassword(), userParam.getConfirmPassword())) {
            throw new IllegalArgumentException("两次密码不一致");
        }
        // 判断用户名是否存在（并发场景由数据库唯一约束兜底，见 DataIntegrityViolationException）
        List<User> users = userRepository.findByUsername(userParam.getUsername());
        if (!users.isEmpty()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = BeanUtil.copyProperties(userParam, User.class);
        String encode = passwordEncoder.encode(userParam.getPassword());
        user.setPassword(encode);
        // 新用户默认天梯分（@ColumnDefault 不生效：JPA 会把 null 显式写入覆盖库默认值）
        user.setRating(Constants.DEFAULT_RATING);
        log.info(user.toString());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataAccessException e) {
            log.error(e);
            throw new IllegalArgumentException("用户名已存在或注册失败");
        }
        return MapUtil.of("msg", "注册成功");
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

    // 头像允许的扩展名
    private static final List<String> AVATAR_EXTS = List.of("jpg", "jpeg", "png", "webp", "gif");

    @Value("${avatar.dir}")
    private String avatarDir;

    @Value("${server.port}")
    private int serverPort;

    @Override
    public String updateAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像文件");
        }
        String ext = extOf(file.getOriginalFilename());
        if (!AVATAR_EXTS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 " + String.join("/", AVATAR_EXTS) + " 格式的图片");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(avatarDir).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);
            // 文件名只用 userId + 校验过的扩展名, 不引入任何用户输入, 无路径穿越风险
            java.nio.file.Path target = dir.resolve(userId + "." + ext);
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            // URL 带时间戳, 避免浏览器缓存旧头像
            String url = "http://127.0.0.1:" + serverPort + "/avatar/" + userId + "." + ext
                    + "?t=" + System.currentTimeMillis();
            user.setAvatar(url);
            userRepository.save(user);
            return url;
        } catch (java.io.IOException e) {
            log.error(e);
            throw new RuntimeException("头像保存失败");
        }
    }

    private String extOf(String filename) {
        if (filename == null) return "";
        int k = filename.lastIndexOf('.');
        return k < 0 ? "" : filename.substring(k + 1).toLowerCase();
    }


}
