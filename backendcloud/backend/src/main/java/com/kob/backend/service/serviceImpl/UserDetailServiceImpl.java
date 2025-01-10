package com.kob.backend.service.serviceImpl;

import cn.hutool.core.util.ObjectUtil;
import com.kob.backend.entity.User;
import com.kob.backend.entity.UserDetailsImpl;
import com.kob.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("username"), username));
            return query.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
        };
        Optional<User> one = userRepository.findOne(spec);
        if (ObjectUtil.isNull(one.orElse(null))) {
            throw new UsernameNotFoundException("用户不存在");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl();
        userDetails.setUser(one.get());

        return userDetails;
    }
}
