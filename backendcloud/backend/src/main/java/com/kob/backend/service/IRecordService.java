package com.kob.backend.service;


import cn.hutool.json.JSONObject;
import com.kob.backend.common.PageUtils;
import com.kob.backend.entity.Record;

import java.util.List;
import java.util.Map;

public interface IRecordService {


    JSONObject pageList(Map<String, Object> params);

}
