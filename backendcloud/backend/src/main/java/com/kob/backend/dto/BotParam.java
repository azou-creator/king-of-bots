package com.kob.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Data
public class BotParam {

    @NotNull(groups = {EditValid.class}, message = "id不能为空")
    private Long id;

    private Long userId;

    @NotBlank(groups = {AddValid.class}, message = "机器人名称不能为空")
    private String title;

    private String description;

    private String content;

    private Date createTime;

    private Date updateTime;

}
