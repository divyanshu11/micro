package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.model.cms.SellerVisibilityRankings;

public interface SellerVisibilityRankingsDao extends JpaRepository<SellerVisibilityRankings, Integer>{
	 /**
     * 
     * @param sellerRelevanceFactorIds
     */
    @Modifying
    @Transactional
    @Query("Delete from SellerVisibilityRankings WHERE sellerRelevanceFactorId in (?1)")
    public void deleteSellerVisibilityRankingsForSellerRelevanceFactorIds(List<Integer> sellerRelevanceFactorIds);
    
    /**
     * 
     * @param sellerRelevantFactorIds
     * @return
     */
    public List<SellerVisibilityRankings> findBySellerRelevanceFactorIdIn(List<Integer> sellerRelevantFactorIds);

}
