package com.kob.backend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.kob.backend.common.PageUtils;
import com.kob.backend.common.R;
import com.kob.backend.common.SecurityUtils;
import com.kob.backend.dto.AddValid;
import com.kob.backend.dto.BotParam;
import com.kob.backend.dto.EditValid;
import com.kob.backend.entity.Bot;
import com.kob.backend.entity.User;
import com.kob.backend.service.IBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bot")
public class BotController {

    @Autowired
    private IBotService botService;

    @PostMapping("/add")
    public R<String> add(@Validated(AddValid.class) @RequestBody BotParam botParam) {
        Bot bot = BeanUtil.copyProperties(botParam, Bot.class);
        bot.setUserId(SecurityUtils.getUser().getId());
        botService.save(bot);
        return R.ok("添加成功");
    }

    @GetMapping("/{id}")
    public R<Bot> get(@PathVariable("id") Long id) {
        Bot bot = botService.getById(id);
        return R.ok(bot);
    }


    @DeleteMapping("/remove/{id}")
    public R<String> remove(@PathVariable("id") Long id) {
        User user = SecurityUtils.getUser();
        Bot bot = botService.getById(id);
        if (!ObjectUtil.equal(bot.getUserId(), user.getId())) {
            return R.fail("没有操作权限");
        }
        boolean b = botService.deleteById(id);
        if (b) {
            return R.ok("删除成功");
        } else {
            return R.fail("删除失败");
        }
    }


    @PutMapping("/update")
    public R<String> update(@Validated(EditValid.class) @RequestBody BotParam botParam) {
        // 校验归属并保留不可篡改字段(userId/createTime), 防止越权修改他人 bot 或清空归属
        Bot origin = botService.getById(botParam.getId());
        if (origin == null || !ObjectUtil.equal(origin.getUserId(), SecurityUtils.getUser().getId())) {
            return R.fail("没有操作权限");
        }
        Bot bot = BeanUtil.copyProperties(botParam, Bot.class);
        bot.setUserId(origin.getUserId());
        bot.setCreateTime(origin.getCreateTime());
        boolean b = botService.updateById(bot);
        if (b) {
            return R.ok("更新成功");
        } else {
            return R.fail("更新失败");
        }
    }

    @GetMapping("/list")
    public R<PageUtils<Bot>> list(@RequestParam Map<String, Object> params) {
        // 返回 PageUtils(list/totalCount/totalPage), 与 rankList 接口结构一致(前端按此解构)
        return R.ok(new PageUtils<>(botService.page(params)));
    }

}
