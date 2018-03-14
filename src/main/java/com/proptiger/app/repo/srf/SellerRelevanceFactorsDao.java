package com.proptiger.app.repo.srf;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.proptiger.core.model.cms.SellerRelevanceFactors;

public interface SellerRelevanceFactorsDao extends JpaRepository<SellerRelevanceFactors, Integer>,SellerRelevanceFactorsCustomDao{
	public List<SellerRelevanceFactors> findBySellerId(Integer sellerId);

    @Modifying
    @Query("Update SellerRelevanceFactors SRF set SRF.sellerTransGroupId = ?2 WHERE SRF.id = ?1")
    public void updateSellerRelevanceFactorsGroupId(Integer id, Integer groupId);

    @Query("SELECT srf FROM SellerRelevanceFactors srf JOIN srf.transactionCategoryGroup tcg JOIN "
            + "tcg.transactionCategoryGroupsMappings tcgm where tcgm.transactionCategoryId in (?1) and srf.saleTypeId = ?2")
    public List<SellerRelevanceFactors> getSellersByTransactionCategoryAndSaleType(
            Collection<Integer> transactionCategoryId,
            Integer saleTypeId);

    /**
     * 
     * @param transactionCategory
     * @return
     */
    @Query("SELECT srf FROM SellerRelevanceFactors srf JOIN srf.transactionCategoryGroup tcg JOIN "
            + "tcg.transactionCategoryGroupsMappings tcgm JOIN tcgm.sellerTransactionCategory stc where stc.label in (?1)")
    public List<SellerRelevanceFactors> findByTransactionCategory(List<String> transactionCategory);

    /**
     * Find seller relevance factors for given seller ids
     * 
     * @param sellerIds
     * @return
     */
    @Query("SELECT srf FROM SellerRelevanceFactors srf WHERE srf.sellerId in (?1)")
    public List<SellerRelevanceFactors> getSellerIdByIds(Collection<Integer> sellerIds);

    @Query("SELECT distinct srf FROM SellerRelevanceFactors srf LEFT JOIN FETCH srf.sellerRelevanceFactorScores scr LEFT JOIN FETCH srf.sellerRelevancePackageComponents srpc LEFT JOIN FETCH srf.sellerVisibilityRankings WHERE srf.sellerId in (?1)")
    public List<SellerRelevanceFactors> findBySellerIdsWithScoresAndVisibility(Collection<Integer> sellerIds);

    @Query("SELECT distinct srf FROM SellerRelevanceFactors srf LEFT JOIN FETCH srf.sellerRelevanceFactorPackage srp WHERE srf.sellerId in (?1)")
    public List<SellerRelevanceFactors> findBySellerIdsWithPackages(Collection<Integer> sellerIds);

    @Query("SELECT distinct srf FROM SellerRelevanceFactors srf LEFT JOIN FETCH srf.sellerRelevanceFactorPackage srp LEFT JOIN FETCH srp.sellerRelevancePackageComponents WHERE srf.sellerId in (?1)")
    public List<SellerRelevanceFactors> findBySellerIdsWithPackagesAncComponents(Collection<Integer> sellerIds);
    
    @Query("SELECT distinct srf FROM SellerRelevanceFactors srf LEFT JOIN FETCH srf.sellerRelevanceFactorScores scr LEFT JOIN FETCH srf.sellerRelevancePackageComponents srpc LEFT JOIN FETCH srf.sellerVisibilityRankings WHERE srf.sellerId in (?1) AND srf.saleTypeId=?2")
    public List<SellerRelevanceFactors> findBySellerIdsAndSaleTypeIdWithScoresAndVisibility(Collection<Integer> sellerIds, Integer saleType);
    
    @Query("SELECT distinct srf FROM SellerRelevanceFactors srf LEFT JOIN FETCH srf.sellerRelevanceFactorPackage srp LEFT JOIN FETCH srp.sellerRelevancePackageComponents WHERE srf.sellerId in (?1) AND srf.saleTypeId=?2")
    public List<SellerRelevanceFactors> findBySellerIdsAndSaleTypeIdWithPackagesAncComponents(Collection<Integer> sellerIds, Integer saleType);
    
    @Query("SELECT srf FROM SellerRelevanceFactors srf WHERE srf.sellerTransGroupId in (?1)  and srf.saleTypeId = ?2")
    public List<SellerRelevanceFactors> findByTransactionCategoryIds(List<Integer> sellerTransGroupIds, Integer saleTypeId);

}
