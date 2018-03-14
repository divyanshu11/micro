package com.proptiger.app.repo.srf;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.proptiger.core.model.cms.SellerRelevanceFactorsOld;

public interface SellerRelevanceFactorsOldDao extends JpaRepository<SellerRelevanceFactorsOld, Integer>,SellerRelevanceFactorsOldCustomDao{

	 /**
     * Find seller relevance factors for given seller ids
     * 
     * @param sellerIds
     * @return
     */
    @Query("SELECT srf FROM SellerRelevanceFactorsOld srf WHERE srf.sellerId in (?1)")
    public List<SellerRelevanceFactorsOld> getSellerIdByIds(Collection<Integer> sellerIds);

    /**
     * 
     * @param transactionCategory
     * @return
     */
    @Query("SELECT srf FROM SellerRelevanceFactorsOld srf JOIN srf.sellerTransactionCategory stc WHERE stc.label in ?1")
    public List<SellerRelevanceFactorsOld> findByTransactionCategory(List<String> transactionCategory);

    @Query("SELECT srf FROM SellerRelevanceFactorsOld srf where srf.sellerCategoryId = 6 and srf.isBuyExpert = 1")
    public List<SellerRelevanceFactorsOld> getExpertSellersForBuy();

    @Query("SELECT srf FROM SellerRelevanceFactorsOld srf where srf.sellerCategoryId = 6 and srf.isRentExpert = 1")
    public List<SellerRelevanceFactorsOld> getExpertSellersForRent();
}
