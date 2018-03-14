package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.proptiger.core.model.cms.SellerScoreBreakup;

public interface SellerScoreBreakupBaseDao extends JpaRepository<SellerScoreBreakup, Integer>{
	/** 
	     * @param sellerId
	     * @param cityId
	     * @param listingCountScore
	     * @param listingQualityScore
	     * @param callPickupScore
	     * @param mplusScore
	     * @param score
	     * @param buyerFeedbackScore
	     */
	    @Modifying
	    @Query(
	            nativeQuery = true,
	            value = "insert into cms.seller_score_breakup(seller_id, city_id, listing_count_score, listing_quality_score, call_pickup_score, mplus_score, score,buyer_feedback_score,reported_deals_score) values(?1, ?2, ?3, ?4, ?5, ?6, ?7,?8,?9) on duplicate key update listing_count_score = values(listing_count_score), listing_quality_score = values(listing_quality_score), call_pickup_score = values(call_pickup_score), mplus_score = values(call_pickup_score), score=values(call_pickup_score), buyer_feedback_score=values(buyer_feedback_score), reported_deals_score=values(reported_deals_score)")
	    public void insertOrUpdateSellerScoreBreakup(
	            Integer sellerId,
	            Integer cityId,
	            Double listingCountScore,
	            Double listingQualityScore,
	            Double callPickupScore,
	            Double mplusScore,
	            Double score,
	            Double buyerFeedbackScore,
	            Double reportedDealsScore);

	    /**
	     * 
	     * @param sellerId
	     * @return
	     */
	    public List<SellerScoreBreakup> findBySellerId(Integer sellerId);

	    /**
	     * 
	     * @param sellerId
	     * @param cityId
	     * @return
	     */
	    public SellerScoreBreakup findBySellerIdAndCityId(Integer sellerId, Integer cityId);

}
