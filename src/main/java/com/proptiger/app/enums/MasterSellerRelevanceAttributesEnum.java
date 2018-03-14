package com.proptiger.app.enums;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.proptiger.app.repo.cms.MasterSellerRelevanceAttributeDao;
import com.proptiger.core.model.cms.MasterSellerRelevancePackageType;
//import com.proptiger.data.repo.MasterSellerRelevanceAttributeDao;

//Moved from petra...@Divyanshu

@Service
public class MasterSellerRelevanceAttributesEnum {

	@Autowired
    private MasterSellerRelevanceAttributeDao masterSellerRelevanceAttributeDao;

    @Cacheable(value = "master_seller_relevance_attribute_map")
    public Map<String, Integer> getMasterSellerRelevanceAttributeMap() {
        
        List<MasterSellerRelevancePackageType> attributeMappingList = masterSellerRelevanceAttributeDao.findAll();
        
        return attributeMappingList.stream().collect(
                Collectors.toMap(
                        MasterSellerRelevancePackageType::getAttributeName,
                        MasterSellerRelevancePackageType::getId));
    }
}
