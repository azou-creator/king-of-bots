package com.kob.backend.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.kob.backend.entity.User;
import lombok.Data;

import java.util.Date;

@Data
public class RecordDTO {


    private Long id;

    // Jackson 对 getAId() 这类 getter 会推断出全小写键(aid/asteps), 显式固定与前端约定一致的键名
    @JsonProperty("aId")
    private Long aId;

    @JsonProperty("aSx")
    private Integer aSx;

    @JsonProperty("aSy")
    private Integer aSy;

    @JsonProperty("bId")
    private Long bId;

    @JsonProperty("bSx")
    private Integer bSx;

    @JsonProperty("bSy")
    private Integer bSy;

    @JsonProperty("aSteps")
    private String aSteps;

    @JsonProperty("bSteps")
    private String bSteps;

    private String map;

    private String loser;

    private Date createTime;

    private User userA;

    private User userB;



}
