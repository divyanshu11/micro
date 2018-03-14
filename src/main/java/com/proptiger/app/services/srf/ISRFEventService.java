package com.proptiger.app.services.srf;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.proptiger.app.model.srf.SellerRelevanceEvent;



public interface ISRFEventService {
	 public List<SellerRelevanceEvent> getAllEventsBetween(List<Integer> sellerIds, Date startDate, Date endDate);

	    public String getSellerIntent(SellerRelevanceEvent srfEvent);

	    public Set<Integer> filterSellers(Set<Integer> sellerIds);

}
