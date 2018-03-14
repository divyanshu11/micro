package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.proptiger.core.model.cms.TransactionCategoryGroupsMapping;

public interface SellerTransactionCategoryGroupMappingDao 
									extends JpaRepository<TransactionCategoryGroupsMapping, Integer>{

	public List<TransactionCategoryGroupsMapping> findBycategoryGroupId(Integer categoryGroupId);
}
