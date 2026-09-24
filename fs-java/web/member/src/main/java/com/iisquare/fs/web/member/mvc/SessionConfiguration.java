package com.iisquare.fs.web.member.mvc;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;

import java.time.Duration;

@Configuration
@EnableRedisIndexedHttpSession
public class SessionConfiguration {

    @Value("${server.servlet.session.timeout:30d}")
    private Duration timeout;

    @Value("${server.servlet.session.cookie.max-age}")
    private Duration maxAge;

    @Bean
    public SessionIdResolver httpSessionIdResolver() {
        return new SessionIdResolver(maxAge);
    }

    /**
     * 会话有效期取自server.servlet.session.timeout，避免与注解中的固定值不一致
     * 在全部单例初始化完成后设置，确保晚于@EnableRedisIndexedHttpSession的默认值生效
     */
    @Bean
    public SmartInitializingSingleton sessionTimeoutInitializer(RedisIndexedSessionRepository sessionRepository) {
        return () -> sessionRepository.setDefaultMaxInactiveInterval(timeout);
    }

}
