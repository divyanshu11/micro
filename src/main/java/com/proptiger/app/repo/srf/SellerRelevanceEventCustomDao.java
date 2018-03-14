package com.proptiger.app.repo.srf;

import java.util.List;

import com.proptiger.app.model.srf.SellerRelevanceEvent;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;


public interface SellerRelevanceEventCustomDao {

	public PaginatedResponse<List<SellerRelevanceEvent>> getSellerRelevanceEventsBySelector(FIQLSelector selector);
}
