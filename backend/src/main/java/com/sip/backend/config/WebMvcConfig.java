package com.sip.backend.config;

import com.sip.backend.common.RequestIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebMvcConfig {

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RequestIdFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(0);
        return reg;
    }
}