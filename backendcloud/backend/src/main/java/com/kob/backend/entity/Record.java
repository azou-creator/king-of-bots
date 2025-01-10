package com.kob.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.*;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "record")
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long aId;

    private Integer aSx;

    private Integer aSy;

    private Long bId;

    private Integer bSx;

    private Integer bSy;

    private String aSteps;

    private String bSteps;

    private String map;

    private String loser;

    @CreatedDate
    private Date createTime;
}
