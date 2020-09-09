package com.ncr.storage.config.spring;

import com.ncr.storage.web.filter.RequestContextInitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

/**
 * Servlet filter(s) initializations
 */
@Configuration
public class ServletFilterConfig {
    /**
     * @return
     */
    @Bean
    public Filter requestContextInitFilter() {
        return new RequestContextInitFilter();
    }
}
