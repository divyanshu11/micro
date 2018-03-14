package com.proptiger.app.config;

import org.springframework.context.annotation.Configuration;

import com.proptiger.core.config.SessionRepositoryConfigurer;

/**
 * This class configures session repository and other dependency to get session
 * of active user from redis or userservice and set that in request scope
 * 
 * */

@Configuration
public class AppSessionConfig extends SessionRepositoryConfigurer{

	
	
}
