package com.proptiger.app.mvc.cms;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proptiger.app.model.srf.SellerRelevanceEvent.EventCategory;
import com.proptiger.app.services.srf.SellerRelevanceEventService;
import com.proptiger.core.annotations.InternalIp;
import com.proptiger.core.annotations.LoggedIn;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.meta.DisableCaching;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.APIResponse;
import com.proptiger.core.util.Constants;
//import com.proptiger.data.model.srf.SellerRelevanceEvent.EventCategory;
//import com.proptiger.data.mvc.cms.SellerRelevanceEventService;


@Controller
@DisableCaching
public class SellerRelevanceEventsController {

	 @Autowired
	    private SellerRelevanceEventService sellerRelevanceEventService;

	    @LoggedIn(roles = {"MarketForceCRO"})
	    @ResponseBody
	    @RequestMapping(value = "data/v1/seller-relevance-events", method = RequestMethod.GET)
	    public APIResponse getGroupedSellerRelevanceEvents(
	            @ModelAttribute(Constants.LOGIN_INFO_OBJECT_NAME) ActiveUser activeUser,
	            @RequestParam(required = true) List<EventCategory> eventTypes,
	            @RequestParam(required = false) Set<Integer> sellerIds,
	            @RequestParam(required = false) Date startDate,
	            @RequestParam(required = false) Date endDate,
	            @RequestParam(required = false, defaultValue = "0") Integer start,
	            @RequestParam(required = false, defaultValue = "10") Integer rows) {
	        return new APIResponse(
	                sellerRelevanceEventService
	                        .getSellerIntent(activeUser, eventTypes, sellerIds, startDate, endDate, start, rows));
	    }

	    @InternalIp
	    @ResponseBody
	    @RequestMapping(value = "data/v1/seller-relevance-events/selector", method = RequestMethod.GET)
	    public APIResponse getSellerRelevanceEventsBySelector(@ModelAttribute FIQLSelector selector) {
	        return new APIResponse(sellerRelevanceEventService.getSellerRelevanceEventBySelector(selector));
	    }

	    @InternalIp
	    @ResponseBody
	    @RequestMapping(value = "cron/v1/seller-relevance-events", method = RequestMethod.GET)
	    public APIResponse populateSellerRelevanceFactorsEventsInDB(
	            @RequestParam(required = true) List<EventCategory> eventTypes,
	            @RequestParam(required = true, defaultValue = "0") int noOfDays) {
	        return sellerRelevanceEventService.populateSellerRelevanceFactorsEventsInDB(eventTypes, noOfDays);
	    }
}
