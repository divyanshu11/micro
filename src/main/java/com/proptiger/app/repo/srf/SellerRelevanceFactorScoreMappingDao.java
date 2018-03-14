package com.proptiger.app.repo.srf;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.model.cms.SellerRelevanceFactorScoreMapping;

public interface SellerRelevanceFactorScoreMappingDao extends JpaRepository<SellerRelevanceFactorScoreMapping, Integer> {

	 @Modifying
	    @Transactional
	    @Query("delete from SellerRelevanceFactorScoreMapping SRFM WHERE SRFM.sellerRelevanceFactorId = ?1")
	    public void deleteSellerRelevanceScoreMappingForRelevanceId(Integer sellerRelevanceId);
	    
	    /**
	     * 
	     * @param sellerRelevantFactorIds
	     * @return
	     */
	    @Query(value = "select srfsm from SellerRelevanceFactorScoreMapping srfsm where sellerRelevanceFactorId in ?1")
	    public List<SellerRelevanceFactorScoreMapping> fetchBySellerRelevanceFactorIdsIn(Collection<Integer> sellerRelevantFactorIds);

}
