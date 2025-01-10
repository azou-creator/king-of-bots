package com.kob.backend.service.serviceImpl;

import com.kob.backend.common.SecurityUtils;
import com.kob.backend.entity.Bot;
import com.kob.backend.entity.User;
import com.kob.backend.repository.BotRepository;
import com.kob.backend.service.IBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BotServiceImplService implements IBotService {

    @Autowired
    private BotRepository botRepository;

    @Override
    public Bot save(Bot bot) {
        return botRepository.save(bot);
    }

    @Override
    public Bot getById(Long id) {
        return botRepository.findById(id).orElse(null);
    }

    @Override
    public boolean deleteById(Long id) {
        try {
            botRepository.deleteById(id);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean updateById(Bot bot) {
        try {
            botRepository.save(bot);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public Page<Bot> page(Map<String, Object> params) {
        int page = Integer.parseInt(params.get("page").toString());
        int size = Integer.parseInt(params.get("limit").toString());
        User user = SecurityUtils.getUser();
        String title = "";
        if (params.get("title") != null) {
            title = params.get("title").toString();
        }
        String finalTitle = title;
        Specification<Bot> spec = new Specification<Bot>() {
            @Override
            public Predicate toPredicate(Root<Bot> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<Predicate>();
                if (StringUtils.hasLength(finalTitle))
                    predicates.add(criteriaBuilder.like(root.get("title"), "%" + finalTitle + "%"));
                Long id = user.getId();
                predicates.add(criteriaBuilder.equal(root.get("id"), id));
                return query.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
            }
        };

        Pageable pageable = PageRequest.of(page, size);
        return botRepository.findAll(spec, pageable);
    }
}
