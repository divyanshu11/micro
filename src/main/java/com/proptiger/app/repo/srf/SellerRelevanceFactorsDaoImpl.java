package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.repo.util.EntityCustomDaoImpl;

@Repository
public class SellerRelevanceFactorsDaoImpl extends EntityCustomDaoImpl<SellerRelevanceFactors> {
	 /**
     * Get list of seller relevance factors using fiql
     * @param fiqlSelector
     * @return
     */
    public PaginatedResponse<List<SellerRelevanceFactors>> getSellerRelevanceFactorsFromDB(FIQLSelector fiqlSelector) {
        return getPaginatedEntities(fiqlSelector, SellerRelevanceFactors.class);
    }

}
