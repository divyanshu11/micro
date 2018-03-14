package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.model.cms.SellerRelevancePackage;

public interface SellerRelevancePackageDao extends JpaRepository<SellerRelevancePackage, Integer>{

	@Modifying
    @Transactional
    @Query("Delete from SellerRelevancePackage SRP WHERE SRP.id in (?1)")
    public void deleteSellerPackage(List<Integer> sellerPackageIdList);

    @Query("Select SRP.id FROM SellerRelevancePackage SRP WHERE SRP.sellerRelevanceId = ?1")
    public List<Integer> getPackageIdBySellerRelevanceId(Integer sellerRelevanceId);

    public SellerRelevancePackage findByIdAndSellerRelevanceId(Integer id, Integer sellerRelevanceId);
    
    public List<SellerRelevancePackage> findBySellerRelevanceId(Integer sellerRelevanceId);

}
