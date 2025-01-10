package com.kob.backend.dto;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Data
public class UserParam {

    @NotNull(groups = EditValid.class, message = "id不能为空")
    private Long id;

    @NotEmpty(groups = {AddValid.class, EditValid.class}, message = "用户名不能为空")
    private String username;

    @NotEmpty(groups = {AddValid.class, EditValid.class}, message = "密码不能为空")
    private String password;

    @NotEmpty(groups = {AddValid.class}, message = "确认密码不能为空")
    private String confirmPassword;

    private String avatar;

    private Integer rating;

    private Date createTime;

    private Date updateTime;

}
