package com.kob.backend.controller;

import cn.hutool.json.JSONObject;
import com.kob.backend.common.PageUtils;
import com.kob.backend.common.R;
import com.kob.backend.dto.RecordDTO;
import com.kob.backend.service.IRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/record")
public class RecordController {


    @Autowired
    private IRecordService recordService;


    @GetMapping("/list")
    public R<JSONObject> list(@RequestParam Map<String, Object> params) {
        return R.ok(recordService.pageList(params));
    }

    /** 单条对局记录(回放页按 id 加载) */
    @GetMapping("/{id}")
    public R<RecordDTO> get(@PathVariable("id") Long id) {
        return R.ok(recordService.getById(id));
    }


}
