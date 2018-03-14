package com.proptiger.app.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.service.HealthCheckService;
//import com.proptiger.app.service.RatingParameterService;
import com.proptiger.app.services.srf.AccountLockedEventsService;
import com.proptiger.core.exception.ProAPIException;
import com.proptiger.core.meta.DisableCaching;
import com.proptiger.core.pojo.response.APIResponse;



@Controller
//@RequestMapping(value="/healthcheck")
public class HealthCheckController {

	@Autowired
	HealthCheckService healthService;

//	@Autowired
//	RatingParameterService ratingParameterService;
	
	@Autowired
	AccountLockedEventsService accountLockedEventsService;
	
	@Value("${health.check.time.out.duration.sec}")
	private String healthCheckTimeOutDurationSec;

	@RequestMapping(value = "ping", method = RequestMethod.GET)
	@DisableCaching
	@ResponseBody
	public APIResponse ping() {
		return new APIResponse("pong");
	}
	
	
//	@RequestMapping(value = "healthcheck", method = RequestMethod.GET)
//	@DisableCaching
//	@ResponseBody
//	public APIResponse healthCheck() {
//
//		Map<String, String> healthStatus = new HashMap<>();
//		long startTime = System.currentTimeMillis();
//		// check Db
//		ratingParameterService.getRatingParametersById(1);
//		healthStatus.put("DB", "ok");
//
//		// check redis
//		healthService.checkRedis();
//		healthStatus.put("Redis", "ok");
//
//		long endTime = System.currentTimeMillis();
//		long secDiff = (endTime - startTime) / 1000;
//		if (secDiff > Integer.parseInt(healthCheckTimeOutDurationSec)) {
//			throw new ProAPIException("themis health check took more than specified sec");
//		}
//		return new APIResponse(healthStatus);
//	}
//	@RequestMapping(value = "accountlocked", method = RequestMethod.GET)
//	@DisableCaching
//	@ResponseBody
//	public APIResponse accountcheck()
//	{
//		accountLockedEventsService.
//	}
}

