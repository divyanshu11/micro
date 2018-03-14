package com.proptiger.app.repo.srf;

import java.util.List;

import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;

/**
 * Interface to support custom queries to SellerRelevanceFactors model
 *
 */
public interface SellerRelevanceFactorsCustomDao {
	 /**
     * Fetch paginated list of SellerRelevanceFactors using FIQL selector
     * @param selector
     * @return
     */

    public PaginatedResponse<List<SellerRelevanceFactors>> getSellerRelevanceFactorsFromDB(FIQLSelector selector);

}
