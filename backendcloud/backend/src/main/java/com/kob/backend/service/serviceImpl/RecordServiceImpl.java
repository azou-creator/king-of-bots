package com.kob.backend.service.serviceImpl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.kob.backend.dto.RecordDTO;
import com.kob.backend.entity.Record;
import com.kob.backend.entity.User;
import com.kob.backend.repository.RecordRepository;
import com.kob.backend.service.IRecordService;
import com.kob.backend.service.IUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RecordServiceImpl implements IRecordService {

    @Resource
    private IUserService userService;

    @Resource
    private RecordRepository recordRepository;

    @Override
    public JSONObject pageList(Map<String, Object> params) {
        int page = (int) params.getOrDefault("page", 1);
        int size = (int) params.getOrDefault("size", 10);
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<Record> recordPage = recordRepository.findAll(pageRequest);

        JSONObject jsonObject = new JSONObject();
        jsonObject.putOpt("currPage", recordPage.getNumber() + 1);
        jsonObject.putOpt("pageSize", recordPage.getSize());
        jsonObject.putOpt("totalPage", recordPage.getTotalPages());
        jsonObject.putOpt("totalCount", recordPage.getTotalElements());

        List<Record> list = recordPage.getContent();
        List<RecordDTO> recordDTOList = new ArrayList<>();
        for (Record record : list) {
            User userA = userService.getById(record.getAId());
            User userB = userService.getById(record.getBId());
            RecordDTO recordDTO = BeanUtil.copyProperties(record, RecordDTO.class);
            recordDTO.setUserA(userA);
            recordDTO.setUserB(userB);
            recordDTOList.add(recordDTO);
        }

        jsonObject.putOpt("list", recordDTOList);
        return jsonObject;
    }

}
