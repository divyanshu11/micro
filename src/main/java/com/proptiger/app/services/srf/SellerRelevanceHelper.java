package com.proptiger.app.services.srf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

//import com.proptiger.app.model.transaction.ProductPaymentStatus;
import com.proptiger.app.repo.cms.MasterSellerScoreTypesDao;
import com.proptiger.app.repo.srf.SellerRelevanceFactorsDao;
import com.proptiger.app.repo.srf.SellerRelevancePackageDao;
import com.proptiger.app.service.order.ProductPaymentStatusService;
import com.proptiger.core.dto.cms.score.CompanyRelevantScoresDTO;
import com.proptiger.core.enums.MobileApplication;
import com.proptiger.core.enums.external.mbridge.SellerSuspensionStatus;
import com.proptiger.core.enums.notification.MediumType;
import com.proptiger.core.enums.notification.NotificationTypeEnum;
import com.proptiger.core.enums.notification.Tokens;
import com.proptiger.core.helper.ICRMHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.helper.MadelyneServiceHelper;
import com.proptiger.core.helper.UserServiceHelper;
import com.proptiger.core.internal.dto.mail.DefaultMediumDetails;
import com.proptiger.core.internal.dto.mail.MediumDetails;
import com.proptiger.core.model.cms.Listing;
import com.proptiger.core.model.cms.MasterSellerScoreTypes;
import com.proptiger.core.model.cms.SellerRelevanceFactorScoreMapping;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.SellerRelevancePackage;
import com.proptiger.core.model.cms.SellerVisibilityRankings;
import com.proptiger.core.model.cms.MasterSellerRelevancePackageType.MasterSellerRelevancePackageTypeEnum;
import com.proptiger.core.model.cms.MasterSellerVisibilityAttributes.VisibilityAttributes;
import com.proptiger.core.model.cms.MasterSellerVisibiltyRanks.VisibilityRanks;
import com.proptiger.core.model.cms.MasterTransactionCategoryGroup.SellerRelevanceTransactionStatuses;
import com.proptiger.core.model.cms.MasterTransactionCategoryGroup.TransactionCategoryGroups;
import com.proptiger.core.model.cms.RawSellerDTO;
import com.proptiger.core.model.cms.SellerTransactionCategories.CategoryType;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentTypeEnum;
import com.proptiger.core.model.notification.external.NotificationCreatorServiceRequest;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
//import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.model.user.GCMUser;
import com.proptiger.core.pojo.Pair;
import com.proptiger.core.service.ConfigService;
import com.proptiger.core.service.ConfigService.ConfigGroupName;
import com.proptiger.core.util.DateUtil;
//import com.proptiger.core.util.DateUtil;
import com.proptiger.core.util.UtilityClass;

//CompanyRelevantScoresDTO used in ListingIndexingCacheService,ListingIndexingProcessorService

//Not used now..
//import com.proptiger.data.dto.cms.CompanyRelevantScoresDTO;

//import com.proptiger.data.repo.cms.MasterSellerScoreTypesDao;
//import com.proptiger.data.repo.srf.SellerRelevanceFactorsDao;
//import com.proptiger.data.repo.srf.SellerRelevancePackageDao;

//import com.proptiger.data.service.order.ProductPaymentStatusService;

//import com.proptiger.data.service.srf.SellerRelevanceHelper;
//petra
//import com.proptiger.marketforce.data.model.RawSeller;
//import com.proptiger.marketforce.data.model.RawSeller.SellerSuspensionStatus;
//import com.proptiger.marketforce.data.model.RawSeller.SellerType;
//import com.proptiger.marketforce.data.service.RawSellerService;

@Service
public class SellerRelevanceHelper {
	
	//divyanshu.....
	@Autowired
	private MIDLServiceHelper midlServiceHelper;

	@Autowired
    private MasterSellerScoreTypesDao                     masterSellerScoreTypesDao;

    @Autowired
    private ProductPaymentStatusService                   productPaymentService;

    @Autowired
    private ConfigService                                 configService;

    private static final Comparator<ProductPaymentStatus> LEAD_PAYMENT_STATUS_ORDERING_DESC    =
            (ProductPaymentStatus l1, ProductPaymentStatus l2) -> l2.getPaymentDate().compareTo(l1.getPaymentDate());

    @Autowired
    private UserServiceHelper                             userServiceHelper;

    @Autowired
    private MadelyneServiceHelper                         notificationCreatorService;

    @Autowired
    private SellerRelevanceFactorsDao                     sellerRelevanceFactorsDao;

    @Autowired
    private SellerRelevancePackageDao                     sellerRelevancePackageDao;

    @Autowired
    private ICRMHelper                                    icrmHelper;

    // DO NOT AUTOWIRE
    //divyanshu
    //private RawSellerService                              rawSellerService;

    @Autowired
    ApplicationContext                                    applicationContext;

    private static Logger                                 logger                               =
            LoggerFactory.getLogger(SellerRelevanceHelper.class);

    @Value("${seller.partial.penalize.lead.delivery.days}")
    private int                                           PARTIAL_PENALIZE_LEAD_DELIVERY_DAYS;

    private static final SimpleDateFormat                 DATE_FORMATTER                       =
            new SimpleDateFormat("yyyy-MM-dd");
    private static Map<String, Integer>                   SELLER_STATUS_PRIORITY_MAP           =
            ImmutableMap.<String, Integer> builder().put(SellerSuspensionStatus.Suspended.name(), 1)
                    .put(SellerSuspensionStatus.ToBeSuspended.name(), 2)
                    .put(TransactionCategoryGroups.FULLY_PENALISED.getLabel(), 3)
                    .put(TransactionCategoryGroups.ACCOUNT_LOCKED.getLabel(), 4)
                    .put(SellerRelevanceTransactionStatuses.CITY_EXPERT_DEAL_MAKER.name(), 5)
                    .put(SellerRelevanceTransactionStatuses.LOCALITY_EXPERT_DEAL_MAKER.name(), 6)
                    .put(TransactionCategoryGroups.EXPERT_DEAL_MAKER.getLabel(), 7)
                    .put(TransactionCategoryGroups.DEAL_MAKER.getLabel(), 8)
                    .put(TransactionCategoryGroups.PREPAID_SELLER.getLabel(), 9)
                    .put(TransactionCategoryGroups.PARTIAL_PENALISED.getLabel(), 10)
                    .put(TransactionCategoryGroups.BOOSTED.getLabel(), 11)
                    .put(TransactionCategoryGroups.NOT_RELEVANT.getLabel(), 12)
                    .put(TransactionCategoryGroups.NEW_AND_REPOST_LISTING_BLOCKED.getLabel(), 13).build();

    private static final Float                            CITY_EXPERT_LOCALITY_SCORE           = 10f;
    private static final Float                            CITY_EXPERT_CITY_SCORE               = 8f;
    private static final Float                            LOCALITY_EXPERT_LOCALITY_SCORE       = 9f;
    private static final Float                            EXPERT_DEFAULT_SCORE                 = 7f;
    private static final Float                            DEAL_MAKER_DEFAULT_SCORE             = 5f;
    private static final Float                            PREPAID_SELLER_DEFAULT_SCORE         = 2.5f;
    private static final Float                            LISTING_PREMIUM_SELLER_DEFAULT_SCORE = 1.5f;
    private static final Float                            PREMIUM_SELLER_DEFAULT_SCORE         = 0f;

    private static final Predicate<Integer>               IS_SELLER_STATUS_PAID_PREDICATE      =
            (Integer categoryId) -> {
                                                                                                           return TransactionCategoryGroups.EXPERT_DEAL_MAKER
                                                                                                                   .getId()
                                                                                                                   .equals(
                                                                                                                           categoryId)
                                                                                                                   || TransactionCategoryGroups.DEAL_MAKER
                                                                                                                           .getId()
                                                                                                                           .equals(
                                                                                                                                   categoryId)
                                                                                                                   || TransactionCategoryGroups.PREPAID_SELLER
                                                                                                                           .getId()
                                                                                                                           .equals(
                                                                                                                                   categoryId);
                                                                                                       };

    @PostConstruct
    public void init() {
//        rawSellerService = applicationContext.getBean(RawSellerService.class);
    }

    public String getFormattedDate(Integer days) {
        Date date = getDateTime(days);
        return DATE_FORMATTER.format(date);
    }

    public Date getDateTime(Integer addDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, addDays);
        return cal.getTime();
    }

    public boolean isSellerStatusPaid(Integer categoryId) {
        return IS_SELLER_STATUS_PAID_PREDICATE.test(categoryId);
    }

    public List<SellerRelevanceFactorScoreMapping> setScoresForUserV2(SellerRelevanceFactors sellerRelevanceFactors) {
        List<SellerRelevanceFactorScoreMapping> sellerRelevanceFactorScoreMappings = new ArrayList<>();
        switch (TransactionCategoryGroups.getById(sellerRelevanceFactors.getSellerTransGroupId())) {
            case FULLY_PENALISED:
                setScores(
                        "seller_transaction_reveal_score",
                        -1000F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;
            case NEW_AND_REPOST_LISTING_BLOCKED:
                setScores(
                        "seller_transaction_reveal_score",
                        -1000F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;
            case NOT_RELEVANT:
                setScores(
                        "seller_transaction_reveal_score",
                        0F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;
            case BOOSTED:
                setScores("top_seller_priority", 10F, sellerRelevanceFactorScoreMappings, sellerRelevanceFactors);
                break;
            case EXPERT_DEAL_MAKER:
                setScores(
                        "seller_transaction_reveal_score",
                        7F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;
            case DEAL_MAKER:
                setScores(
                        "seller_transaction_reveal_score",
                        5F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;
            case PREPAID_SELLER:
                setScores(
                        "seller_transaction_reveal_score",
                        2.5F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;
            case PARTIAL_PENALISED:
                if (null != sellerRelevanceFactors.getCap() && sellerRelevanceFactors.getCap() != 0) {
                    Date partialPenalizeDate = getDateTime(PARTIAL_PENALIZE_LEAD_DELIVERY_DAYS);
                    Integer leadCount = icrmHelper.getLeadCountBySaleType(
   //divyanshu            //       rawSellerService.getRawSellerFromSolrViaSellerUserId(sellerRelevanceFactors.getSellerId())
                     //               .getCompanyId(),
                                   midlServiceHelper.getRawSellerFromSolrViaSellerUserId(sellerRelevanceFactors.getSellerId()).getCompanyUser().getCompanyId(),
                            partialPenalizeDate,
                            sellerRelevanceFactors.getSaleTypeId());
                    if (leadCount > sellerRelevanceFactors.getCap()) {
                        setScores(
                                "seller_transaction_reveal_score",
                                -1000F,
                                sellerRelevanceFactorScoreMappings,
                                sellerRelevanceFactors);
                    }
                    else {
                        setScores(
                                "seller_transaction_reveal_score",
                                0F,
                                sellerRelevanceFactorScoreMappings,
                                sellerRelevanceFactors);
                    }
                }
                else {
                    setScores(
                            "seller_transaction_reveal_score",
                            -1000F,
                            sellerRelevanceFactorScoreMappings,
                            sellerRelevanceFactors);
                }

                break;
            case ACCOUNT_LOCKED:
                setScores(
                        "seller_transaction_reveal_score",
                        -100F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;
            case PREMIUM_SELLER:
                setScores(
                        "seller_transaction_reveal_score",
                        0F,
                        sellerRelevanceFactorScoreMappings,
                        sellerRelevanceFactors);
                break;

        }
        return sellerRelevanceFactorScoreMappings;
    }

    // TODO - cacheable
    private Map<String, Integer> getMasterSellerScoreMap() {
        Map<String, Integer> sellerScoreTypeIdNameMap = new HashMap<>();
        List<MasterSellerScoreTypes> masterSellerScoreTypeList = masterSellerScoreTypesDao.findAll();
        for (MasterSellerScoreTypes masterSellerScoreTypes : masterSellerScoreTypeList) {
            Integer scoreId = masterSellerScoreTypes.getId();
            String scoreName = masterSellerScoreTypes.getScoreName();
            sellerScoreTypeIdNameMap.put(scoreName, scoreId);
        }
        return sellerScoreTypeIdNameMap;
    }

    private void setScores(
            String scoreName,
            Float scoreValue,
            List<SellerRelevanceFactorScoreMapping> sellerRelevanceFactorScoreMappings,
            SellerRelevanceFactors sellerRelevanceFactors) {

        Integer scoreId = getMasterSellerScoreMap().get(scoreName);

        SellerRelevanceFactorScoreMapping factorScoreMapping = new SellerRelevanceFactorScoreMapping();
        factorScoreMapping.setScoreId(scoreId);
        factorScoreMapping.setScoreValue(scoreValue);
        sellerRelevanceFactorScoreMappings.add(factorScoreMapping);
    }

    public Map<Integer, Map<Integer, List<ProductPaymentStatus>>> getSellerPaymentMap(
            String fromDate,
            Integer sellerId,
            Integer saleTypeId) {
        List<ProductPaymentStatus> productPaymentStatusList =
                productPaymentService.getLeadPaymentListFromDate(fromDate, sellerId, null);
        logger.error("############## no of payments received: " + sellerId + " " + productPaymentStatusList.size());
        logger.error(
                "############## product ids owner: " + productPaymentStatusList.stream()
                        .map(ProductPaymentStatus::getProductId).collect(Collectors.toList()));
        return productPaymentStatusList.stream().collect(
                Collectors.groupingBy(
                        ProductPaymentStatus::getCrmUserId,
                        Collectors.groupingBy(ProductPaymentStatus::getSaleTypeId)));
    }

    public List<ProductPaymentStatus> getClosedDealsIn(
            List<ProductPaymentStatus> productPaymentStatuses,
            Date from,
            Date to) {
        List<ProductPaymentStatus> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(productPaymentStatuses)) {
            productPaymentStatuses.sort(LEAD_PAYMENT_STATUS_ORDERING_DESC);
            for (ProductPaymentStatus lps : productPaymentStatuses) {
                if ((lps.getPaymentDate().compareTo(to) <= 0 && lps.getPaymentDate().compareTo(from) >= 0)) {
                    result.add(lps);
                }
            }
        }
        return result;
    }

    public SellerRelevanceFactors resetToNonPaidSeller(
            SellerRelevanceFactors sellerRelevanceFactors,
            RawSellerDTO rawSeller) {
        sellerRelevanceFactors.setManualOverriden(false);
        if (rawSeller.getCompanyUser().getSellerType().equals(com.proptiger.core.enums.external.mbridge.SellerType.Owner)) {
            sellerRelevanceFactors.setSellerTransGroupId(TransactionCategoryGroups.NOT_RELEVANT.getId());
        }
        else {
            sellerRelevanceFactors.setSellerTransGroupId(TransactionCategoryGroups.PARTIAL_PENALISED.getId());
        }
        sellerRelevanceFactors.setManualOverrideStartDate(null);
        sellerRelevanceFactors.setManualOverrideEndDate(null);
        sellerRelevanceFactors.setCap(null);
        return sellerRelevanceFactors;
    }

    public void createOrUpdateSRFForNewPayments(
            Integer sellerUserId,
            //dto is used instead of rawseller (divyanshu)
            RawSellerDTO rawSeller,
            List<ProductPaymentStatus> productPaymentStatusList,
            SellerRelevanceFactors sellerRelevanceFactors,
            Integer updatedTransactionGroupId,
            MasterLeadPaymentTypeEnum saleType) {
        if (CollectionUtils.isEmpty(productPaymentStatusList) || sellerRelevanceFactors == null)
            return;
        Pair<Date, Date> durationPair =
                calculateDuration(rawSeller, productPaymentStatusList, updatedTransactionGroupId, saleType);
        sellerRelevanceFactors.setSellerTransGroupId(updatedTransactionGroupId);
        sellerRelevanceFactors.setManualOverrideStartDate(durationPair.getFirst());
        sellerRelevanceFactors.setManualOverrideEndDate(durationPair.getSecond());
        sellerRelevanceFactors.setCap(calculateCap(rawSeller, productPaymentStatusList, saleType));
    }

    private Pair<Date, Date> calculateDuration(
            RawSellerDTO rawSeller,
            List<ProductPaymentStatus> productPaymentStatusList,
            Integer updatedTransactionGroupId,
            MasterLeadPaymentTypeEnum saleType) {
        Date startDate = productPaymentStatusList.get(0).getPaymentDate();
        boolean isSameDay =
                DateUtils.isSameDay(productPaymentStatusList.get(0).getPaymentDate(), Calendar.getInstance().getTime());
        if (!isSameDay) {
            startDate = DateUtil.changeDateBy(productPaymentStatusList.get(0).getPaymentDate(), 1);
        }
        String configName = TransactionCategoryGroups.getById(updatedTransactionGroupId).getLabel().toLowerCase() + "."
                + saleType.toString().toLowerCase()
                + ".status.duration";
        // TODO - create config for owner
        int duration = UtilityClass
                .safeUnbox(configService.getConfigValueAsInteger(ConfigGroupName.Relevance, configName), 30);
        Date endDate = DateUtil.changeDateBy(startDate, duration);

        Pair<Date, Date> durationPair = new Pair<>(
                new Date(new DateTime(startDate).withTimeAtStartOfDay().getMillis()),
                new Date(new DateTime(endDate).withTimeAtStartOfDay().getMillis()));
        return durationPair;
    }

    public void createOrUpdateSRFForNewPaymentsV2(
            Integer sellerUserId,
            RawSellerDTO rawSeller,
            List<ProductPaymentStatus> productPaymentStatusList,
            SellerRelevanceFactors sellerRelevanceFactors,
            Integer updatedTransactionGroupId,
            MasterLeadPaymentTypeEnum saleType) {
        if (CollectionUtils.isEmpty(productPaymentStatusList) || sellerRelevanceFactors == null)
            return;
        Pair<Date, Date> durationPair = calculateDurationV2(
                productPaymentStatusList,
                updatedTransactionGroupId,
                saleType,
                sellerRelevancePackageDao.findBySellerRelevanceId(sellerRelevanceFactors.getId()));
        sellerRelevanceFactors.setSellerTransGroupId(updatedTransactionGroupId);
        sellerRelevanceFactors.setManualOverrideStartDate(durationPair.getFirst());
        sellerRelevanceFactors.setManualOverrideEndDate(durationPair.getSecond());
        sellerRelevanceFactors.setCap(calculateCap(rawSeller, productPaymentStatusList, saleType));
    }

    private Pair<Date, Date> calculateDurationV2(
            List<ProductPaymentStatus> productPaymentStatusList,
            Integer updatedTransactionGroupId,
            MasterLeadPaymentTypeEnum saleType,
            Collection<SellerRelevancePackage> sellerRelevancePackages) {
        Date startDate = productPaymentStatusList.get(0).getPaymentDate();
        int duration = 0;

        boolean isSameDay =
                DateUtils.isSameDay(productPaymentStatusList.get(0).getPaymentDate(), Calendar.getInstance().getTime());
        if (!isSameDay) {
            startDate = DateUtil.changeDateBy(productPaymentStatusList.get(0).getPaymentDate(), 1);
        }
        if (CollectionUtils.isNotEmpty(sellerRelevancePackages)) {
            duration = getMaxDuration(sellerRelevancePackages, updatedTransactionGroupId, saleType);
        }
        else {
            // return;
            String configName =
                    TransactionCategoryGroups.getById(updatedTransactionGroupId).getLabel().toLowerCase() + "."
                            + saleType.toString().toLowerCase()
                            + ".status.duration";
            duration = UtilityClass
                    .safeUnbox(configService.getConfigValueAsInteger(ConfigGroupName.Relevance, configName), 30);
        }

        Date endDate = DateUtil.changeDateBy(startDate, duration);

        Pair<Date, Date> durationPair = new Pair<>(
                new Date(new DateTime(startDate).withTimeAtStartOfDay().getMillis()),
                new Date(new DateTime(endDate).withTimeAtStartOfDay().getMillis()));
        return durationPair;
    }

    public void calculateAndUpdatePackageDuration(
            Map<Integer, Map<SellerRelevanceFactors, List<ProductPaymentStatus>>> srfnListForUpdatePackageEndDate) {
        for (Map.Entry<Integer, Map<SellerRelevanceFactors, List<ProductPaymentStatus>>> entry : srfnListForUpdatePackageEndDate
                .entrySet()) {
            List<ProductPaymentStatus> productPaymentStatusList =
                    entry.getValue().entrySet().iterator().next().getValue();
            SellerRelevanceFactors sellerRelevanceFactors = entry.getValue().entrySet().iterator().next().getKey();
            Integer updatedTransactionGroupId = sellerRelevanceFactors.getSellerTransGroupId();
            MasterLeadPaymentTypeEnum saleType =
                    MasterLeadPaymentTypeEnum.getMasterLeadPaymentTypeEnumById(sellerRelevanceFactors.getSaleTypeId());

            Pair<Date, Date> durationPair = calculateDurationV2(
                    productPaymentStatusList,
                    updatedTransactionGroupId,
                    saleType,
                    sellerRelevancePackageDao.findBySellerRelevanceId(sellerRelevanceFactors.getId()));
            sellerRelevanceFactors.setManualOverrideEndDate(durationPair.getSecond());
            sellerRelevanceFactorsDao.save(sellerRelevanceFactors);
        }
    }

    private int getMaxDuration(
            Collection<SellerRelevancePackage> sellerRelevancePackages,
            Integer updatedTransactionGroupId,
            MasterLeadPaymentTypeEnum saleType) {
        int duration = 0;
        String packageString = null;
        String configName = null;
        for (SellerRelevancePackage sellerRelevancePackage : sellerRelevancePackages) {
            if (sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.CITY.getId()) {
                packageString = MasterSellerRelevancePackageTypeEnum.CITY.getName().toLowerCase();
            }
            else if (sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.LOCALITY
                    .getId()) {
                packageString = MasterSellerRelevancePackageTypeEnum.LOCALITY.getName().toLowerCase();
            }
            else if (sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.SUBURB.getId()) {
                packageString = MasterSellerRelevancePackageTypeEnum.SUBURB.getName().toLowerCase();
            }
            else if (sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.PROJECT
                    .getId()) {
                packageString = MasterSellerRelevancePackageTypeEnum.PROJECT.getName().toLowerCase();
            }

            if (packageString != null) {
                if (packageString != MasterSellerRelevancePackageTypeEnum.PROJECT.getName().toLowerCase()) {
                    configName =
                            TransactionCategoryGroups.getById(updatedTransactionGroupId).getLabel().toLowerCase() + "."
                                    + saleType.toString().toLowerCase()
                                    + "."
                                    + packageString
                                    + ".status.duration";
                }
                else {
                    configName = saleType.toString().toLowerCase() + "." + packageString + ".status.duration";
                }
            }
            else {
                configName = TransactionCategoryGroups.getById(updatedTransactionGroupId).getLabel().toLowerCase() + "."
                        + saleType.toString().toLowerCase()
                        + ".status.duration";
            }

            int durationTemp = UtilityClass
                    .safeUnbox(configService.getConfigValueAsInteger(ConfigGroupName.Relevance, configName), 30);

            if (durationTemp > duration) {
                duration = durationTemp;
            }
        }
        return duration;
    }

    private int calculateCap(
            RawSellerDTO rawSeller,
            List<ProductPaymentStatus> productPaymentStatusList,
            MasterLeadPaymentTypeEnum saleType) {
        int cap = 0;
        //D-----------
        if (rawSeller.getCompanyUser().getSellerType().equals(com.proptiger.core.enums.external.mbridge.SellerType.Owner)) {
            cap = 30; // TODO - move to config
        }
        else {
            Date to = productPaymentStatusList.get(0).getPaymentDate();
            Date from = DateUtil.changeDateBy(to, -7);
            List<ProductPaymentStatus> closedDeals = getClosedDealsIn(productPaymentStatusList, from, to);
            int noOfClosedDealsInBatch = closedDeals.size();
            String configName = "promised." + saleType.toString().toLowerCase()
                    + ".leads.for."
                    + noOfClosedDealsInBatch
                    + ".deals.reported";

            if (noOfClosedDealsInBatch <= 10) {
                cap = UtilityClass
                        .safeUnbox(configService.getConfigValueAsInteger(ConfigGroupName.Relevance, configName), 0);
            }
            else {
                cap = 1200;
            }
        }
        return cap;
    }

    public Pair<Integer, Integer> getDealClosureCount(List<ProductPaymentStatus> productPaymentStatusList) {
        if (CollectionUtils.isEmpty(productPaymentStatusList)) {
            return new Pair<Integer, Integer>(0, 0);
        }
        productPaymentStatusList.sort(LEAD_PAYMENT_STATUS_ORDERING_DESC);
        Date to = productPaymentStatusList.get(0).getPaymentDate();
        Date from = DateUtil.changeDateBy(to, -7);
        List<ProductPaymentStatus> deals = getClosedDealsIn(productPaymentStatusList, from, to);

        int dealsClosed = 0, openPrepaidPayments = 0;
        for (ProductPaymentStatus lps : deals) {
            // TODO - move to product types
            if (lps.getProductId() == null) {
                openPrepaidPayments++;
            }
            else {
                dealsClosed++;
            }
        }
        return new Pair<Integer, Integer>(dealsClosed, openPrepaidPayments);
    }

    public String getSellerSRFOrderedSetByPriority(Integer sellerUserId) {

        List<SellerRelevanceFactors> sellerRelevanceFactors =
                sellerRelevanceFactorsDao.findBySellerIdsWithPackages(Arrays.asList(sellerUserId));
        return getSellerSRFOrderedSetByPriority(sellerRelevanceFactors);
    }

    public String getSellerSRFOrderedSetByPriority(
            List<SellerRelevanceFactors> sellerRelevanceFactors) {
        TreeSet<SellerRelevanceFactors> sellerStatusToSellerRelevanceFactorSet =
                new TreeSet<>(new Comparator<SellerRelevanceFactors>() {
                    @Override
                    public int compare(SellerRelevanceFactors o1, SellerRelevanceFactors o2) {
                        int comparisonValue = SELLER_STATUS_PRIORITY_MAP
                                .get(TransactionCategoryGroups.getById(o1.getSellerTransGroupId()).getLabel())
                                .compareTo(
                                        SELLER_STATUS_PRIORITY_MAP.get(
                                                TransactionCategoryGroups.getById(o2.getSellerTransGroupId())
                                                        .getLabel()));
                        if (comparisonValue == 0) {
                            SellerRelevanceTransactionStatuses o1TxnStatus =
                                    getPackageFactor(o1.getSellerRelevanceFactorPackage());
                            SellerRelevanceTransactionStatuses o2TxnStatus =
                                    getPackageFactor(o2.getSellerRelevanceFactorPackage());
                            if (o1TxnStatus != null && o2TxnStatus != null) {
                            comparisonValue = SELLER_STATUS_PRIORITY_MAP
                                    .get(o1TxnStatus.toString())
                                    .compareTo(
                                            SELLER_STATUS_PRIORITY_MAP.get(
                                                    o2TxnStatus.toString()));
                            }

                        }
                        return comparisonValue;
                    }

                    private SellerRelevanceTransactionStatuses getPackageFactor(
                            Set<SellerRelevancePackage> o1Packages) {
                        SellerRelevanceTransactionStatuses badge = null;
                        for (SellerRelevancePackage sellerRelevancePackage : o1Packages) {


                            if (sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.CITY
                                    .getId()) {
                                badge = SellerRelevanceTransactionStatuses.CITY_EXPERT_DEAL_MAKER;
                                break;
                            }
                            else if (sellerRelevancePackage
                                    .getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.LOCALITY.getId()
                                    || sellerRelevancePackage
                                            .getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.SUBURB
                                                    .getId()) {
                                badge = SellerRelevanceTransactionStatuses.LOCALITY_EXPERT_DEAL_MAKER;
                            }
                        }
                        return badge;
                    }

                });

        sellerStatusToSellerRelevanceFactorSet.addAll(sellerRelevanceFactors);
        SellerRelevanceFactors sellerStatusWithHighestPriority = sellerStatusToSellerRelevanceFactorSet.first();
        String badge = null;
        switch (TransactionCategoryGroups.getById(sellerStatusWithHighestPriority.getSellerTransGroupId())) {
            case EXPERT_DEAL_MAKER:

                Set<SellerRelevancePackage> sellerRelevancePackages =
                        sellerStatusWithHighestPriority.getSellerRelevanceFactorPackage();
                for (SellerRelevancePackage sellerRelevancePackage : sellerRelevancePackages) {

                    if (sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.CITY
                            .getId()) {
                        badge = SellerRelevanceTransactionStatuses.CITY_EXPERT_DEAL_MAKER.name();
                        ;
                        break;
                    }
                    else if (sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.LOCALITY
                            .getId()
                            || sellerRelevancePackage.getPackageTypeId() == MasterSellerRelevancePackageTypeEnum.SUBURB
                                    .getId()) {
                        badge = SellerRelevanceTransactionStatuses.LOCALITY_EXPERT_DEAL_MAKER.name();
                    }
                }

                break;
            default:
                badge = TransactionCategoryGroups.getById(sellerStatusWithHighestPriority.getSellerTransGroupId())
                        .name();

        }

        return badge;
    }

    @Async
    public void sendNotificationForSRFStatusChange(List<Integer> sellerUserIds) {
        if (CollectionUtils.isNotEmpty(sellerUserIds)) {
            sellerUserIds.forEach(sellerId -> sendNotificationForSRFStatusChangeInternal(sellerId));
        }

    }

    private void sendNotificationForSRFStatusChangeInternal(Integer sellerUserId) {
        String sellerStatusToSellerRelevanceFactorSet =
                getSellerSRFOrderedSetByPriority(sellerUserId);
        Map<String, Object> notficationBodyMap = new HashMap<>();
        notficationBodyMap.put(
                "sellerAccountStatus",
                sellerStatusToSellerRelevanceFactorSet);
        String notificationBody = new Gson().toJson(notficationBodyMap);

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put(Tokens.Default.extraData.name(), notificationBody);
        payloadMap.put(
                Tokens.SellerRelevanceStatusChange.body.name(),
                sellerStatusToSellerRelevanceFactorSet);

        NotificationCreatorServiceRequest request = new NotificationCreatorServiceRequest(
                NotificationTypeEnum.SellerRelevanceStatusChange,
                sellerUserId,
                payloadMap,
                null);

        List<MediumDetails> mediumList = new ArrayList<MediumDetails>();
        List<GCMUser> gcmUsers = userServiceHelper.findGCMUsers(MobileApplication.MakaanSeller, sellerUserId, false);
        if (CollectionUtils.isNotEmpty(gcmUsers)) {
            mediumList.add(new DefaultMediumDetails(MediumType.MakaanSellerApp));
            request.setMediumDetails(mediumList);
            notificationCreatorService.createNotificationGenerated(request);
        }
    }

    public Float getTRSBasedOnVisbilityRank(
            Set<TransactionCategoryGroups> parentGroups,
            Set<SellerVisibilityRankings> sellerVisibilityRankings) {
        Float transactionRevealScore = 0f;
        for (SellerVisibilityRankings svr : sellerVisibilityRankings) {
            if (svr.getVisibilityAttributeId().equals(VisibilityAttributes.SERP.getId())) {
                if (svr.getRankId().equals(VisibilityRanks.SEALEVEL.getId())) {
                    transactionRevealScore = 0f;
                }
                else if (parentGroups.contains(TransactionCategoryGroups.EXPERT_DEAL_MAKER)) {
                    transactionRevealScore = EXPERT_DEFAULT_SCORE;
                }
                else if (parentGroups.contains(TransactionCategoryGroups.DEAL_MAKER)) {
                    transactionRevealScore = DEAL_MAKER_DEFAULT_SCORE;
                }
                else if (parentGroups.contains(TransactionCategoryGroups.PREPAID_SELLER)) {
                    transactionRevealScore = PREPAID_SELLER_DEFAULT_SCORE;
                }
            }
        }
        return transactionRevealScore;
    }

    public Float computeTransactionRevealScoreForListing(
            Listing listing,
            List<String> listingSellerTransactionStatuses,
            CompanyRelevantScoresDTO companyRelevantScoresDTO,
            CategoryType categoryType) {
        Float transactionRevealScore = null;
        Set<String> transactionStatusesSet = new HashSet<>(listingSellerTransactionStatuses);
        if (transactionStatusesSet.contains(SellerRelevanceTransactionStatuses.EXPERT_DEAL_MAKER.toString())) {
            transactionRevealScore = computTransactionRevealScoreForExpert(transactionStatusesSet);
        }
        else if (transactionStatusesSet.contains(SellerRelevanceTransactionStatuses.PREMIUM_SELLER.toString())) {
            transactionRevealScore = computTransactionRevealScoreForPremium(transactionStatusesSet);
        }
        return transactionRevealScore;
    }

    private Float computTransactionRevealScoreForPremium(Set<String> transactionStatusesSet) {
        Float transactionRevealScore = null;
        if (transactionStatusesSet.contains(SellerRelevanceTransactionStatuses.LISTING_PREMIUM_SELLER.toString())) {
            transactionRevealScore = LISTING_PREMIUM_SELLER_DEFAULT_SCORE;
        }
        else {
            transactionRevealScore = PREMIUM_SELLER_DEFAULT_SCORE;
        }
        return transactionRevealScore;
    }

    private Float computTransactionRevealScoreForExpert(Set<String> transactionStatusesSet) {
        Float transactionRevealScore = null;
        if (transactionStatusesSet.contains(SellerRelevanceTransactionStatuses.CITY_EXPERT_DEAL_MAKER.toString())
                && transactionStatusesSet
                        .contains(SellerRelevanceTransactionStatuses.LOCALITY_EXPERT_DEAL_MAKER.toString())) {
            transactionRevealScore = CITY_EXPERT_LOCALITY_SCORE;
        }
        else if (transactionStatusesSet
                .contains(SellerRelevanceTransactionStatuses.CITY_EXPERT_DEAL_MAKER.toString())) {
            transactionRevealScore = CITY_EXPERT_CITY_SCORE;
        }
        else if (transactionStatusesSet
                .contains(SellerRelevanceTransactionStatuses.LOCALITY_EXPERT_DEAL_MAKER.toString())) {
            transactionRevealScore = LOCALITY_EXPERT_LOCALITY_SCORE;
        }
        else {
            transactionRevealScore = EXPERT_DEFAULT_SCORE;
        }
        return transactionRevealScore;
    }

}
