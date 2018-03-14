package com.proptiger.app.services.srf;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.app.enums.MasterSellerRelevanceAttributesEnum;
//import com.proptiger.app.model.transaction.ProductPaymentStatus;


import com.proptiger.app.repo.srf.SellerRelevanceFactorScoreMappingDao;
import com.proptiger.app.repo.srf.SellerRelevanceFactorsDao;
import com.proptiger.app.repo.srf.SellerRelevanceFactorsOldDao;
import com.proptiger.app.repo.srf.SellerTransactionCategoryGroupMappingDao;
import com.proptiger.app.service.order.ProductPaymentStatusService;
import com.proptiger.core.enums.Domain;
import com.proptiger.core.exception.BadRequestException;
import com.proptiger.core.helper.CyclopsServiceHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.cms.Listing;
import com.proptiger.core.model.cms.SellerRelevanceFactorScoreMapping;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.SellerRelevanceFactorsOld;
import com.proptiger.core.model.cms.SellerRelevancePackage;
import com.proptiger.core.model.cms.TransactionCategoryGroupsMapping;
import com.proptiger.core.model.cms.MasterTransactionCategoryGroup.TransactionCategoryGroups;
import com.proptiger.core.model.cms.RawSellerDTO;
import com.proptiger.core.model.cms.SellerTransactionCategories.SellerTransactionCategory;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentTypeEnum;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.Pair;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.util.Constants;
import com.proptiger.core.util.ExclusionAwareBeanUtilsBean;
//import com.proptiger.data.enums.MasterSellerRelevanceAttributesEnum;

//import com.proptiger.data.repo.srf.SellerRelevanceFactorScoreMappingDao;
//import com.proptiger.data.repo.srf.SellerRelevanceFactorsDao;
//import com.proptiger.data.repo.srf.SellerRelevanceFactorsOldDao;
//import com.proptiger.data.repo.srf.SellerTransactionCategoryGroupMappingDao;

/*
 * 
 * TODO Comment for the time...(SolrListingIndexingQueueService)
 * 
 */
//import com.proptiger.data.service.SolrListingIndexingQueueService;

//import com.proptiger.data.service.cms.listing.ListingService;
/*
 * No USE
 *import com.proptiger.data.service.marketplace.AppNotificationCacheService;
 *
*/
//import com.proptiger.data.service.order.LeadPaymentStatusService;
//import com.proptiger.data.service.order.ProductPaymentStatusService;
//import com.proptiger.data.service.srf.SellerRelevanceCacheService;
//import com.proptiger.data.service.srf.SellerRelevanceFactorsService;
//import com.proptiger.data.service.srf.SellerRelevanceHelper;
//import com.proptiger.data.service.srf.SellerRelevancePackageService;
//import com.proptiger.marketforce.data.service.RawSellerService;

	@Service
	public class SellerRelevanceFactorsService {

	    @Autowired
	    SellerRelevanceFactorsDao                    sellerRelevanceFactorsDao;

	    @Autowired
	    SellerRelevanceFactorsOldDao                 sellerRelevanceFactorsOldDao;

	    @Autowired
	    MasterSellerRelevanceAttributesEnum          masterSellerRelevanceAttributesEnum;

	    @Autowired
	    SellerTransactionCategoryGroupMappingDao     sellerTransactionCategoryGroupMappingDao;

	    @Autowired
	    SellerRelevancePackageService                sellerRelevancePackageService;

	    @Autowired
	    private SellerRelevanceFactorScoreMappingDao sellerRelevanceFactorScoreMappingDao;

	    @Autowired
	    private SellerRelevanceHelper                sellerRelevanceHelper;
//@Divyanshu
	    @Autowired MIDLServiceHelper 				 midlServiceHelper;
	    // @Autowired
//	    private RawSellerService                     rawSellerService;

	    // @Autowired
	//    private LeadPaymentStatusService             leadPaymentStatusService;

	    @Autowired
	    private ProductPaymentStatusService          productPaymentService;

	    @Autowired
	    private ApplicationContext                   applicationContext;

	    @Autowired
	    SellerRelevanceCacheService                  sellerRelevanceCacheService;

//	    @Autowired
//	    private AppNotificationCacheService          appNotificationCacheService;

	    // Indexing commented for the time....
//	    @Autowired
//	    private SolrListingIndexingQueueService      solrListingIndexingQueueService;

	//    @Autowired
	//    private ListingService                       listingService;

	    @Autowired
	    private CyclopsServiceHelper                 cyclopsServiceHelper;
	    
	    @Value("${seller.relevance-factors.payment.days}")
	    public int                                   NO_OF_DAYS_PAYMENTS;

	    private static Logger                                         logger                    =
	            LoggerFactory.getLogger(SellerRelevanceFactorsService.class);

	    private static final String                  UPDATED                   = "Updated";
	    private static final String                  NOT_UPDATED               = "Not updated";
	    private static final Integer                 NUMBER_OF_LEADTO_MULTIPLY = 3;

	    @PostConstruct
	    public void postConstructFields() {
	//        leadPaymentStatusService = applicationContext.getBean(LeadPaymentStatusService.class);
//	        rawSellerService = applicationContext.getBean(RawSellerService.class);
	    }

	    /**
	     * 
	     * @param selector
	     * @return
	     */
	    public PaginatedResponse<List<SellerRelevanceFactors>> getSellerRelevanceFactors(FIQLSelector selector) {
	        return sellerRelevanceFactorsDao.getSellerRelevanceFactorsFromDB(selector);
	    }

	    /**
	     * 
	     * @param selelrUserIds
	     * @param masterLeadPaymentTypeEnum
	     * @return
	     */
	    public List<SellerRelevanceFactors> getSellerRelevanceFactors(
	            Set<Integer> selelrUserIds,
	            MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum) {
	        List<SellerRelevanceFactors> sellerRelevanceFactors =
	                getSellerRelevanceFactorsWithCache(selelrUserIds, masterLeadPaymentTypeEnum);
	        if (CollectionUtils.isNotEmpty(sellerRelevanceFactors)) {
	            updateSellerRelevanceCacheBySellerIdAndSaleType(sellerRelevanceFactors);
	        }
	        return sellerRelevanceFactors;
	    }

	    private List<SellerRelevanceFactors> getSellerRelevanceFactorsWithCache(
	            Set<Integer> selelrUserIds,
	            MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum) {
	        List<SellerRelevanceFactors> sellerRelevanceFactors = new ArrayList<>();
	        Set<Integer> nonCachedSellers = new HashSet<>();
	        List<SellerRelevanceFactors> sellerRelevanceFactor = new ArrayList<>();
	        SellerRelevanceFactors sellerRelevanceFactorTemp = new SellerRelevanceFactors();
	        for (Integer selelrUserId : selelrUserIds) {
	            if (masterLeadPaymentTypeEnum != null) {
	                sellerRelevanceFactorTemp = sellerRelevanceCacheService
	                        .getSellerRelevanceFromCache(selelrUserId, masterLeadPaymentTypeEnum.getId());
	                if (sellerRelevanceFactorTemp != null) {
	                    sellerRelevanceFactor.add(sellerRelevanceFactorTemp);
	                }
	            }
	            else {
	                sellerRelevanceFactorTemp = sellerRelevanceCacheService
	                        .getSellerRelevanceFromCache(selelrUserId, MasterLeadPaymentTypeEnum.Buy.getId());
	                if (sellerRelevanceFactorTemp != null) {
	                    sellerRelevanceFactor.add(sellerRelevanceFactorTemp);
	                }
	                sellerRelevanceFactorTemp = sellerRelevanceCacheService
	                        .getSellerRelevanceFromCache(selelrUserId, MasterLeadPaymentTypeEnum.Rent.getId());
	                if (sellerRelevanceFactorTemp != null) {
	                    sellerRelevanceFactor.add(sellerRelevanceFactorTemp);
	                }
	                if (sellerRelevanceFactor.size() != 2) {
	                    sellerRelevanceFactor.removeAll(sellerRelevanceFactor);
	                }
	            }
	            if (CollectionUtils.isNotEmpty(sellerRelevanceFactor)) {
	                sellerRelevanceFactors.addAll(sellerRelevanceFactor);
	            }
	            else {
	                nonCachedSellers.add(selelrUserId);
	            }
	        }
	        if (CollectionUtils.isNotEmpty(nonCachedSellers)) {
	            if (masterLeadPaymentTypeEnum != null) {
	                sellerRelevanceFactors.addAll(
	                        sellerRelevanceFactorsDao.findBySellerIdsAndSaleTypeIdWithScoresAndVisibility(
	                                nonCachedSellers,
	                                masterLeadPaymentTypeEnum.getId()));
	            }
	            else {
	                sellerRelevanceFactors
	                        .addAll(sellerRelevanceFactorsDao.findBySellerIdsWithScoresAndVisibility(nonCachedSellers));
	            }
	        }
	        return sellerRelevanceFactors;
	    }

	    /**
	     * 
	     * @param selelrUserIds
	     * @param masterLeadPaymentTypeEnum
	     * @return
	     */
	    public List<SellerRelevanceFactors> getSellerRelevanceFactorsWithPackageAndComponents(
	            Set<Integer> selelrUserIds,
	            MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum) {
	        List<SellerRelevanceFactors> sellerRelevanceFactors =
	                getSellerRelevanceFactorsWithPackageAndComponentsWithCache(selelrUserIds, masterLeadPaymentTypeEnum);
	        if (CollectionUtils.isNotEmpty(sellerRelevanceFactors)) {
	            updateSellerRelevanceCacheBySellerIdAndSaleType(sellerRelevanceFactors);
	        }
	        return sellerRelevanceFactors;
	    }

	    private List<SellerRelevanceFactors> getSellerRelevanceFactorsWithPackageAndComponentsWithCache(
	            Set<Integer> selelrUserIds,
	            MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum) {
	        List<SellerRelevanceFactors> sellerRelevanceFactors = new ArrayList<>();
	        Set<Integer> nonCachedSellers = new HashSet<>();
	        List<SellerRelevanceFactors> sellerRelevanceFactor = new ArrayList<>();
	        SellerRelevanceFactors sellerRelevanceFactorTemp = new SellerRelevanceFactors();
	        for (Integer selelrUserId : selelrUserIds) {
	            if (masterLeadPaymentTypeEnum != null) {
	                sellerRelevanceFactorTemp = sellerRelevanceCacheService
	                        .getSellerRelevanceFromCache(selelrUserId, masterLeadPaymentTypeEnum.getId());
	                if (sellerRelevanceFactorTemp != null) {
	                    sellerRelevanceFactor.add(sellerRelevanceFactorTemp);
	                }
	            }
	            else {
	                sellerRelevanceFactorTemp = sellerRelevanceCacheService
	                        .getSellerRelevanceFromCache(selelrUserId, MasterLeadPaymentTypeEnum.Buy.getId());
	                if (sellerRelevanceFactorTemp != null) {
	                    sellerRelevanceFactor.add(sellerRelevanceFactorTemp);
	                }
	                sellerRelevanceFactorTemp = sellerRelevanceCacheService
	                        .getSellerRelevanceFromCache(selelrUserId, MasterLeadPaymentTypeEnum.Rent.getId());
	                if (sellerRelevanceFactorTemp != null) {
	                    sellerRelevanceFactor.add(sellerRelevanceFactorTemp);
	                }
	                if (sellerRelevanceFactor.size() != 2) {
	                    sellerRelevanceFactor.removeAll(sellerRelevanceFactor);
	                }
	            }
	            if (CollectionUtils.isNotEmpty(sellerRelevanceFactor)) {
	                sellerRelevanceFactors.addAll(sellerRelevanceFactor);
	            }
	            else {
	                nonCachedSellers.add(selelrUserId);
	            }
	        }
	        if (CollectionUtils.isNotEmpty(nonCachedSellers)) {
	            if (masterLeadPaymentTypeEnum != null) {
	                sellerRelevanceFactors.addAll(
	                        sellerRelevanceFactorsDao.findBySellerIdsAndSaleTypeIdWithPackagesAncComponents(
	                                nonCachedSellers,
	                                masterLeadPaymentTypeEnum.getId()));
	            }
	            else {
	                sellerRelevanceFactors
	                        .addAll(sellerRelevanceFactorsDao.findBySellerIdsWithPackagesAncComponents(nonCachedSellers));
	            }
	        }
	        return sellerRelevanceFactors;
	    }

	    /**
	     *
	     * @param sellerIds
	     * @return
	     */
	    public List<SellerRelevanceFactors> getSellerRelevanceFactorsPackages(List<Integer> sellerIds) {
	        return sellerRelevanceFactorsDao.findBySellerIdsWithPackages(sellerIds);
	    }

	    /**
	     * 
	     * @param selector
	     * @return
	     */
	    public PaginatedResponse<List<SellerRelevanceFactorsOld>> getSellerRelevanceFactorsOld(FIQLSelector selector) {
	        return sellerRelevanceFactorsOldDao.getSellerRelevanceFactorsOldFromDB(selector);
	    }

	    /**
	     * 
	     * @param sellerRelevanceFactors
	     * @return
	     */
	    public Map<String, List<SellerRelevanceFactors>> saveSellerRelevanceFactors(
	            List<SellerRelevanceFactors> sellerRelevanceFactors) {
	        Map<String, List<SellerRelevanceFactors>> finalResultMap = new HashMap<>();
	        List<SellerRelevanceFactors> validList = new ArrayList<>();
	        List<SellerRelevanceFactors> inValidList = validateData(sellerRelevanceFactors, true);
	        if (CollectionUtils.isNotEmpty(sellerRelevanceFactors)) {
	            for (SellerRelevanceFactors sellerRelevanceFactor : sellerRelevanceFactors) {
	                try {
	                    validList.add(
	                            applicationContext.getBean(SellerRelevanceFactorsService.class).saveDataWithScores(
	                                    sellerRelevanceFactor,
	                                    sellerRelevanceFactor.getSellerRelevanceFactorPackage()));
	                }
	                catch (Exception e) {
	                    logger.error("Error while saving seller relevance factors", e);
	                    inValidList.add(sellerRelevanceFactor);
	                }
	            }
	        }
	        if (CollectionUtils.isNotEmpty(validList)) {
	            sellerRelevanceHelper.sendNotificationForSRFStatusChange(
	                    validList.stream().map(SellerRelevanceFactors::getSellerId).distinct()
	                            .collect(Collectors.toList()));
	        }
	        finalResultMap.put(UPDATED, validList);
	        finalResultMap.put(NOT_UPDATED, inValidList);
	        return finalResultMap;
	    }

	    public Map<String, List<SellerRelevanceFactors>> updateSellerRelevanceFactors(
	            List<SellerRelevanceFactors> sellerRelevanceFactorsUpdate) {

	        BeanUtilsBean beanUtilsBean = new ExclusionAwareBeanUtilsBean();
	        List<SellerRelevanceFactors> updatedSellerRelevanceFactors = new ArrayList<>();
	        List<SellerRelevanceFactors> notUpdatedSellerRelevanceFactors = new ArrayList<>();

	        Map<String, List<SellerRelevanceFactors>> finalResultMap = new HashMap<>();

	        for (SellerRelevanceFactors sellerRelevanceFactor : sellerRelevanceFactorsUpdate) {
	            SellerRelevanceFactors sellerRelevanceFactorsTmp =
	                    sellerRelevanceFactorsDao.findOne(sellerRelevanceFactor.getId());
	            Integer downGradedSellerRelevanceId = null;
	            if (checkCategoryDowngrade(sellerRelevanceFactor, sellerRelevanceFactorsTmp)) {
	                downGradedSellerRelevanceId = sellerRelevanceFactorsTmp.getId();
	            }
	            try {
	                beanUtilsBean.copyProperties(sellerRelevanceFactorsTmp, sellerRelevanceFactor);
	            }
	            catch (IllegalAccessException | InvocationTargetException e) {
	                logger.error(
	                        "Unable to copy properties of seller relevance factors for id - "
	                                + sellerRelevanceFactor.getId());
	            }

	            List<SellerRelevanceFactors> inValidList =
	                    validateData(Collections.singletonList(sellerRelevanceFactorsTmp), false);
	            if (CollectionUtils.isEmpty(inValidList)) {
	                try {
	                    updatedSellerRelevanceFactors.add(
	                            applicationContext.getBean(SellerRelevanceFactorsService.class).updateDeleteSellerRelevance(
	                                    sellerRelevanceFactorsTmp,
	                                    sellerRelevanceFactor.getSellerRelevanceFactorPackage(),
	                                    downGradedSellerRelevanceId));
	                }
	                catch (Exception e) {
	                    logger.error("Error in saving seller relevance factors", e);
	                    notUpdatedSellerRelevanceFactors.add(sellerRelevanceFactorsTmp);
	                }
	            }
	            else {
	                notUpdatedSellerRelevanceFactors.add(sellerRelevanceFactorsTmp);
	            }
	        }
	        if (CollectionUtils.isNotEmpty(updatedSellerRelevanceFactors)) {
	            sellerRelevanceHelper.sendNotificationForSRFStatusChange(
	                    updatedSellerRelevanceFactors.stream().map(SellerRelevanceFactors::getSellerId).distinct()
	                            .collect(Collectors.toList()));
	        }
	        finalResultMap.put(UPDATED, updatedSellerRelevanceFactors);
	        finalResultMap.put(NOT_UPDATED, notUpdatedSellerRelevanceFactors);

	        if (CollectionUtils.isNotEmpty(updatedSellerRelevanceFactors)) {
	            deleteSellerRelevanceCacheBySellerIdAndSaleType(updatedSellerRelevanceFactors);
	        }

	        return finalResultMap;
	    }

	    private void updatePackageDuration(Collection<SellerRelevanceFactors> sellerRelevanceFactors) {
	        Map<Integer, Map<SellerRelevanceFactors, List<ProductPaymentStatus>>> srfnListForUpdatePackageEndDate =
	                new HashMap<>();
	        Map<SellerRelevanceFactors, List<ProductPaymentStatus>> tempSrfnForUpdatePackageEndDate = new HashMap<>();
	        for (SellerRelevanceFactors sellerRelevanceFactor : sellerRelevanceFactors) {
	            List<ProductPaymentStatus> productPaymentStatusList = productPaymentService.getLeadPaymentListFromDate(
	                    sellerRelevanceHelper.getFormattedDate(-1 * NO_OF_DAYS_PAYMENTS),
	                    sellerRelevanceFactor.getSellerId(),
	                    sellerRelevanceFactor.getSaleTypeId());

	            tempSrfnForUpdatePackageEndDate.put(sellerRelevanceFactor, productPaymentStatusList);
	            srfnListForUpdatePackageEndDate.put(sellerRelevanceFactor.getId(), tempSrfnForUpdatePackageEndDate);
	        }
	        sellerRelevanceHelper.calculateAndUpdatePackageDuration(srfnListForUpdatePackageEndDate);
	    }

	    // @Transactional
	    public SellerRelevanceFactors updateDeleteSellerRelevance(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            Collection<SellerRelevancePackage> sellerRelevancePackages,
	            Integer downGradedSellerRelevanceId) {
	        sellerRelevanceFactors = applicationContext.getBean(SellerRelevanceFactorsService.class)
	                .saveDataWithScores(sellerRelevanceFactors, sellerRelevancePackages);
	        if (downGradedSellerRelevanceId != null) {
	            deletePackageComponents(downGradedSellerRelevanceId);
	        }
	        return sellerRelevanceFactors;
	    }

	    @Transactional
	    public String patchSellerRelevanceFactors(List<SellerRelevanceFactors> sellerRelevanceFactors) {
	        List<Integer> sellerRelevanceFactorIds = new ArrayList<>();
	        for (SellerRelevanceFactors sellerRelevanceFactor : sellerRelevanceFactors) {
	            if (null == sellerRelevanceFactor.getSellerTransGroupId()) {
	                logger.error(
	                        "Patch API alert : Seller Transaction group Id cannot be null for seller relevance Id "
	                                + sellerRelevanceFactor.getId());
	                continue;
	            }
	            sellerRelevanceFactorsDao.updateSellerRelevanceFactorsGroupId(
	                    sellerRelevanceFactor.getId(),
	                    sellerRelevanceFactor.getSellerTransGroupId());
	            sellerRelevanceFactorIds.add(sellerRelevanceFactor.getId());
	        }
	        if (CollectionUtils.isNotEmpty(sellerRelevanceFactors)) {
	            sellerRelevanceHelper.sendNotificationForSRFStatusChange(
	                    sellerRelevanceFactors.stream().map(SellerRelevanceFactors::getSellerId).distinct()
	                            .collect(Collectors.toList()));
	        }

	        /*
	         * Trigger listing indexing for updated sellers
	         */
//	        solrListingIndexingQueueService.process(Listing.FIELD_NAME_SELLER_ID, sellerRelevanceFactorIds, Domain.Makaan);

	        updatePackageDuration(sellerRelevanceFactors);
	        return "Updated seller relevance factors for Ids - " + sellerRelevanceFactorIds.toString();
	    }

	    public void saveSellerRelevanceScores(
	            List<SellerRelevanceFactorScoreMapping> scoreMappingList,
	            SellerRelevanceFactors sellerRelevanceFactor) {
	        for (SellerRelevanceFactorScoreMapping scoreMapping : scoreMappingList) {
	            scoreMapping.setSellerRelevanceFactorId(sellerRelevanceFactor.getId());
	        }
	        sellerRelevanceFactorScoreMappingDao
	                .deleteSellerRelevanceScoreMappingForRelevanceId(sellerRelevanceFactor.getId());
	        sellerRelevanceFactorScoreMappingDao.save(scoreMappingList);
	    }

	    @Transactional
	    public SellerRelevanceFactors saveDataWithScores(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            Collection<SellerRelevancePackage> sellerRelevancePackages) {
	        Map<Integer, Map<SellerRelevanceFactors, List<ProductPaymentStatus>>> srfnListForUpdatePackageEndDate =
	                new HashMap<>();
	        if (sellerRelevanceHelper.isSellerStatusPaid(sellerRelevanceFactors.getSellerTransGroupId())) {
	            Map<SellerRelevanceFactors, List<ProductPaymentStatus>> tempSrfnForUpdatePackageEndDate = new HashMap<>();
	            Integer sellerUserId = sellerRelevanceFactors.getSellerId();
	            Integer saleTypeId = sellerRelevanceFactors.getSaleTypeId();
	            List<ProductPaymentStatus> productPaymentStatusList = productPaymentService.getLeadPaymentListFromDate(
	                    sellerRelevanceHelper.getFormattedDate(-1 * NO_OF_DAYS_PAYMENTS),
	                    sellerUserId,
	                    saleTypeId);
	            validateSRFStatusForPaidSellers(sellerRelevanceFactors, productPaymentStatusList);
	            RawSellerDTO rawSellerDTO=midlServiceHelper.getRawSellerFromSolrViaSellerUserId(sellerUserId);
	            sellerRelevanceHelper.createOrUpdateSRFForNewPayments(
	                    sellerUserId,
	       // @Divyanshu     rawSellerService.getRawSellerFromSolrViaSellerUserId(sellerUserId),
	                  //  midlServiceHelper.getRawSellerFromSolrViaSellerUserId(sellerUserId),
	                   rawSellerDTO,
	                    productPaymentStatusList,
	                    sellerRelevanceFactors,
	                    sellerRelevanceFactors.getSellerTransGroupId(),
	                    MasterLeadPaymentTypeEnum.getMasterLeadPaymentTypeEnumById(saleTypeId));
	            tempSrfnForUpdatePackageEndDate.put(sellerRelevanceFactors, productPaymentStatusList);
	            srfnListForUpdatePackageEndDate.put(sellerRelevanceFactors.getId(), tempSrfnForUpdatePackageEndDate);
	        }
	        SellerRelevanceFactors sellerRelevanceFactorsDB =
	                sellerRelevanceFactorsDao.saveAndFlush(sellerRelevanceFactors);
	        sellerRelevancePackageService
	                .saveSellerRelevancePackage(sellerRelevancePackages, sellerRelevanceFactorsDB.getId());
	        updateScoreForUser(sellerRelevanceFactorsDB);
	        updateSRFForOtherSaleTypesIfFullyPenalizedOrAcLocked(sellerRelevanceFactors);

	        /*
	         * Trigger listing indexing for updated sellers
	         */
//	        solrListingIndexingQueueService.process(
//	                Listing.FIELD_NAME_SELLER_ID,
//	                Collections.singletonList(sellerRelevanceFactorsDB.getSellerId()),
//	                Domain.Makaan);

	        sellerRelevanceHelper.calculateAndUpdatePackageDuration(srfnListForUpdatePackageEndDate);
	        return sellerRelevanceFactorsDB;
	    }


	    private void updateScoreForUser(SellerRelevanceFactors sellerRelevanceFactors) {
	        // sellerRelevanceFactors =
	        // sellerRelevanceFactorsDao.save(sellerRelevanceFactors);
	        List<SellerRelevanceFactorScoreMapping> scoreMappingList =
	                sellerRelevanceHelper.setScoresForUserV2(sellerRelevanceFactors);
	        saveSellerRelevanceScores(scoreMappingList, sellerRelevanceFactors);
	    }
	    
	    private boolean validateNewSRFStatus(
	            SellerRelevanceFactors sellerRelevanceFactorsDB,
	            SellerRelevanceFactors sellerRelevanceFactorsNew) {
	        if (sellerRelevanceHelper.isSellerStatusPaid(sellerRelevanceFactorsDB.getSellerTransGroupId())) {
	            if (TransactionCategoryGroups.BOOSTED.getId().equals(sellerRelevanceFactorsNew.getSellerTransGroupId())) {
	                throw new BadRequestException(
	                        "Cannot set boosted status for paid sellers " + sellerRelevanceFactorsDB.getSellerId());
	            }
	        }
	        return true;
	    }

	    private boolean validateSRFStatusForPaidSellers(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            List<ProductPaymentStatus> productPaymentStatusList) {
	        if (CollectionUtils.isEmpty(productPaymentStatusList)) {
	            throw new BadRequestException(
	                    "Seller " + sellerRelevanceFactors.getSellerId() + " has not done any payment");
	        }
	        Pair<Integer, Integer> dealClosurePair = sellerRelevanceHelper.getDealClosureCount(productPaymentStatusList);
	        if (dealClosurePair.getFirst() <= 0 && !TransactionCategoryGroups.PREPAID_SELLER.getId()
	                .equals(sellerRelevanceFactors.getSellerTransGroupId())) {
	            throw new BadRequestException(
	                    "Seller " + sellerRelevanceFactors.getSellerId() + " has not closed any deal");
	        }
	        return true;
	    }

	    private void updateSRFForOtherSaleTypesIfFullyPenalizedOrAcLocked(SellerRelevanceFactors sellerRelevanceFactors) {
	        if (sellerRelevanceHelper.isSellerStatusPaid(sellerRelevanceFactors.getSellerTransGroupId())) {
	            List<SellerRelevanceFactors> sellerSRFsList =
	                    sellerRelevanceFactorsDao.findBySellerId(sellerRelevanceFactors.getSellerId());

	            for (SellerRelevanceFactors srf : sellerSRFsList) {
	                if (!srf.getId().equals(sellerRelevanceFactors.getId())
	                        && (TransactionCategoryGroups.FULLY_PENALISED.getId().equals(srf.getSellerTransGroupId()))
	                        || TransactionCategoryGroups.ACCOUNT_LOCKED.getId().equals(srf.getSellerTransGroupId())) {
	                    srf.setSellerTransGroupId(TransactionCategoryGroups.PARTIAL_PENALISED.getId());
	                    applicationContext.getBean(SellerRelevanceFactorsService.class).saveDataWithScores(srf, null);
	                }
	            }
	        }
	    }

	    private List<SellerRelevanceFactors> validateData(
	            List<SellerRelevanceFactors> sellerRelevanceFactors,
	            boolean removeInvalidData) {

	        List<SellerRelevanceFactors> inValidSellerRelevanceFactors = new ArrayList<>();
	        for (SellerRelevanceFactors sellerRelevanceFactor : sellerRelevanceFactors) {

	            if (null == sellerRelevanceFactor.getSaleTypeId() && null == sellerRelevanceFactor.getId()) {
	                inValidSellerRelevanceFactors.add(sellerRelevanceFactor);
	                logger.error(
	                        "Sale Type Id cannot be null for seller Id -" + sellerRelevanceFactor.getSellerId()
	                                + " group id - "
	                                + sellerRelevanceFactor.getSellerTransGroupId());

	                continue;
	            }
	            validateDataOnConditions(sellerRelevanceFactor, inValidSellerRelevanceFactors);
	        }

	        if (removeInvalidData) {
	            sellerRelevanceFactors.removeAll(inValidSellerRelevanceFactors);
	        }

	        return inValidSellerRelevanceFactors;
	    }

	    private boolean checkCategoryDowngrade(
	            SellerRelevanceFactors sellerRelevanceFactorsNew,
	            SellerRelevanceFactors sellerRelevanceFactorsOld) {

	        if (sellerRelevanceFactorsOld.getSellerTransGroupId()
	                .equals(TransactionCategoryGroups.EXPERT_DEAL_MAKER.getId())
	                && !sellerRelevanceFactorsNew.getSellerTransGroupId()
	                        .equals(TransactionCategoryGroups.EXPERT_DEAL_MAKER.getId())) {

	            return true;
	        }
	        return false;
	    }

	    public void deletePackageComponents(Integer packageRemovesellerRelevanceId) {
	        if (null != packageRemovesellerRelevanceId) {
	            List<Integer> sellerPackageIds =
	                    sellerRelevancePackageService.getPackageIdsBySellerRelevanceId(packageRemovesellerRelevanceId);
	            if (!CollectionUtils.isEmpty(sellerPackageIds)) {
	                sellerRelevancePackageService.deleteSellerRelevancePackage(sellerPackageIds);
	            }
	        }
	    }

	    public Collection<SellerRelevancePackage> addPackagesAndComponents(
	            Collection<SellerRelevancePackage> sellerRelevancePackages,
	            Integer sellerRelevanceId) {
	        return sellerRelevancePackageService.saveSellerRelevancePackage(sellerRelevancePackages, sellerRelevanceId);
	    }

	    private void validateDataOnConditions(
	            SellerRelevanceFactors sellerRelevanceFactor,
	            List<SellerRelevanceFactors> inValidSellerRelevanceFactors) {

	        List<TransactionCategoryGroupsMapping> transactionCategoryGroupsMappingList =
	                sellerTransactionCategoryGroupMappingDao
	                        .findBycategoryGroupId(sellerRelevanceFactor.getSellerTransGroupId());

	        for (TransactionCategoryGroupsMapping transactionCategoryGroupsMapping : transactionCategoryGroupsMappingList) {

	            switch (SellerTransactionCategory.getById(transactionCategoryGroupsMapping.getTransactionCategoryId())) {
	                case PARTIAL_PENALISED:
	                    validatePartialPenalisedConditions(sellerRelevanceFactor, inValidSellerRelevanceFactors);
	                    break;

	                case BOOSTED:
	                    validateBoostedConditions(sellerRelevanceFactor, inValidSellerRelevanceFactors);
	                    break;

	                default:
	            }
	        }
	    }

	    private void validatePartialPenalisedConditions(
	            SellerRelevanceFactors sellerRelevanceFactor,
	            List<SellerRelevanceFactors> inValidSellerRelevanceFactors) {

	        if (sellerRelevanceFactor.getSaleTypeId().equals(MasterLeadPaymentTypeEnum.Buy.getId())) {

	            if (sellerRelevanceFactor.getCap() == null || sellerRelevanceFactor.getCap() < 0) {
	                inValidSellerRelevanceFactors.add(sellerRelevanceFactor);
	                logger.error(
	                        "Buy cap is null,Could not save Partial Penalize Relevance factor for sellerId: "
	                                + sellerRelevanceFactor.getSellerId());
	            }

	        }
	        else if (sellerRelevanceFactor.getSaleTypeId().equals(MasterLeadPaymentTypeEnum.Rent.getId())) {
	            if (sellerRelevanceFactor.getCap() == null || sellerRelevanceFactor.getCap() < 0) {
	                inValidSellerRelevanceFactors.add(sellerRelevanceFactor);
	                logger.error(
	                        "Rent cap is null,Could not save Partial Penalize Relevance factor for sellerId: "
	                                + sellerRelevanceFactor.getSellerId());
	            }
	        }
	    }

	    private void validateBoostedConditions(
	            SellerRelevanceFactors sellerRelevanceFactor,
	            List<SellerRelevanceFactors> inValidSellerRelevanceFactors) {

	        if (sellerRelevanceFactor.getSaleTypeId().equals(MasterLeadPaymentTypeEnum.Buy.getId())) {

	            if (sellerRelevanceFactor.getCap() == null || sellerRelevanceFactor.getCap().equals(0)) {
	                inValidSellerRelevanceFactors.add(sellerRelevanceFactor);
	                logger.error(
	                        "Buy cap is null,Could not save Boosted Relevance factor for sellerId: "
	                                + sellerRelevanceFactor.getSellerId());
	            }

	        }
	        else if (sellerRelevanceFactor.getSaleTypeId().equals(MasterLeadPaymentTypeEnum.Rent.getId())) {
	            if (sellerRelevanceFactor.getCap() == null || sellerRelevanceFactor.getCap().equals(0)) {
	                inValidSellerRelevanceFactors.add(sellerRelevanceFactor);
	                logger.error(
	                        "Rent cap is null,Could not save Boosted Relevance factor for sellerId: "
	                                + sellerRelevanceFactor.getSellerId());
	            }
	        }
	    }

	    @Cacheable(value = Constants.CacheName.SELLER_RELEVANCE_FACTORS)
	    public List<Integer> getSellersBySaleTypeTransactionId(
	            Collection<Integer> transactionCategoryId,
	            Integer saleTypeId) {
	        List<SellerRelevanceFactors> sellerRelevanceFactorsList =
	                sellerRelevanceFactorsDao.getSellersByTransactionCategoryAndSaleType(transactionCategoryId, saleTypeId);
	        return sellerRelevanceFactorsList.stream().map(s -> s.getSellerId()).collect(Collectors.toList());
	    }

	    @Async
	    private void updateSellerRelevanceCacheBySellerIdAndSaleType(List<SellerRelevanceFactors> sellerRelevanceFactors) {
	        for (SellerRelevanceFactors sellerRelevanceFactor : sellerRelevanceFactors) {
	            sellerRelevanceCacheService.setSellerRelevanceToCache(
	                    sellerRelevanceFactor.getSellerId(),
	                    sellerRelevanceFactor.getSaleTypeId(),
	                    sellerRelevanceFactor);
	        }
	    }

	    @Async
	    private void deleteSellerRelevanceCacheBySellerIdAndSaleType(List<SellerRelevanceFactors> sellerRelevanceFactors) {
	        for (SellerRelevanceFactors sellerRelevanceFactor : sellerRelevanceFactors) {
	            sellerRelevanceCacheService.deleteSellerRelevanceFromCache(
	                    sellerRelevanceFactor.getSellerId(),
	                    sellerRelevanceFactor.getSaleTypeId());
	            //Commented by @Divyanshu
	//            appNotificationCacheService
	//                    .deleteSellerDashboardNotificationV2FromCache(sellerRelevanceFactor.getSellerId());
	        }
	    }

	    public List<SellerRelevanceFactors> getSellersBySaleTypeTransactionCategoryIds(
	            List<Integer> transactionCategory,
	            Integer saleTypeId) {
	        List<SellerRelevanceFactors> sellerRelevanceFactorsList =
	                sellerRelevanceFactorsDao.findByTransactionCategoryIds(transactionCategory, saleTypeId);
	        return sellerRelevanceFactorsList;
	    }
//????????????
	    @Async
	    public void multiplyLeadForOwner(Set<Integer> listingsIds) {
	//@Divyanshu        Map<Integer, Listing> listings = listingService.findMapByListingIds(listingsIds, Domain.Makaan);
	        Map<Integer,Listing> listings = midlServiceHelper.findMapByListingIds(listingsIds, Domain.Makaan);
	    	for (Listing l : listings.values()) {
	            applicationContext.getBean(SellerRelevanceFactorsService.class).multiplyLeadForOwnerAsync(l);
	        }
	    }

	    @Async
	    public void multiplyLeadForOwnerAsync(Listing l) {
	        cyclopsServiceHelper.multiplyLeadForSeller(
	                l.getSellerId(),
	                l.getListingCategory().name(),
	                Integer.valueOf(l.getProperty().getProject().getLocalityId()),
	                Long.valueOf(l.getCurrentListingPrice().getPrice()),
	                null,
	                Integer.valueOf(l.getProperty().getProject().getLocality().getSuburb().getCityId()),
	                NUMBER_OF_LEADTO_MULTIPLY,
	                l.getId());
	    }

	    public Map<Integer, String> getHighestBadge(Set<Integer> sellerIds) {
	        
	        List<SellerRelevanceFactors> factors = sellerRelevanceFactorsDao.findBySellerIdsWithPackages(sellerIds);

	        Map<Integer, List<SellerRelevanceFactors>> sellerToFactorsMap =  new HashMap<>();
	        for(SellerRelevanceFactors factor: factors) {
	            
	            List<SellerRelevanceFactors> srfs = sellerToFactorsMap.get(factor.getSellerId());
	            if (srfs == null) {
	                srfs = new ArrayList<>();
	            }
	            srfs.add(factor);
	            sellerToFactorsMap.put(factor.getSellerId(), srfs);

	        }
	        
	        Map<Integer, String> sellerBadgeMap = new HashMap<>();
	        sellerToFactorsMap.forEach((k, v) -> {
	            String badge =
	                    sellerRelevanceHelper.getSellerSRFOrderedSetByPriority(v);
	            sellerBadgeMap.put(k, badge);

	        });
	        return sellerBadgeMap;
	    }
}
