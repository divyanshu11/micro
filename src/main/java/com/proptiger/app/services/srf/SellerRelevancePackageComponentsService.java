package com.proptiger.app.services.srf;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proptiger.app.repo.srf.SellerRelevancePackageComponentsDao;
import com.proptiger.core.model.cms.SellerRelevancePackageComponents;


@Service
public class SellerRelevancePackageComponentsService {
	 @Autowired
	    SellerRelevancePackageComponentsDao sellerRelevancePackageComponentsDao;

	    /**
	     * 
	     * @param sellerRelevanceIds
	     * @return
	     */
	    public List<SellerRelevancePackageComponents> getPackageComponenetsForSellerRelevanceIds(
	            List<Integer> sellerRelevanceIds) {
	        return sellerRelevancePackageComponentsDao.findBySellerRelevanceIdIn(sellerRelevanceIds);
	    }

	    public List<SellerRelevancePackageComponents> findBySellerRelevanceIdAndAttributeId(
	            Integer sellerRelevanceId,
	            Integer attributeId) {
	        return sellerRelevancePackageComponentsDao
	                .findBySellerRelevanceIdAndAttributeId(sellerRelevanceId, attributeId);
	    }

	    public List<SellerRelevancePackageComponents> findBySellerRelevanceIdAndAttributeIdAndAttributeValuesIn(
	            Integer sellerRelevanceId,
	            Integer attributeId,
	            Collection<Integer> attributeValues) {
	        return sellerRelevancePackageComponentsDao.findBySellerRelevanceIdAndAttributeIdAndAttributeValuesIn(
	                sellerRelevanceId,
	                attributeId,
	                attributeValues);
	    }
	}


