package com.proptiger.app.repo.srf;

import java.util.List;

import com.proptiger.core.model.cms.SellerRelevanceFactorsOld;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;

public interface SellerRelevanceFactorsOldCustomDao {
	  /**
     * Fetch paginated list of SellerRelevanceFactors using FIQL selector
     * 
     * @param selector
     * @return
     */

    public PaginatedResponse<List<SellerRelevanceFactorsOld>> getSellerRelevanceFactorsOldFromDB(FIQLSelector selector);

}
