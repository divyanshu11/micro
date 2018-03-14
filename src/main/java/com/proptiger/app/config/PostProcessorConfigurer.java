package com.proptiger.app.config;

import org.springframework.context.annotation.Bean;

public class PostProcessorConfigurer {
	public PostProcessorConfigurer() {

    }

    @Bean
    public static CustomBeanPostProcessor createPostProcessor() {
        return new CustomBeanPostProcessor();
    }

}
