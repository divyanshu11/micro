package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.proptiger.core.model.cms.SellerRelevanceFactorsOld;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.repo.util.EntityCustomDaoImpl;

@Repository
public class SellerRelevanceFactorsOldDaoImpl extends EntityCustomDaoImpl<SellerRelevanceFactorsOld>{

	 /**
     * Get list of seller relevance factors using fiql
     * @param fiqlSelector
     * @return
     */
    public PaginatedResponse<List<SellerRelevanceFactorsOld>> getSellerRelevanceFactorsOldFromDB(
            FIQLSelector fiqlSelector) {
        return getPaginatedEntities(fiqlSelector, SellerRelevanceFactorsOld.class);
    }
}
