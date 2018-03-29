package com.proptiger.app.services.srf;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proptiger.app.repo.srf.SellerRelevanceFactorScoreMappingDao;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.proptiger.core.dto.cms.score.CompanyRelevantScoresDTO;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.helper.MicroServiceHelper;
import com.proptiger.core.model.cms.MasterSellerScoreTypes.SellerScoreTypes;
import com.proptiger.core.model.cms.MasterTransactionCategoryGroup.TransactionCategoryGroups;
import com.proptiger.core.model.cms.SellerRelevanceFactorScoreMapping;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.SellerRelevancePackageComponents;
import com.proptiger.core.model.cms.SellerTransactionCategories.CategoryType;
import com.proptiger.core.model.cms.SellerVisibilityRankings;
//import com.proptiger.data.dto.cms.CompanyRelevantScoresDTO;
import com.proptiger.app.repo.srf.SellerRelevanceFactorScoreMappingDao;
import com.proptiger.app.repo.srf.SellerRelevanceFactorsDao;
//import com.proptiger.data.service.cms.listing.ListingIndexingCacheService;


/**
 * 
 * @author user
 *
 */
@Service
public class SellerRelevanceFactorScoreMappingService {

	@Autowired
    SellerRelevanceFactorScoreMappingDao sellerRelevanceFactorScoreMappingDao;
//Divyanshu
	@Autowired
	MIDLServiceHelper					midlServiceHelper;
//    @Autowired
//    ListingIndexingCacheService          listingIndexingCacheService;

    @Autowired
    SellerRelevanceFactorsDao            sellerRelevanceFactorsDao;

    /**
     * 
     * @param sellerRelevanceFactorsIds
     * @return
     */
    public List<SellerRelevanceFactorScoreMapping> findBySellerRelevanceFactorsId(
            Collection<Integer> sellerRelevanceFactorsIds) {
        return sellerRelevanceFactorScoreMappingDao.fetchBySellerRelevanceFactorIdsIn(sellerRelevanceFactorsIds);
    }

    public Map<Integer, CompanyRelevantScoresDTO> getSellerIdToSellerRelevanceScoreDTOMapV2(
            Collection<Integer> sellerIdsWOSellerRelevanceScore,
            Integer domainId) {
        Map<Integer, Map<Integer, SellerRelevanceFactors>> sellerIdToSaleTypeIdToSellerRFsMap =
                getSellerIdToSaleTypeIdToSellerRFs(sellerIdsWOSellerRelevanceScore);
        return setSellerRelevantScoresInCacheV2(
                sellerIdToSaleTypeIdToSellerRFsMap,
                sellerIdsWOSellerRelevanceScore,
                domainId);
    }

    private Map<Integer, Map<Integer, SellerRelevanceFactors>> getSellerIdToSaleTypeIdToSellerRFs(
            Collection<Integer> sellerIds) {
        List<SellerRelevanceFactors> sellerRelevanceFactors =
                sellerRelevanceFactorsDao.findBySellerIdsWithScoresAndVisibility(sellerIds);
        Map<Integer, Map<Integer, SellerRelevanceFactors>> sellerIdToSaleTypeIdToSRFMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(sellerRelevanceFactors))
            sellerIdToSaleTypeIdToSRFMap = sellerRelevanceFactors.stream()
                    .collect(Collectors.groupingBy(SellerRelevanceFactors::getSellerId, Collectors.toMap(
                            SellerRelevanceFactors::getSaleTypeId,
                            Function.identity())));
        return sellerIdToSaleTypeIdToSRFMap;
    }

    private Map<SellerScoreTypes, Number> getSellerRelevanceDTOV2(
            Set<SellerRelevanceFactorScoreMapping> sellerRelevanceFactorScoreMappingList) {
        Map<SellerScoreTypes, Number> scoreMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(sellerRelevanceFactorScoreMappingList))
            scoreMap = sellerRelevanceFactorScoreMappingList.stream().collect(Collectors.toMap(
                    srfsm -> SellerScoreTypes.lookupById(srfsm.getScoreId()),
                    SellerRelevanceFactorScoreMapping::getScoreValue));
        for (SellerScoreTypes sellerScoreType : SellerScoreTypes.values()) {
            scoreMap.putIfAbsent(sellerScoreType, 0);
        }
        return scoreMap;
    }

    private Map<Integer, CompanyRelevantScoresDTO> setSellerRelevantScoresInCacheV2(
            Map<Integer, Map<Integer, SellerRelevanceFactors>> sellerIdToSaleTypeIdToSellerRFsMap,
            Collection<Integer> sellerIdsWOSellerRelevanceScore,
            Integer domainId) {
        Map<Integer, CompanyRelevantScoresDTO> sellerIdToRelevantScoresMap = new HashMap<>();
        for (Integer sellerId : sellerIdsWOSellerRelevanceScore) {
            Map<Integer, SellerRelevanceFactors> saleTypeToSellerRFs =
                    Optional.ofNullable(sellerIdToSaleTypeIdToSellerRFsMap.get(sellerId)).orElseGet(HashMap::new);
            CompanyRelevantScoresDTO companyRelevantScoresDTO = new CompanyRelevantScoresDTO();

            Map<CategoryType, Float> sellerTransactionRevealScoreMap = new HashMap<>();
            Map<CategoryType, Integer> topSellerPriorityScoreMap = new HashMap<>();
            Map<CategoryType, Map<TransactionCategoryGroups, Map<String, Set<Integer>>>> relevantFactors =
                    new HashMap<>();
            Map<CategoryType, Set<SellerVisibilityRankings>> sellerVisibilityRankingsMap = new HashMap<>();
            
            for (CategoryType ct : CategoryType.values()) {
                SellerRelevanceFactors sellerRelevanceFactor = saleTypeToSellerRFs.get(ct.getPaymentTypeId());
                // getSellerRelevanceFactors(saleTypeToSellerRFs, ct, sellerId);
                if (sellerRelevanceFactor == null) {
                    sellerRelevanceFactor = new SellerRelevanceFactors();
                    sellerRelevanceFactor.setSellerId(sellerId);
                }
                Map<SellerScoreTypes, Number> scoreMap =
                        getSellerRelevanceDTOV2(sellerRelevanceFactor.getSellerRelevanceFactorScores());
                sellerTransactionRevealScoreMap
                        .put(ct, scoreMap.get(SellerScoreTypes.SELLER_TRANSACTION_REVEAL_SCORE).floatValue());
                topSellerPriorityScoreMap.put(ct, scoreMap.get(SellerScoreTypes.TOP_SELLER_PRIORITY_SCORE).intValue());

                relevantFactors.put(ct, Optional.ofNullable(relevantFactors.get(ct)).orElseGet(HashMap::new));

                TransactionCategoryGroups group =
                        TransactionCategoryGroups.getById(sellerRelevanceFactor.getSellerTransGroupId());
                if (group != null) {
                    Set<SellerRelevancePackageComponents> sellerRelevancePackageComponents =
                            Optional.ofNullable(sellerRelevanceFactor.getSellerRelevancePackageComponents())
                                    .orElseGet(HashSet::new);
                    relevantFactors.get(ct).put(group, new HashMap<>());

                    for (SellerRelevancePackageComponents packageComponent : sellerRelevancePackageComponents) {
                        String subPackName = packageComponent.getMasterSellerRelevancePackageType().getAttributeName();
                        if (relevantFactors.get(ct).get(group).get(subPackName) == null)
                            relevantFactors.get(ct).get(group).put(subPackName, new HashSet<>());
                        relevantFactors.get(ct).get(group).get(subPackName).add(packageComponent.getAttributeValues());
                    }
                }
                
                Set<SellerVisibilityRankings> sellerVisibilityRankings = Optional
                        .ofNullable(sellerRelevanceFactor.getSellerVisibilityRankings()).orElseGet(HashSet::new);
                if(CollectionUtils.isNotEmpty(sellerVisibilityRankings)){
                    sellerVisibilityRankingsMap.put(ct, sellerVisibilityRankings);
                }
            }
            companyRelevantScoresDTO.setTopSellerPriorityScore(topSellerPriorityScoreMap);
            companyRelevantScoresDTO.setTransactionRevealScore(sellerTransactionRevealScoreMap);
            companyRelevantScoresDTO.setRelevantFactors(relevantFactors);
            companyRelevantScoresDTO.setVisibilityRankings(sellerVisibilityRankingsMap);
            
            sellerIdToRelevantScoresMap.put(sellerId, companyRelevantScoresDTO);
 //Divyanshu           listingIndexingCacheService.setCompanyRelevantScoreToCache(sellerId, domainId, companyRelevantScoresDTO);
            			midlServiceHelper.setCompanyRelevantScoreToCache(sellerId, domainId, companyRelevantScoresDTO);
        }
        return sellerIdToRelevantScoresMap;
    }
}
