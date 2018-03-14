package com.proptiger.app.services.srf;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.app.repo.srf.SellerVisibilityRankingsDao;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.SellerVisibilityRankings;
//import com.proptiger.data.repo.srf.SellerVisibilityRankingsDao;

@Service
public class SellerVisibilityRankingsService {

	@Autowired
    SellerVisibilityRankingsDao sellerVisibilityRankingsDao;
    /**
     * 
     * @param sellerRelevanceFactorIds
     */
    public void deleteSellerVisibilityRankingsForSellerRelevanceFactorIds(List<Integer> sellerRelevanceFactorIds) {
        sellerVisibilityRankingsDao.deleteSellerVisibilityRankingsForSellerRelevanceFactorIds(sellerRelevanceFactorIds);
    }
    /**
     * 
     * @param sellerRelevantIds
     * @return
     */
    public List<SellerVisibilityRankings> getSellerVisibilityRankingsBySellerRelevantIds(
            List<Integer> sellerRelevantIds) {
        return sellerVisibilityRankingsDao.findBySellerRelevanceFactorIdIn(sellerRelevantIds);
    }
    /**
     * 
     * @param sellerRelevanceFactors
     * @param sellerVisibilityRankingsNew
     */
    @Transactional
    public void updateSellerVisibilityRankings(
            SellerRelevanceFactors sellerRelevanceFactors,
            List<SellerVisibilityRankings> sellerVisibilityRankingsNew) {
        deleteSellerVisibilityRankingsForSellerRelevanceFactorIds(
                Collections.singletonList(sellerRelevanceFactors.getId()));
        if(CollectionUtils.isNotEmpty(sellerVisibilityRankingsNew)){
            setSRFIdInVisibilityRankings(sellerRelevanceFactors, sellerVisibilityRankingsNew);
            sellerVisibilityRankingsDao.save(sellerVisibilityRankingsNew);
        }
    }
    private void setSRFIdInVisibilityRankings(
            SellerRelevanceFactors sellerRelevanceFactors,
            List<SellerVisibilityRankings> sellerVisibilityRankingsNew) {
        for(SellerVisibilityRankings svr : sellerVisibilityRankingsNew){
            svr.setSellerRelevanceFactorId(sellerRelevanceFactors.getId());
        }
    }
}
