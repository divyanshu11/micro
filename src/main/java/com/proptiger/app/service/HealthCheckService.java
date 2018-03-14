package com.proptiger.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proptiger.core.exception.ProAPIException;
import com.proptiger.core.util.Caching;
import com.proptiger.core.util.Constants;


@Service
public class HealthCheckService {
	private static final String REDIS_CONNECTION_ERROR = "Cannot connect to redis server";
	private static Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

	@Autowired
	private Caching caching;

	public void checkRedis() {
		int value = 1;
		String key = Constants.CacheName.CACHE;
		try {
			caching.saveResponse(key, value);
			Integer redisValue = caching.getSavedResponse(key, Integer.class);
			caching.deleteResponseFromCache(key);

			if (redisValue == null || redisValue != value) {
				throw new ProAPIException(REDIS_CONNECTION_ERROR);
			}
		} catch (Exception e) {
			logger.error(REDIS_CONNECTION_ERROR, e);
			throw new ProAPIException(REDIS_CONNECTION_ERROR);
		}
	}

}
