package com.proptiger.app.service.cron;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.app.repo.srf.SellerRelevanceFactorsDao;
import com.proptiger.app.repo.srf.SellerVisibilityRankingsDao;
import com.proptiger.app.services.srf.SellerRelevanceFactorsService;
import com.proptiger.app.services.srf.SellerRelevanceHelper;
import com.proptiger.app.services.srf.SellerRelevancePackageComponentsService;
import com.proptiger.app.services.srf.SellerRelevanceService;
import com.proptiger.app.services.srf.SellerVisibilityRankingsService;
import com.proptiger.core.enums.Domain;
import com.proptiger.core.enums.external.mbridge.SellerSuspensionStatus;
import com.proptiger.core.enums.external.mbridge.SellerType;
import com.proptiger.core.helper.ICRMHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.cms.Listing;
import com.proptiger.core.model.cms.SellerRelevanceFactorScoreMapping;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.SellerRelevancePackage;
import com.proptiger.core.model.cms.SellerRelevancePackageComponents;
import com.proptiger.core.model.cms.SellerVisibilityRankings;
import com.proptiger.core.model.cms.MasterSellerRelevancePackageType.MasterSellerRelevancePackageTypeEnum;
import com.proptiger.core.model.cms.MasterTransactionCategoryGroup.TransactionCategoryGroups;
import com.proptiger.core.model.cms.RawSellerDTO;
import com.proptiger.core.model.cms.SellerTransactionCategories.SellerTransactionCategory;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentTypeEnum;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.Pair;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.util.DateUtil;
//import com.proptiger.data.service.SolrListingIndexingQueueService;
//import com.proptiger.data.service.cron.SellerRelevanceCronService;
//import com.proptiger.data.service.cron.SellerRelevanceFactorsDao;
//import com.proptiger.data.service.cron.SellerRelevanceFactorsService;
//import com.proptiger.data.service.cron.SellerRelevanceHelper;
//import com.proptiger.data.service.cron.SellerRelevancePackageComponentsService;
//import com.proptiger.data.service.cron.SellerRelevanceService;
//import com.proptiger.data.service.cron.SellerVisibilityRankingsDao;
//import com.proptiger.data.service.cron.SellerVisibilityRankingsService;

//import com.proptiger.data.service.marketplace.ListingScoreService;
//import com.proptiger.marketforce.data.model.RawSeller;
//import com.proptiger.marketforce.data.model.RawSeller.SellerSuspensionStatus;
//import com.proptiger.marketforce.data.model.RawSeller.SellerType;
//import com.proptiger.marketforce.data.service.RawSellerService;
import com.rits.cloning.Cloner;

@Service
public class SellerRelevanceCronService {

	 @Autowired
	    private SellerRelevanceService            sellerRelevanceService;

	    @Autowired
	    private SellerRelevanceFactorsService     sellerRelevanceFactorsService;

	    @Autowired
	    SellerRelevanceFactorsDao                 sellerRelevanceFactorsDao;

	    @Autowired
	    private MIDLServiceHelper				midlServiceHelper;
	    
//	    private RawSellerService                  rawSellerService;

	    @Autowired
	    private ApplicationContext                applicationContext;

	    @Autowired
	    SellerVisibilityRankingsService           sellerVisibilityRankingsService;

	    @Autowired
	    SellerVisibilityRankingsDao               sellerVisibilityRankingsDao;

	    @Autowired
	    SellerRelevancePackageComponentsService   sellerRelevancePackageComponentsService;

//	    @Autowired
//	    ListingScoreService                       listingScoreService;

	    @Autowired
	    private SellerRelevanceHelper             sellerRelevanceHelper;

//	    @Autowired
//	    private SolrListingIndexingQueueService   solrListingIndexingQueueService;

	    @Autowired
	    private ICRMHelper                        icrmHelper;

	    @Value("${seller.non.penalize.buy.payment.count}")
	    private int                               BUY_NON_PENALIZE_COUNT;

	    @Value("${seller.non.penalize.rent.payment.count}")
	    private int                               RENT_NON_PENALIZE_COUNT;

	    @Value("${seller.account.locked.buy.payment.count}")
	    private int                               BUY_ACCOUNT_LOCKED_COUNT;

	    @Value("${seller.account.locked.rent.payment.count}")
	    private int                               RENT_ACCOUNT_LOCKED_COUNT;

	    @Value("${seller.non.penalize.day.count}")
	    private int                               NON_PENALIZE_PREVIOUS_DAY_COUNT;

	    @Value("${seller.deal.maker.expiration.day.count.inc}")
	    private int                               DEAL_MAKER_EXPIRATION_DAY_COUNT_INC;

	    @Value("${seller.partial.penalize.lead.delivery.days}")
	    private int                               PARTIAL_PENALIZE_LEAD_DELIVERY_DAYS;

	    private static final int                  SUSPENSION_REMOVAL_PREVIOUS_PAYMENTS_DAY_COUNT = 15;

	    private static final Predicate<RawSellerDTO> suspensionPredicate                            = (rSeller) -> {
	                                                                                                 return (SellerSuspensionStatus.Suspended
	                                                                                                         .equals(
	                                                                                                                 rSeller.getSuspensionStatus())
	                                                                                                         || SellerSuspensionStatus.ToBeSuspended
	                                                                                                                 .equals(
	                                                                                                                         rSeller.getSuspensionStatus()));
	                                                                                             };

	    @Value("${seller.relevance-factors.payment.days}")
	    public int                                NO_OF_DAYS_PAYMENTS;

	    private static final Integer              ROWS                                           = 200;

	    private static Logger                     logger                                         =
	            LoggerFactory.getLogger(SellerRelevanceCronService.class);

	    @PostConstruct
	    public void init() {
	//        rawSellerService = applicationContext.getBean(RawSellerService.class);
	  //       sellerRelevanceService =
	   //      applicationContext.getBean(SellerRelevanceService.class);
	    }

	    public Map<String, Object> processSellers(boolean saveSellerInfo, Integer sellerUserId) {
	        Map<Integer, Map<Integer, SellerRelevanceFactors>> sellerRevelanceFactorMap =
	                getSellerRelevanceFactors(sellerUserId);
	        String dateBeforeNumDays = sellerRelevanceHelper.getFormattedDate(-1 * NO_OF_DAYS_PAYMENTS);
	        Map<Integer, Map<Integer, List<ProductPaymentStatus>>> crmPaymentMap =
	                sellerRelevanceHelper.getSellerPaymentMap(dateBeforeNumDays, sellerUserId, null);
	        boolean triggerListingIndexing = sellerUserId != null ? true : false;
	        return processSellersInternal(crmPaymentMap, sellerRevelanceFactorMap, saveSellerInfo, triggerListingIndexing);
	    }

	    /************************************************************************************************
	     *
	     * PRIVATE METHODS
	     * 
	     ************************************************************************************************/
	    private Map<String, Object> processSellersInternal(
	            Map<Integer, Map<Integer, List<ProductPaymentStatus>>> origCrmPaymentMap,
	            Map<Integer, Map<Integer, SellerRelevanceFactors>> allSellerRevelanceFactorMaps,
	            boolean saveSellerInfo,
	            boolean triggerListingIndexing) {
	        Set<Integer> failedSellerIdSet = new HashSet<>();
	        List<SellerRelevanceFactors> updatedSellerRelevanceFactors = new LinkedList<>();
	        Map<Integer, Map<Integer, List<ProductPaymentStatus>>> crmPaymentMapClone =
	        		new Cloner().deepClone(origCrmPaymentMap);
	      //          new Cloner().deepClone(origCrmPaymentMap);
//TODO 
	        allSellerRevelanceFactorMaps.entrySet().stream().forEach(entry -> {
	            Integer sellerUserId = entry.getKey();
	            Map<Integer, SellerRelevanceFactors> sellerSaleTypeToSRFMap = entry.getValue();
	            try {
	        //Divyanshu        RawSeller rawSeller = rawSellerService.getRawSellerFromSolrViaSellerUserId(sellerUserId);
	            		RawSellerDTO rawSeller = midlServiceHelper.getRawSellerFromSolrViaSellerUserId(sellerUserId);
	                boolean isSellerFullyPenalizedOrAcLocked = sellerSaleTypeToSRFMap.entrySet().stream()
	                        .filter(
	                                s -> s.getValue().getTransactionCategoryGroup().getId()
	                                        .equals(TransactionCategoryGroups.FULLY_PENALISED.getId())
	                                        || s.getValue().getTransactionCategoryGroup().getId()
	                                                .equals(TransactionCategoryGroups.ACCOUNT_LOCKED.getId()))
	                        .findAny().isPresent();
	                // Fully penalized and account locked seller is special case and
	                // need to be handled
	                // separately
	                if (isSellerFullyPenalizedOrAcLocked) {
	                    // check fully penalized case
	                    processSRFStatusForSellersWithForSpecialCase(
	                            sellerUserId,
	                            crmPaymentMapClone,
	                            sellerSaleTypeToSRFMap,
	                            rawSeller,
	                            updatedSellerRelevanceFactors,
	                            saveSellerInfo);
	                }
	                else {
	                    sellerSaleTypeToSRFMap.entrySet().stream().forEach(entry2 -> {
	                        int saleType = entry2.getKey();
	                        SellerRelevanceFactors sellerRelevanceFactor = entry2.getValue();
	                        try {
	                            List<ProductPaymentStatus> productPaymentStatusList =
	                                    crmPaymentMapClone.get(sellerUserId) != null
	                                            ? crmPaymentMapClone.get(sellerUserId).get(saleType)
	                                            : Collections.emptyList();
	                            processSRFStatusForSeller(
	                                    sellerUserId,
	                                    rawSeller,
	                                    sellerRelevanceFactor,
	                                    saleType,
	                                    productPaymentStatusList,
	                                    updatedSellerRelevanceFactors,
	                                    saveSellerInfo);
	                        }
	                        catch (Exception e) {
	                            logger.error("Failed for " + saleType + " seller id - " + sellerUserId, e);
	                            failedSellerIdSet.add(sellerUserId);
	                        }
	                        finally {
	                            if (crmPaymentMapClone.get(sellerUserId) != null) {
	                                crmPaymentMapClone.get(sellerUserId).remove(saleType);
	                            }
	                        }
	                    });
	                }
	                // suspension
	                validateAndRemoveSuspension(sellerUserId, origCrmPaymentMap, rawSeller);
	            }
	            catch (Exception e) {
	                logger.error("Failed for seller id - " + sellerUserId, e);
	                if (crmPaymentMapClone.get(sellerUserId) != null) {
	                    crmPaymentMapClone.get(sellerUserId).clear();
	                }
	                failedSellerIdSet.add(sellerUserId);
	            }

	        });
	        // process sellers who have paid for first time
	        crmPaymentMapClone.entrySet().stream().forEach(entry -> {
	            Integer sellerUserId = entry.getKey();
	            Map<Integer, List<ProductPaymentStatus>> saleTypeToPaymentsMap = entry.getValue();
	            try {
	            	
	// Divyanshu         RawSellerDTO rawSeller = rawSellerService.getRawSellerFromSolrViaSellerUserId(sellerUserId);
	            		RawSellerDTO rawSeller = midlServiceHelper.getRawSellerFromSolrViaSellerUserId(sellerUserId);
	                saleTypeToPaymentsMap.entrySet().stream().forEach(entry2 -> {
	                    Integer saleTypeId = entry2.getKey();
	                    List<ProductPaymentStatus> productPaymentStatusList = entry2.getValue();
	                    try {
	                        processNewPayments(
	                                sellerUserId,
	                                rawSeller,
	                                saleTypeId,
	                                productPaymentStatusList,
	                                updatedSellerRelevanceFactors,
	                                saveSellerInfo);
	                    }
	                    catch (Exception e) {
	                        logger.error("Failed for " + saleTypeId + " seller id - " + sellerUserId, e);
	                        failedSellerIdSet.add(sellerUserId);
	                    }
	                });
	                // suspension
	                validateAndRemoveSuspension(sellerUserId, origCrmPaymentMap, rawSeller);
	            }
	            catch (Exception e) {
	                logger.error("Failed for seller id - " + sellerUserId, e);
	                if (crmPaymentMapClone.get(sellerUserId) != null) {
	                    crmPaymentMapClone.get(sellerUserId).clear();
	                }
	                failedSellerIdSet.add(sellerUserId);
	            }
	        });
	        /*
	         * Trigger listing indexing for updated sellers
	         * @Divyanshu commented for the time
	         */
	        if (triggerListingIndexing) {
//Divyanshu	            solrListingIndexingQueueService.process(
//	                    Listing.FIELD_NAME_SELLER_ID,
//	                    updatedSellerRelevanceFactors.stream().map(srf -> srf.getSellerId()).collect(Collectors.toList()),
//	                    Domain.Makaan);
	        	//OK
	        		midlServiceHelper.process(
		                    Listing.FIELD_NAME_SELLER_ID,
		                    updatedSellerRelevanceFactors.stream().map(srf -> srf.getSellerId()).collect(Collectors.toList()),
		                    Domain.Makaan);
	        }
	        return responseBuilder(updatedSellerRelevanceFactors, failedSellerIdSet);

	    }

	    private void processNewPayments(
	            Integer sellerUserId,
	            RawSellerDTO rawSeller,
	            int saleTypeId,
	            List<ProductPaymentStatus> productPaymentStatusList,
	            List<SellerRelevanceFactors> updatedSellerRelevanceFactors,
	            boolean saveSellerInfo) {
	        if (CollectionUtils.isNotEmpty(productPaymentStatusList)) {
	            Integer transGroupId = getTransGroupIdFromPayments(productPaymentStatusList, rawSeller);
	            SellerRelevanceFactors sellerRelevanceFactors =
	                    new SellerRelevanceFactors(sellerUserId, saleTypeId, transGroupId);
	            sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                    sellerUserId,
	                    rawSeller,
	                    productPaymentStatusList,
	                    sellerRelevanceFactors,
	                    transGroupId,
	                    MasterLeadPaymentTypeEnum.getMasterLeadPaymentTypeEnumById(saleTypeId));
	            List<SellerRelevanceFactorScoreMapping> sellerRelevanceFactorScoreMappings =
	                    sellerRelevanceHelper.setScoresForUserV2(sellerRelevanceFactors);
	            List<SellerVisibilityRankings> svr =
	                    getSellerVisibilityRankings(sellerRelevanceFactors, productPaymentStatusList);
	            List<SellerRelevancePackage> newPackagesToBeAdded = getSellerPackagesAndComponentsForAddition(
	                    sellerUserId,
	                    rawSeller,
	                    null,
	                    sellerRelevanceFactors,
	                    productPaymentStatusList);
	            saveSellerRelevanceFactorsTransactional(
	                    sellerRelevanceFactors,
	                    newPackagesToBeAdded,
	                    sellerRelevanceFactorScoreMappings,
	                    svr,
	                    saveSellerInfo);
	            updatedSellerRelevanceFactors.add(sellerRelevanceFactors);
	        }

	    }

	    private void processSRFStatusForSeller(
	            Integer sellerUserId,
	            RawSellerDTO rawSeller,
	            SellerRelevanceFactors sellerRelevanceFactor,
	            int saleType,
	            List<ProductPaymentStatus> productPaymentStatusList,
	            List<SellerRelevanceFactors> updatedSellerRelevanceFactors,
	            boolean saveSellerInfo) {
	        if (sellerRelevanceFactor == null) {
	            return;
	        }
	        Integer categoryGroupId = sellerRelevanceFactor.getSellerTransGroupId();
	        // Get updated status of seller
	        SellerRelevanceFactors updatSellerRelevanceFactors = computeSRFStatusForSeller(
	                sellerUserId,
	                rawSeller,
	                productPaymentStatusList,
	                saleType,
	                sellerRelevanceFactor,
	                MasterLeadPaymentTypeEnum.getMasterLeadPaymentTypeEnumById(saleType));

	        // Handle packages & components deletion of packages
	        if (updatSellerRelevanceFactors.isDirty()
	                && !categoryGroupId.equals(updatSellerRelevanceFactors.getSellerTransGroupId())) {
	            // Handle packages & components deletion of packages
	            deleteStalePackageAndComponents(
	                    sellerUserId,
	                    rawSeller,
	                    categoryGroupId,
	                    updatSellerRelevanceFactors,
	                    productPaymentStatusList);

	        }
	        // Handle packages & components addition of packages
	        List<SellerRelevancePackage> newPackagesToBeAdded = null;
	        if (updatSellerRelevanceFactors.isDirty()) {
	            newPackagesToBeAdded = getSellerPackagesAndComponentsForAddition(
	                    sellerUserId,
	                    rawSeller,
	                    categoryGroupId,
	                    updatSellerRelevanceFactors,
	                    productPaymentStatusList);
	        }
	        // Set scores
	        List<SellerRelevanceFactorScoreMapping> sellerRelevanceFactorScoreMappings = new ArrayList<>();
	        if (updatSellerRelevanceFactors.isDirty()) {
	            sellerRelevanceFactorScoreMappings = sellerRelevanceHelper.setScoresForUserV2(updatSellerRelevanceFactors);
	        }

	        // if seller is no more expert deal maker or
	        // deal maker, delete it's visibility rankings
	        if (categoryGroupId != updatSellerRelevanceFactors.getSellerTransGroupId()
	                || sellerConvertedToDomainExpert(updatSellerRelevanceFactors)) {
	            sellerVisibilityRankingsService.deleteSellerVisibilityRankingsForSellerRelevanceFactorIds(
	                    Collections.singletonList(updatSellerRelevanceFactors.getId()));
	        }

	        // If seller is a deal maker or expert deal
	        // maker get it's visibility rankings
	        List<SellerVisibilityRankings> svr =
	                getSellerVisibilityRankings(updatSellerRelevanceFactors, productPaymentStatusList);
	        saveSellerRelevanceFactorsTransactional(
	                updatSellerRelevanceFactors,
	                newPackagesToBeAdded,
	                sellerRelevanceFactorScoreMappings,
	                svr,
	                saveSellerInfo);
	        updatedSellerRelevanceFactors.add(updatSellerRelevanceFactors);
	    }

	    private boolean sellerConvertedToDomainExpert(SellerRelevanceFactors updatSellerRelevanceFactors) {
	        boolean isDomainExpert = false;
	        if (updatSellerRelevanceFactors.getId() != null) {
	            List<SellerRelevancePackageComponents> sellerRelevancePackageComponents =
	                    sellerRelevancePackageComponentsService.getPackageComponenetsForSellerRelevanceIds(
	                            Collections.singletonList(updatSellerRelevanceFactors.getId()));
	            if (sellerRelevanceService.checkDomainExpert(sellerRelevancePackageComponents)) {
	                isDomainExpert = true;
	            }
	        }

	        return isDomainExpert;
	    }

	    private List<SellerVisibilityRankings> getSellerVisibilityRankings(
	            SellerRelevanceFactors updatSellerRelevanceFactors,
	            List<ProductPaymentStatus> productPaymentStatusList) {
	        List<SellerVisibilityRankings> svr = new ArrayList<>();

	        if (updatSellerRelevanceFactors.getSellerTransGroupId() == SellerTransactionCategory.EXPERT_DEAL_MAKER.getId()
	                || updatSellerRelevanceFactors.getSellerTransGroupId() == SellerTransactionCategory.DEAL_MAKER.getId()
	                || updatSellerRelevanceFactors.getSellerTransGroupId() == SellerTransactionCategory.PREPAID_SELLER
	                        .getId()) {
	            svr = sellerRelevanceService
	                    .computeAndSetVisibilityRankings(updatSellerRelevanceFactors, productPaymentStatusList);
	        }
	        return svr;
	    }

	    private SellerRelevanceFactors computeSRFStatusForSeller(
	            Integer userId,
	            RawSellerDTO rawSeller,
	            List<ProductPaymentStatus> productPaymentStatusList,
	            Integer saleType,
	            SellerRelevanceFactors sellerRelevanceFactors,
	            MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum) {
	        // Sort lead payment list in descending order of date
	        Optional.ofNullable(productPaymentStatusList).ifPresent(
	                l -> l.sort(
	                        (ProductPaymentStatus l1, ProductPaymentStatus l2) -> l2.getPaymentDate()
	                                .compareTo(l1.getPaymentDate())));

	        Date overrideStartDate = sellerRelevanceFactors.getManualOverrideStartDate();
	        Date overrideEndDate = sellerRelevanceFactors.getManualOverrideEndDate();
	        switch (TransactionCategoryGroups.getById(sellerRelevanceFactors.getSellerTransGroupId())) {
	            case EXPERT_DEAL_MAKER: {
	                List<ProductPaymentStatus> newClosedDeals = sellerRelevanceHelper
	                        .getClosedDealsIn(productPaymentStatusList, overrideStartDate, overrideEndDate);
	                if (CollectionUtils.isNotEmpty(newClosedDeals)) {
	                    sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                            userId,
	                            rawSeller,
	                            productPaymentStatusList,
	                            sellerRelevanceFactors,
	                            TransactionCategoryGroups.EXPERT_DEAL_MAKER.getId(),
	                            masterLeadPaymentTypeEnum);
	                }
	                else if (overrideEndDate.compareTo(Calendar.getInstance().getTime()) <= 0) {
	                    validateLeadDeliveryAndUpdateSRF(
	                            userId,
	                            sellerRelevanceFactors,
	                            saleType,
	                            overrideStartDate,
	                            rawSeller);
	                }
	                // clean packages
	                if (!sellerRelevanceFactors.getSellerTransGroupId()
	                        .equals(TransactionCategoryGroups.EXPERT_DEAL_MAKER.getId())) {
	                    sellerRelevanceFactorsService.deletePackageComponents(sellerRelevanceFactors.getId());
	                }
	            }
	                break;
	            case DEAL_MAKER: {
	                List<ProductPaymentStatus> newClosedDeals = sellerRelevanceHelper
	                        .getClosedDealsIn(productPaymentStatusList, overrideStartDate, overrideEndDate);
	                if (CollectionUtils.isNotEmpty(newClosedDeals)) {
	                    sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                            userId,
	                            rawSeller,
	                            productPaymentStatusList,
	                            sellerRelevanceFactors,
	                            getTransGroupIdFromPayments(productPaymentStatusList, rawSeller),
	                            masterLeadPaymentTypeEnum);
	                }
	                else if (overrideEndDate.compareTo(Calendar.getInstance().getTime()) <= 0) {
	                    validateLeadDeliveryAndUpdateSRF(
	                            userId,
	                            sellerRelevanceFactors,
	                            saleType,
	                            overrideStartDate,
	                            rawSeller);
	                }
	            }
	                break;
	            case PREPAID_SELLER: {
	                List<ProductPaymentStatus> newClosedDeals = sellerRelevanceHelper
	                        .getClosedDealsIn(productPaymentStatusList, overrideStartDate, overrideEndDate);
	                Pair<Integer, Integer> dealClosurePair =
	                        sellerRelevanceHelper.getDealClosureCount(productPaymentStatusList);
	                Integer dealsClosedForPrepayments = dealClosurePair.getFirst();
	                if (CollectionUtils.isNotEmpty(newClosedDeals) || dealsClosedForPrepayments > 0) {
	                    sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                            userId,
	                            rawSeller,
	                            productPaymentStatusList,
	                            sellerRelevanceFactors,
	                            getTransGroupIdFromPayments(productPaymentStatusList, rawSeller),
	                            masterLeadPaymentTypeEnum);
	                }
	                else if (overrideEndDate.compareTo(Calendar.getInstance().getTime()) <= 0) {
	                    validateLeadDeliveryAndUpdateSRF(
	                            userId,
	                            sellerRelevanceFactors,
	                            saleType,
	                            overrideStartDate,
	                            rawSeller);
	                }
	            }
	                break;
	            case PREMIUM_SELLER: {
	                List<ProductPaymentStatus> newClosedDeals = sellerRelevanceHelper
	                        .getClosedDealsIn(productPaymentStatusList, overrideStartDate, overrideEndDate);
	                if (CollectionUtils.isNotEmpty(newClosedDeals)) {
	                    sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                            userId,
	                            rawSeller,
	                            productPaymentStatusList,
	                            sellerRelevanceFactors,
	                            getTransGroupIdFromPayments(productPaymentStatusList, rawSeller),
	                            masterLeadPaymentTypeEnum);
	                }
	                else if (overrideEndDate.compareTo(Calendar.getInstance().getTime()) <= 0) {
	                    validateLeadDeliveryAndUpdateSRF(
	                            userId,
	                            sellerRelevanceFactors,
	                            saleType,
	                            overrideStartDate,
	                            rawSeller);
	                }
	            }
	                break;
	            case NEW_AND_REPOST_LISTING_BLOCKED:
	            case FULLY_PENALISED:
	                // Special case - handled outside this function.
	                break;

	            case NOT_RELEVANT:
	            case BOOSTED:
	            case PARTIAL_PENALISED: {
	                Date to = Calendar.getInstance().getTime();
	                Date from = getFromDateForRecentPayments();
	                List<ProductPaymentStatus> newClosedDeals =
	                        sellerRelevanceHelper.getClosedDealsIn(productPaymentStatusList, from, to);
	                if (CollectionUtils.isNotEmpty(newClosedDeals)) {
	                    sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                            userId,
	                            rawSeller,
	                            productPaymentStatusList,
	                            sellerRelevanceFactors,
	                            getTransGroupIdFromPayments(productPaymentStatusList, rawSeller),
	                            masterLeadPaymentTypeEnum);
	                }
	                else if (sellerRelevanceFactors.getCap() != null && sellerRelevanceFactors.getCap() != 0) {
	                    validateLeadDeliveryAndUpdateSRF(
	                            userId,
	                            sellerRelevanceFactors,
	                            saleType,
	                            DateUtil.changeDateBy(
	                                    Calendar.getInstance().getTime(),
	                                    PARTIAL_PENALIZE_LEAD_DELIVERY_DAYS),
	                            rawSeller);
	                }

	            }
	                break;
	            default:
	        }
	        return sellerRelevanceFactors;
	    }

	    private List<SellerRelevancePackage> getSellerPackagesAndComponentsForAddition(
	            Integer sellerUserId,
	            RawSellerDTO rawSeller,
	            Integer oldCategoryGroupId,
	            SellerRelevanceFactors updatSellerRelevanceFactors,
	            List<ProductPaymentStatus> productPaymentStatusList) {
	        logger.error("############## getSellerPackagesAndComponentsForAddition start");
	        List<SellerRelevancePackage> newPackages = new LinkedList<>();
	        if (rawSeller.getCompanyUser().getSellerType().equals(SellerType.Owner)) {
	            if (TransactionCategoryGroups.PREMIUM_SELLER.getId()
	                    .equals(updatSellerRelevanceFactors.getSellerTransGroupId())) {
	                // Add components
	                Date to = updatSellerRelevanceFactors.getManualOverrideEndDate();
	                Date from = updatSellerRelevanceFactors.getManualOverrideStartDate();
	                List<ProductPaymentStatus> newClosedDeals =
	                        sellerRelevanceHelper.getClosedDealsIn(productPaymentStatusList, from, to);
	                Set<Integer> listingIds =
	                        newClosedDeals.stream().map(ProductPaymentStatus::getProductId).collect(Collectors.toSet());
	                logger.error("############## Owner listing Ids" +listingIds);
	                // Remove listings that are already there in DB as components
	                // for seller
	                if (updatSellerRelevanceFactors.getId() != null && CollectionUtils.isNotEmpty(listingIds)) {
	                    Set<Integer> existingListingIds = sellerRelevancePackageComponentsService
	                            .findBySellerRelevanceIdAndAttributeIdAndAttributeValuesIn(
	                                    updatSellerRelevanceFactors.getId(),
	                                    MasterSellerRelevancePackageTypeEnum.LISTING.getId(),
	                                    listingIds)
	                            .stream().map(SellerRelevancePackageComponents::getAttributeValues)
	                            .collect(Collectors.toSet());
	                    listingIds.removeAll(existingListingIds);
	                }
	                logger.error("############## Final Owner listing Ids" +listingIds);
	                for (Integer listingId : listingIds) {
	                    SellerRelevancePackageComponents component = new SellerRelevancePackageComponents();
	                    component.setAttributeId(MasterSellerRelevancePackageTypeEnum.LISTING.getId());
	                    component.setAttributeValues(listingId);

	                    SellerRelevancePackage sellerPackage = new SellerRelevancePackage();
	                    sellerPackage.setPackageTypeId(MasterSellerRelevancePackageTypeEnum.LISTING.getId());
	                    sellerPackage.setPackageStartDate(Calendar.getInstance().getTime());
	                    sellerPackage.setSellerRelevancePackageComponents(new HashSet<>(Arrays.asList(component)));

	                    newPackages.add(sellerPackage);
	                }
	            }
	        }
	        return newPackages;
	    }

	    private void deleteStalePackageAndComponents(
	            Integer sellerUserId,
	            RawSellerDTO rawSeller,
	            Integer oldCategoryGroupId,
	            SellerRelevanceFactors updatSellerRelevanceFactors,
	            List<ProductPaymentStatus> productPaymentStatusList) {
	        if (rawSeller.getCompanyUser().getSellerType().equals(SellerType.Owner)) {
	            if (!TransactionCategoryGroups.PREMIUM_SELLER.getId()
	                    .equals(updatSellerRelevanceFactors.getSellerTransGroupId())) {
	                // Delete all Components
	                sellerRelevanceFactorsService.deletePackageComponents(updatSellerRelevanceFactors.getId());
	            }
	        }
	        else {
	            if ((oldCategoryGroupId == null || sellerRelevanceHelper.isSellerStatusPaid(oldCategoryGroupId))
	                    && !sellerRelevanceHelper.isSellerStatusPaid(updatSellerRelevanceFactors.getSellerTransGroupId())) {
	                // Delete all Components
	                sellerRelevanceFactorsService.deletePackageComponents(updatSellerRelevanceFactors.getId());
	            }
	        }
	    }

	    private void validateLeadDeliveryAndUpdateSRF(
	            Integer userId,
	            SellerRelevanceFactors sellerRelevanceFactors,
	            Integer saleType,
	            Date leadDeliveryFrom,
	            RawSellerDTO rawSeller) {
	        Integer leadCount = icrmHelper.getLeadCountBySaleType(
// Divyanshu	          rawSellerService.getRawSellerFromSolrViaSellerUserId(sellerRelevanceFactors.getSellerId())
	        			midlServiceHelper.getRawSellerFromSolrViaSellerUserId(sellerRelevanceFactors.getSellerId())
	        		.getCompanyUser().getCompanyId(),
	                leadDeliveryFrom,
	                saleType);
	        if (leadCount < sellerRelevanceFactors.getCap()
	                && (sellerRelevanceFactors.getManualOverrideEndDate() != null && sellerRelevanceFactors
	                        .getManualOverrideEndDate().compareTo(Calendar.getInstance().getTime()) <= 0)) {
	            sellerRelevanceFactors
	                    .setManualOverrideEndDate(DateUtil.changeDateBy(null, DEAL_MAKER_EXPIRATION_DAY_COUNT_INC));

	        }
	        else if (leadCount >= sellerRelevanceFactors.getCap()) {
	            sellerRelevanceHelper.resetToNonPaidSeller(sellerRelevanceFactors, rawSeller);
	        }

	    }

	    private Integer getTransGroupIdFromPayments(
	            List<ProductPaymentStatus> productPaymentStatusList,
	            RawSellerDTO rawSeller) {
	        Integer updatedTransactionGroupId = null;
	        Pair<Integer, Integer> dealClosurePair = sellerRelevanceHelper.getDealClosureCount(productPaymentStatusList);
	        if (rawSeller.getCompanyUser().getSellerType().equals(SellerType.Owner)) {
	            updatedTransactionGroupId = dealClosurePair.getFirst() + dealClosurePair.getSecond() > 0
	                    ? TransactionCategoryGroups.PREMIUM_SELLER.getId()
	                    : null;
	        }
	        else {
	            updatedTransactionGroupId = dealClosurePair.getFirst() > 0
	                    ? TransactionCategoryGroups.DEAL_MAKER.getId()
	                    : TransactionCategoryGroups.PREPAID_SELLER.getId();
	        }
	        return updatedTransactionGroupId;
	    }

	    /**
	     * 
	     * @param sellerUserId
	     * @return
	     */
	    public Map<Integer, Map<Integer, SellerRelevanceFactors>> getSellerRelevanceFactors(Integer sellerUserId) {
	        long totalCount = -1;
	        int start = 0;
	        List<SellerRelevanceFactors> sellerRelevanceFactorList = new ArrayList<>();
	        FIQLSelector selector = new FIQLSelector();
	        if (sellerUserId != null) {
	            selector.addAndConditionToFilter("sellerId", sellerUserId);
	        }
	        selector.setRows(ROWS);
	        do {
	            PaginatedResponse<List<SellerRelevanceFactors>> sellerRelevanceFactorListTmp =
	                    sellerRelevanceFactorsService.getSellerRelevanceFactors(selector);
	            totalCount = sellerRelevanceFactorListTmp.getTotalCount();
	            if (CollectionUtils.isEmpty(sellerRelevanceFactorListTmp.getResults())) {
	                break;
	            }
	            sellerRelevanceFactorList.addAll(sellerRelevanceFactorListTmp.getResults());
	            start += ROWS;
	            selector.setStart(start);
	        }
	        while (start < totalCount);

	        return sellerRelevanceFactorList.stream().collect(
	                Collectors.groupingBy(
	                        SellerRelevanceFactors::getSellerId,
	                        Collectors.toMap(SellerRelevanceFactors::getSaleTypeId, Function.identity())));
	    }

	    private SellerRelevanceFactors saveSellerRelevanceFactorsTransactional(
	            SellerRelevanceFactors sellerRelevanceFactor,
	            List<SellerRelevancePackage> newPackagesToBeAdded,
	            List<SellerRelevanceFactorScoreMapping> srfScoreMappings,
	            List<SellerVisibilityRankings> srfVisibilityRankings,
	            boolean saveSellerInfo) {
	        return applicationContext.getBean(SellerRelevanceCronService.class).saveSellerRelevanceFactors(
	                sellerRelevanceFactor,
	                newPackagesToBeAdded,
	                srfScoreMappings,
	                srfVisibilityRankings,
	                saveSellerInfo);
	    }

	    @Transactional
	    public SellerRelevanceFactors saveSellerRelevanceFactors(
	            SellerRelevanceFactors sellerRelevanceFactor,
	            List<SellerRelevancePackage> newPackagesToBeAdded,
	            List<SellerRelevanceFactorScoreMapping> srfScoreMappings,
	            List<SellerVisibilityRankings> srfVisibilityRankings,
	            boolean saveSellerInfo) {
	        if (saveSellerInfo) {
	            sellerRelevanceFactor = sellerRelevanceFactorsDao.save(sellerRelevanceFactor);
	            if (CollectionUtils.isNotEmpty(srfScoreMappings)) {
	                sellerRelevanceFactorsService.saveSellerRelevanceScores(srfScoreMappings, sellerRelevanceFactor);
	            }
	            if (CollectionUtils.isNotEmpty(newPackagesToBeAdded)) {
	                sellerRelevanceFactorsService
	                        .addPackagesAndComponents(newPackagesToBeAdded, sellerRelevanceFactor.getId());
	            }
	            if (CollectionUtils.isNotEmpty(srfVisibilityRankings)) {
	                sellerVisibilityRankingsService
	                        .updateSellerVisibilityRankings(sellerRelevanceFactor, srfVisibilityRankings);
	            }

	        }
	        return sellerRelevanceFactor;
	    }

	    private Map<String, Object> responseBuilder(
	            Collection<SellerRelevanceFactors> updatedBuySellerRelevanceFactors,
	            Set<Integer> failedSellerIds) {
	        Map<Integer, Map<Integer, List<SellerRelevanceFactors>>> sf = updatedBuySellerRelevanceFactors.stream().collect(
	                Collectors.groupingBy(
	                        SellerRelevanceFactors::getSaleTypeId,
	                        Collectors.groupingBy(SellerRelevanceFactors::getSellerTransGroupId)));
	        Map<String, Object> responseMap = new HashMap<>();
	        for (MasterLeadPaymentTypeEnum masterLeadPaymentTypeEnum : MasterLeadPaymentTypeEnum.values()) {
	            responseMap.put(
	                    "For " + masterLeadPaymentTypeEnum.name(),
	                    getFinalMap(sf.get(masterLeadPaymentTypeEnum.getId())));
	        }
	        responseMap.put("Failed Seller Ids", failedSellerIds);
	        return responseMap;
	    }

	    private Map<String, List<Integer>> getFinalMap(Map<Integer, List<SellerRelevanceFactors>> sellerIdList) {
	        Map<String, List<Integer>> catSellerIdMapBuy = new HashMap<>();
	        if (MapUtils.isNotEmpty(sellerIdList)) {
	            sellerIdList.keySet().stream().forEach(catId -> {
	                List<SellerRelevanceFactors> srf = sellerIdList.get(catId);
	                List<Integer> srfSellerList =
	                        srf.stream().map(SellerRelevanceFactors::getSellerId).collect(Collectors.toList());
	                String catName = TransactionCategoryGroups.getById(catId).getLabel();
	                catSellerIdMapBuy.put("Sellers converted to " + catName, srfSellerList);
	            });
	        }
	        return catSellerIdMapBuy;
	    }

	    private void validateAndRemoveSuspension(
	            Integer sellerUserId,
	            Map<Integer, Map<Integer, List<ProductPaymentStatus>>> crmPaymentMap,
	            RawSellerDTO rawSeller) {
	        Map<Integer, List<ProductPaymentStatus>> paymentsMap = crmPaymentMap.get(sellerUserId);
	        if (!suspensionPredicate.test(rawSeller) || MapUtils.isEmpty(paymentsMap)) {
	            return;
	        }
	        List<ProductPaymentStatus> buyLeadPaymentsList = paymentsMap.get(MasterLeadPaymentTypeEnum.Buy.getId());
	        List<ProductPaymentStatus> rentLeadPaymentsList = paymentsMap.get(MasterLeadPaymentTypeEnum.Rent.getId());

	        Date fromDate = DateUtil
	                .changeDateBy(rawSeller.getSuspensionDate(), -1 * SUSPENSION_REMOVAL_PREVIOUS_PAYMENTS_DAY_COUNT);

	        List<ProductPaymentStatus> buyClosedDeals =
	                sellerRelevanceHelper.getClosedDealsIn(buyLeadPaymentsList, fromDate, Calendar.getInstance().getTime());
	        List<ProductPaymentStatus> rentClosedDeals = sellerRelevanceHelper
	                .getClosedDealsIn(rentLeadPaymentsList, fromDate, Calendar.getInstance().getTime());

	        boolean isBuyPaymentDone = buyClosedDeals.size() >= BUY_NON_PENALIZE_COUNT ? true : false;
	        boolean isRentPaymentDone = rentClosedDeals.size() >= RENT_NON_PENALIZE_COUNT ? true : false;
	        // suspension
	        if (isBuyPaymentDone || isRentPaymentDone) {
	            checkAndRemoveSuspension(rawSeller);
	        }
	    }

	    private void processSRFStatusForSellersWithForSpecialCase(
	            Integer sellerUserId,
	            Map<Integer, Map<Integer, List<ProductPaymentStatus>>> crmPaymentMap,
	            Map<Integer, SellerRelevanceFactors> sellerRevelanceFactorMaps,
	            RawSellerDTO rawSeller,
	            List<SellerRelevanceFactors> updatedSellerRelevanceFactors,
	            boolean saveSellerInfo) {
	        Map<Integer, List<ProductPaymentStatus>> paymentsMap = crmPaymentMap.get(sellerUserId);
	        if (MapUtils.isEmpty(paymentsMap)) {
	            return;
	        }
	        SellerRelevanceFactors sellerRelevanceFactors =
	                sellerRevelanceFactorMaps.entrySet().iterator().next().getValue();
	        Map<Integer, Integer> minPaymentCount = getMinPaidDealCountForSpecialCaseSellers(sellerRelevanceFactors);

	        List<ProductPaymentStatus> buyLeadPaymentsList = paymentsMap.get(MasterLeadPaymentTypeEnum.Buy.getId());
	        List<ProductPaymentStatus> rentLeadPaymentsList = paymentsMap.get(MasterLeadPaymentTypeEnum.Rent.getId());

	        Date fromDate = getFullyPenalizedPaymentsFromDate(rawSeller, sellerRevelanceFactorMaps);
	        List<ProductPaymentStatus> buyClosedDeals =
	                sellerRelevanceHelper.getClosedDealsIn(buyLeadPaymentsList, fromDate, Calendar.getInstance().getTime());
	        List<ProductPaymentStatus> rentClosedDeals = sellerRelevanceHelper
	                .getClosedDealsIn(rentLeadPaymentsList, fromDate, Calendar.getInstance().getTime());

	        boolean isBuyPaymentDone =
	                buyClosedDeals.size() >= minPaymentCount.get(MasterLeadPaymentTypeEnum.Buy.getId()) ? true : false;
	        boolean isRentPaymentDone =
	                rentClosedDeals.size() >= minPaymentCount.get(MasterLeadPaymentTypeEnum.Rent.getId()) ? true : false;

	        SellerRelevanceFactors buySellerRelevanceFactors =
	                sellerRevelanceFactorMaps.get(MasterLeadPaymentTypeEnum.Buy.getId()) != null
	                        ? sellerRevelanceFactorMaps.get(MasterLeadPaymentTypeEnum.Buy.getId())
	                        : new SellerRelevanceFactors(
	                                sellerUserId,
	                                MasterLeadPaymentTypeEnum.Buy.getId(),
	                                sellerRelevanceFactors.getTransactionCategoryGroup().getId());
	        SellerRelevanceFactors rentSellerRelevanceFactors =
	                sellerRevelanceFactorMaps.get(MasterLeadPaymentTypeEnum.Rent.getId()) != null
	                        ? sellerRevelanceFactorMaps.get(MasterLeadPaymentTypeEnum.Rent.getId())
	                        : new SellerRelevanceFactors(
	                                sellerUserId,
	                                MasterLeadPaymentTypeEnum.Rent.getId(),
	                                sellerRelevanceFactors.getTransactionCategoryGroup().getId());
	        if (isBuyPaymentDone && isRentPaymentDone) {
	            sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                    sellerUserId,
	                    rawSeller,
	                    buyLeadPaymentsList,
	                    buySellerRelevanceFactors,
	                    getTransGroupIdFromPayments(buyClosedDeals, rawSeller),
	                    MasterLeadPaymentTypeEnum.Buy);
	            sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                    sellerUserId,
	                    rawSeller,
	                    rentLeadPaymentsList,
	                    rentSellerRelevanceFactors,
	                    getTransGroupIdFromPayments(rentClosedDeals, rawSeller),
	                    MasterLeadPaymentTypeEnum.Rent);
	        }
	        else if (isBuyPaymentDone) {
	            sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                    sellerUserId,
	                    rawSeller,
	                    buyLeadPaymentsList,
	                    buySellerRelevanceFactors,
	                    getTransGroupIdFromPayments(buyClosedDeals, rawSeller),
	                    MasterLeadPaymentTypeEnum.Buy);
	            sellerRelevanceHelper.resetToNonPaidSeller(rentSellerRelevanceFactors, rawSeller);
	        }
	        else if (isRentPaymentDone) {
	            sellerRelevanceHelper.createOrUpdateSRFForNewPaymentsV2(
	                    sellerUserId,
	                    rawSeller,
	                    rentLeadPaymentsList,
	                    rentSellerRelevanceFactors,
	                    getTransGroupIdFromPayments(rentClosedDeals, rawSeller),
	                    MasterLeadPaymentTypeEnum.Rent);
	            sellerRelevanceHelper.resetToNonPaidSeller(buySellerRelevanceFactors, rawSeller);

	        }

	        // Set scores and save for buy
	        if (buySellerRelevanceFactors.isDirty()) {
	            saveSellerRelevanceFactorsTransactional(
	                    buySellerRelevanceFactors,
	                    null,
	                    sellerRelevanceHelper.setScoresForUserV2(buySellerRelevanceFactors),
	                    Collections.emptyList(),
	                    saveSellerInfo);
	            updatedSellerRelevanceFactors.add(buySellerRelevanceFactors);
	        }
	        // Set scores and save for rent
	        if (rentSellerRelevanceFactors.isDirty()) {
	            saveSellerRelevanceFactorsTransactional(
	                    rentSellerRelevanceFactors,
	                    null,
	                    sellerRelevanceHelper.setScoresForUserV2(rentSellerRelevanceFactors),
	                    Collections.emptyList(),
	                    saveSellerInfo);
	            updatedSellerRelevanceFactors.add(rentSellerRelevanceFactors);
	        }
	        if (crmPaymentMap.get(sellerUserId) != null) {
	            crmPaymentMap.get(sellerUserId).remove(MasterLeadPaymentTypeEnum.Buy.getId());
	            crmPaymentMap.get(sellerUserId).remove(MasterLeadPaymentTypeEnum.Rent.getId());
	        }
	    }

	    private Map<Integer, Integer> getMinPaidDealCountForSpecialCaseSellers(
	            SellerRelevanceFactors sellerRelevanceFactors) {
	        Map<Integer, Integer> minPaymentCount = new HashMap<>();
	        switch (TransactionCategoryGroups.getById(sellerRelevanceFactors.getSellerTransGroupId())) {
	            case FULLY_PENALISED:
	                minPaymentCount.put(MasterLeadPaymentTypeEnum.Buy.getId(), BUY_NON_PENALIZE_COUNT);
	                minPaymentCount.put(MasterLeadPaymentTypeEnum.Rent.getId(), RENT_NON_PENALIZE_COUNT);
	                break;
	            case ACCOUNT_LOCKED:
	                minPaymentCount.put(MasterLeadPaymentTypeEnum.Buy.getId(), BUY_ACCOUNT_LOCKED_COUNT);
	                minPaymentCount.put(MasterLeadPaymentTypeEnum.Rent.getId(), RENT_ACCOUNT_LOCKED_COUNT);
	                break;
	            default:
	                minPaymentCount.put(MasterLeadPaymentTypeEnum.Buy.getId(), BUY_NON_PENALIZE_COUNT);
	                minPaymentCount.put(MasterLeadPaymentTypeEnum.Rent.getId(), RENT_NON_PENALIZE_COUNT);
	        }
	        return minPaymentCount;
	    }

	    private Date getFullyPenalizedPaymentsFromDate(
	            RawSellerDTO rawSeller,
	            Map<Integer, SellerRelevanceFactors> sellerRevelanceFactorMap) {
	        Date date = null;

	        if (suspensionPredicate.test(rawSeller)) {
	            date = DateUtil
	                    .changeDateBy(rawSeller.getSuspensionDate(), -1 * SUSPENSION_REMOVAL_PREVIOUS_PAYMENTS_DAY_COUNT);
	        }
	        else {
	            Optional<Entry<Integer, SellerRelevanceFactors>> sellerRelevanceFactorsOpt =
	                    sellerRevelanceFactorMap.entrySet().stream()
	                            .filter(
	                                    s -> s.getValue().getSellerTransGroupId()
	                                            .equals(TransactionCategoryGroups.FULLY_PENALISED.getId()))
	                            .findFirst();
	            if (sellerRelevanceFactorsOpt.isPresent()) {
	                date = sellerRelevanceFactorsOpt.get().getValue().getUpdatedAt();
	            }
	            else {
	                date = DateUtil.changeDateBy(Calendar.getInstance().getTime(), NON_PENALIZE_PREVIOUS_DAY_COUNT);
	            }
	        }
	        return date;
	    }

	    private boolean checkAndRemoveSuspension(RawSellerDTO rawSeller) {
	        if (rawSeller != null && rawSeller.getSuspensionStatus().equals(SellerSuspensionStatus.Suspended)
	                || rawSeller.getSuspensionStatus().equals(SellerSuspensionStatus.ToBeSuspended)) {
	            List<RawSellerDTO> savedRawSeller = new ArrayList<>();
	            try {
//Divyanshu	                savedRawSeller = rawSellerService.removeSuspensionStatusOfSeller(Collections.singletonList(rawSeller));
	            		savedRawSeller = midlServiceHelper.removeSuspensionStatusOfSeller(Collections.singletonList(rawSeller));
	            	if (CollectionUtils.isNotEmpty(savedRawSeller)) {
	//                    rawSellerService.runSelleIndexingViaSellerUserIds(
	            		midlServiceHelper.runSelleIndexingViaSellerUserIds(
	                            savedRawSeller.stream().map(RawSellerDTO::getSellerUserId).collect(Collectors.toSet()),
	                            Domain.Makaan);
	//                    rawSellerService.publishRawSellerUpdateEventsToSns(
	            		midlServiceHelper.publishRawSellerUpdateEventsToSns(
	                            savedRawSeller.stream().map(RawSellerDTO::getId).collect(Collectors.toList()));
	                    return true;
	                }
	            }
	            catch (Exception e) {
	                logger.error("Could not remove suspension ", e);
	            }

	        }
	        return false;
	    }

	    private Date getFromDateForRecentPayments() {
	        return new Date(new DateTime().minusDays(2).withTimeAtStartOfDay().getMillis());
	    }

}
