package com.proptiger.app.repo.srf;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proptiger.core.model.cms.SellerRelevanceFactors;


@Service
public class SellerRelevanceFactorsDaoProxy {

	@Autowired
    private SellerRelevanceFactorsDao sellerRelevanceFactorsDao;

    public void saveOrUpdate(SellerRelevanceFactors sellerRelevanceFactors) {

    }

    public void saveOrUpdate(List<SellerRelevanceFactors> sellerRelevanceFactors) {

    }
}
