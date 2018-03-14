package com.proptiger.app.repo.srf;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.model.cms.SellerRelevancePackageComponents;

public interface SellerRelevancePackageComponentsDao extends JpaRepository<SellerRelevancePackageComponents ,Integer>{

	/**
     * 
     * @param sellerRelevanceIds
     * @return
     */
    public List<SellerRelevancePackageComponents> findBySellerRelevanceIdIn(List<Integer> sellerRelevanceIds);

    public List<SellerRelevancePackageComponents> findBySellerRelevanceIdAndAttributeId(
            Integer sellerRelevanceId,
            Integer attributeId);

    public List<SellerRelevancePackageComponents> findBySellerRelevanceIdAndAttributeIdAndAttributeValuesIn(
            Integer sellerRelevanceId,
            Integer attributeId,
            Collection<Integer> attributeValues);

    @Query("Select SRF.id FROM SellerRelevancePackageComponents SRF WHERE SRF.sellerPackageId IN (?1)")
    public List<Integer> getSellerFactorAttributeIds(List<Integer> sellerPackageIds);

    @Modifying
    @Transactional
    @Query("Delete from SellerRelevancePackageComponents MSFM WHERE MSFM.id in (?1)")
    public void deleteMasterSellerRelevanceFactorsAttributeMappingByIds(List<Integer> attributeMappingIds);

    @Query("Select SRPC FROM SellerRelevancePackageComponents SRPC JOIN FETCH SRPC.sellerRelevanceFactors SRF"
            + " WHERE SRPC.attributeId IN (?1) and SRF.saleTypeId = ?2")
    public List<SellerRelevancePackageComponents> getPackageComponentsByEntitySaleType(
            List<Integer> entityTypeIds,
            Integer saleTypeId);

    @Modifying
    @Transactional
    @Query("Delete from SellerRelevancePackageComponents SRPC WHERE SRPC.sellerPackageId = ?1")
    public void deleteSellerRelevancePackageComponentsByPackageId(Integer packageId);
}
