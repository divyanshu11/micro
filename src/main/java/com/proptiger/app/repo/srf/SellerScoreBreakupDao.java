package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.model.cms.SellerScoreBreakup;
import com.proptiger.core.util.Constants;

@Repository
public class SellerScoreBreakupDao {

    @Autowired
    private SellerScoreBreakupBaseDao sellerScoreBreakupBaseDao;

    /**
     * 
     * @param sellerId
     * @param cityId
     * @param listingCountScore
     * @param listingQualityScore
     * @param callPickupScore
     * @param mplusScore
     * @param score
     * @param buyerFeedbackScore
     */
    @Transactional
    public void saveSellerScoreBreakup(
            Integer sellerId,
            Integer cityId,
            Double listingCountScore,
            Double listingQualityScore,
            Double callPickupScore,
            Double mplusScore,
            Double score,
            Double buyerFeedbackScore,
            Double reportedDealsScore) {
        sellerScoreBreakupBaseDao.insertOrUpdateSellerScoreBreakup(
                sellerId,
                cityId,
                listingCountScore,
                listingQualityScore,
                callPickupScore,
                mplusScore,
                score,
                buyerFeedbackScore,
                reportedDealsScore);
    }

    /**
     * 
     * @param sellerId
     * @return
     */
    @Cacheable(value = Constants.CacheName.SELLER_SCORE_BREAKUP_LIST, key = "#sellerId")
    public List<SellerScoreBreakup> getSellerScoreBreakup(Integer sellerId) {
        return sellerScoreBreakupBaseDao.findBySellerId(sellerId);
    }

    /**
     * 
     * @param sellerId
     * @param cityId
     * @return
     */
    @Cacheable(value = Constants.CacheName.SELLER_SCORE_BREAKUP, key = "#sellerId+#cityId")
    public SellerScoreBreakup getSellerCityScoreBreakup(Integer sellerId, Integer cityId) {
        return sellerScoreBreakupBaseDao.findBySellerIdAndCityId(sellerId, cityId);
    }

}
