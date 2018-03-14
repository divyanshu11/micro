package com.proptiger.app.services.srf;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.util.Constants;

@Service
public class SellerRelevanceCacheService {
	@Cacheable(
            key = "#sellerId+'-SellerRelevance'+#saleTypeId",
            value = Constants.CacheName.SELLER_RELEVANCE_FACTORS,
            unless = "#result == null")
    public SellerRelevanceFactors getSellerRelevanceFromCache(
            Integer sellerId,
            Integer saleTypeId) {
        return null;
    }
    
    
    @Cacheable(
            key = "#sellerId+'-SellerRelevance'+#saleTypeId",
            value = Constants.CacheName.SELLER_RELEVANCE_FACTORS,
            unless = "#result == null")
    public SellerRelevanceFactors setSellerRelevanceToCache(
            Integer sellerId,
            Integer saleTypeId,
            SellerRelevanceFactors sellerRelevanceFactors) {
        return sellerRelevanceFactors;
    }
    
    
    @CacheEvict(key = "#sellerId+'-SellerRelevance'+#saleTypeId", value = Constants.CacheName.SELLER_RELEVANCE_FACTORS)
    public SellerRelevanceFactors deleteSellerRelevanceFromCache(Integer sellerId, Integer saleTypeId) {
        return null;
    }
    

}
