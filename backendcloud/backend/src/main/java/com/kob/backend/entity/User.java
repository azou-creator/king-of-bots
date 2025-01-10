package com.kob.backend.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private String avatar;

    @ColumnDefault("1500")
    private Integer rating;

    @CreatedDate
    private Date createTime;

    @LastModifiedDate
    private Date updateTime;

    private Integer logic;

}
