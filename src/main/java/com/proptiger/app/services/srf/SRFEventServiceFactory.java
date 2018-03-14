package com.proptiger.app.services.srf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.proptiger.app.model.srf.SellerRelevanceEvent.EventCategory;
import com.proptiger.core.exception.ProAPIException;



@Service
public class SRFEventServiceFactory {

	 @Autowired
	    private ApplicationContext applicationContext;

	    public ISRFEventService getSRFEventsServiceInstance(EventCategory eventCategory) {
	        ISRFEventService instance = null;
	        switch (eventCategory) {
	            case ACCOUNT_LOCKED:
	                instance = (ISRFEventService) applicationContext.getBean(AccountLockedEventsService.class);
	                break;
	            case SUSPENDED:
	            case PARTIAL_PENALIZED:
	            default:
	                throw new ProAPIException("Event Category not supported");
	        }
	        return instance;
	    }
}
