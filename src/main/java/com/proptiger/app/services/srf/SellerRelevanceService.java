package com.proptiger.app.services.srf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.proptiger.app.model.relevance.BaseLevel;
//import com.proptiger.app.model.transaction.ProductPaymentStatus;
import com.proptiger.app.model.relevance.BaseLevelFactory;
import com.proptiger.core.helper.ICRMHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.SellerRelevancePackageComponents;
import com.proptiger.core.model.cms.SellerVisibilityRankings;
import com.proptiger.core.model.cms.MasterSellerRelevancePackageType.MasterSellerRelevancePackageTypeEnum;
import com.proptiger.core.model.cms.MasterSellerVisibilityAttributes.VisibilityAttributes;
import com.proptiger.core.model.cms.MasterSellerVisibiltyRanks.VisibilityRanks;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.pojo.Pair;
import com.proptiger.core.service.ConfigService;
import com.proptiger.core.service.ConfigService.ConfigGroupName;
import com.proptiger.core.util.DateUtil;
import com.proptiger.core.util.UtilityClass;
//import com.proptiger.data.model.relevance.BaseLevel;
//import com.proptiger.data.model.relevance.BaseLevelFactory;

/*
 * Can it be moved to core.. ???
* TODO Comment for the time..
*
*/
//import com.proptiger.data.service.marketplace.ListingScoreConfig;
//import com.proptiger.data.service.srf.SellerRelevanceHelper;
//import com.proptiger.data.service.srf.SellerRelevancePackageComponentsService;
//import com.proptiger.data.service.srf.SellerVisibilityRankingsService;
//import com.proptiger.marketforce.data.service.RawSellerService;

@Service
public class SellerRelevanceService {
	
	 @Value("${seller.excluded.from.lead.control}")
	    private String                                  sellerExcludedFromLeadControl;

	    private static final String                     LEVEL_1                            = "LEVEL_1";

	    private static final String                     LEVEL_2                            = "LEVEL_2";

	    private static final String                     LEVEL_3                            = "LEVEL_3";

	    private static final int                        LEAD_DELIVERY_DAYS_FOR_ONE_PAYMENT = 7;

	    private static int                              PAID_SELLER_INTITIAL_VISIBILITY_DURATION;

	    private static List<Integer>                    sellerIdsExcludedFromLeadControl;

	    @Autowired
	    private SellerRelevancePackageComponentsService sellerRelevancePackageComponentsService;

	    @Autowired
	    private SellerVisibilityRankingsService         sellerVisibilityRankingsService;

	    @Autowired
	    private ConfigService                           configService;

	    @Autowired
	    private BaseLevelFactory                        levelFactory;

	    @Autowired
	    private SellerRelevanceHelper           sellerRelevanceHelper;

	    @Autowired
	    private ICRMHelper                      icrmHelper;

	    @Autowired
	    private ApplicationContext              applicationContext;
//@Divyanshu
	    @Autowired
	    private MIDLServiceHelper midlServiceHelper;
	    // DO NOT AUTOWIRE
	   // private RawSellerService                rawSellerService;    

	    @PostConstruct
	    public void init() {
//	        rawSellerService = applicationContext.getBean(RawSellerService.class);

	        PAID_SELLER_INTITIAL_VISIBILITY_DURATION = UtilityClass.safeUnbox(
	                configService.getConfigValueAsInteger(
	                        ConfigGroupName.Relevance,
	                   //     ListingScoreConfig.RelevanceScore.PAID_SELLER_INTITIAL_VISIBILITY_DURATION),
	                        /*
	                         * Hardcoded the value for the time ...@Divyanshu
	                         */
	                        "paid.seller.inital.visiblity.duration"),
	                        3);
	        sellerIdsExcludedFromLeadControl = Arrays.stream(sellerExcludedFromLeadControl.split(","))
	                .collect(Collectors.toSet()).stream().map(Integer::parseInt).collect(Collectors.toList());
	    }

	    /**
	     * 
	     * @param updatSellerRelevanceFactors
	     * @param leadPaymentList
	     */
	    public List<SellerVisibilityRankings> computeAndSetVisibilityRankings(
	            SellerRelevanceFactors updatSellerRelevanceFactors,
	            List<ProductPaymentStatus> leadPaymentList) {

	        List<SellerVisibilityRankings> svr = null;
	        List<SellerRelevancePackageComponents> sellerRelevancePackageComponents = null;

	        if (updatSellerRelevanceFactors.getId() != null) {
	            sellerRelevancePackageComponents =
	                    sellerRelevancePackageComponentsService.getPackageComponenetsForSellerRelevanceIds(
	                            Collections.singletonList(updatSellerRelevanceFactors.getId()));

	        }
	        /*
	         * Do not process city experts, locality experts and Proptiger
	         */
	        boolean isDomainExpert = checkDomainExpert(sellerRelevancePackageComponents);

	        if (!isDomainExpert && !sellerIdsExcludedFromLeadControl.contains(updatSellerRelevanceFactors.getSellerId())) {
	            svr = getVisibilityRanks(updatSellerRelevanceFactors, leadPaymentList);
	        }
	        return svr;
	    }

	    /**
	     * Check if seller is domain expert
	     * 
	     * @param sellerRelevancePackageComponents
	     * @return
	     */
	    public boolean checkDomainExpert(List<SellerRelevancePackageComponents> sellerRelevancePackageComponents) {
	        boolean isDomainExpert = false;
	        if (sellerRelevancePackageComponents != null) {
	            for (SellerRelevancePackageComponents srpc : sellerRelevancePackageComponents) {
	                if (srpc.getAttributeId().equals(MasterSellerRelevancePackageTypeEnum.CITY.getId())
	                        || srpc.getAttributeId().equals(MasterSellerRelevancePackageTypeEnum.LOCALITY.getId())) {
	                    isDomainExpert = true;
	                    break;
	                }
	            }
	        }
	        return isDomainExpert;
	    }

	    private List<SellerVisibilityRankings> getVisibilityRanks(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            List<ProductPaymentStatus> leadPaymentList) {

	        int daysSincePayment = (int) DateUtil.getNumberOfDaysDifferenceInDate(
	                sellerRelevanceFactors.getManualOverrideStartDate(),
	                Calendar.getInstance().getTime());

	        Date toDate = sellerRelevanceFactors.getManualOverrideStartDate();
	        Date fromDate = DateUtil.changeDateBy(toDate, -7);
	        int paidDealsCount = sellerRelevanceHelper.getClosedDealsIn(leadPaymentList, fromDate, toDate).size();

	        int leadsPromised = sellerRelevanceFactors.getCap();
	        int daysEstimated = LEAD_DELIVERY_DAYS_FOR_ONE_PAYMENT * (1 + paidDealsCount);
	        int leadsDelivered = icrmHelper.getLeadCountBySaleType(
	        //@Divyanshu		
	           //   rawSellerService.getRawSellerFromSolrViaSellerUserId(sellerRelevanceFactors.getSellerId()).getCompanyId(),
	                midlServiceHelper.getRawSellerFromSolrViaSellerUserId(sellerRelevanceFactors.getSellerId()).getCompanyUser().getCompanyId(),
	                sellerRelevanceFactors.getManualOverrideStartDate(),
	                sellerRelevanceFactors.getSaleTypeId());
	        List<SellerVisibilityRankings> sellerVisibilityRankingsNew = new ArrayList<>();
	        List<SellerVisibilityRankings> sellerVisibilityRankingsOld = null;
	        if (sellerRelevanceFactors.getId() != null) {
	            sellerVisibilityRankingsOld =
	                    sellerVisibilityRankingsService.getSellerVisibilityRankingsBySellerRelevantIds(
	                            Collections.singletonList(sellerRelevanceFactors.getId()));
	        }
	        /*
	         * Too many cases ahead
	         */
	        if (CollectionUtils.isEmpty(sellerVisibilityRankingsOld)) {
	            sellerVisibilityRankingsNew = keepAtDefaultLevel(sellerRelevanceFactors);
	        }
	        else if (daysSincePayment <= PAID_SELLER_INTITIAL_VISIBILITY_DURATION) {
	            sellerVisibilityRankingsNew = keepAtDefaultLevel(sellerRelevanceFactors);
	        }
	        else if (daysSincePayment >= daysEstimated) {
	            if (leadsDelivered < leadsPromised) {
	                sellerVisibilityRankingsNew = levelUp(sellerRelevanceFactors, sellerVisibilityRankingsOld);
	            }
	            else {
	                sellerVisibilityRankingsNew = levelDown(sellerRelevanceFactors, sellerVisibilityRankingsOld);
	            }
	        }
	        else if (leadsDelivered > leadsPromised) {
	            sellerVisibilityRankingsNew = levelDown(sellerRelevanceFactors, sellerVisibilityRankingsOld);
	        }
	        else {
	            Float rateOfDelivery = (float) leadsDelivered / daysSincePayment;
	            Float estimatedLeadsDelivery = rateOfDelivery * daysEstimated;
	            sellerVisibilityRankingsNew = changeLevel(
	                    sellerRelevanceFactors,
	                    sellerVisibilityRankingsOld,
	                    rateOfDelivery,
	                    estimatedLeadsDelivery,
	                    leadsPromised);
	        }

	        return sellerVisibilityRankingsNew;
	    }

	    private List<SellerVisibilityRankings> keepAtDefaultLevel(SellerRelevanceFactors sellerRelevanceFactors) {
	        BaseLevel level = levelFactory.getLevel(LEVEL_2, sellerRelevanceFactors);
	        return getVisibilityRanksForSellerForAttributesAndRanks(
	                sellerRelevanceFactors,
	                level.getAttributeIdToRankIdPairList());
	    }

	    private List<SellerVisibilityRankings> changeLevel(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            List<SellerVisibilityRankings> sellerVisibilityRankingsOld,
	            Float rateOfDelivery,
	            Float estimatedLeadsDelivery,
	            int leadsPromised) {

	        List<SellerVisibilityRankings> sellerVisibilityRankingsList = new ArrayList<>();
	        if (estimatedLeadsDelivery > leadsPromised) {
	            sellerVisibilityRankingsList = levelDown(sellerRelevanceFactors, sellerVisibilityRankingsOld);
	        }
	        else {
	            sellerVisibilityRankingsList = levelUp(sellerRelevanceFactors, sellerVisibilityRankingsOld);
	        }

	        return sellerVisibilityRankingsList;
	    }

	    private List<SellerVisibilityRankings> levelDown(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            List<SellerVisibilityRankings> sellerVisibilityRankingsOld) {
	        List<SellerVisibilityRankings> sellerVisibilityRankingsUpdated = new ArrayList<>();

	        Map<Integer, Integer> attributeIdToRankIdMap = new HashMap<>();

	        for (SellerVisibilityRankings svr : sellerVisibilityRankingsOld) {
	            attributeIdToRankIdMap.put(svr.getVisibilityAttributeId(), svr.getRankId());
	        }

	        /*
	         * On first level move to second level
	         */

	        if (attributeIdToRankIdMap.get(VisibilityAttributes.SERP.getId()).equals(VisibilityRanks.MOUNTAIN.getId())
	                && attributeIdToRankIdMap.get(VisibilityAttributes.MULTIPLICATION.getId())
	                        .equals(VisibilityRanks.MOUNTAIN.getId())) {
	            BaseLevel level = levelFactory.getLevel(LEVEL_2, sellerRelevanceFactors);
	            sellerVisibilityRankingsUpdated = getVisibilityRanksForSellerForAttributesAndRanks(
	                    sellerRelevanceFactors,
	                    level.getAttributeIdToRankIdPairList());
	        }

	        /*
	         * On second level, move to third level
	         */
	        if (attributeIdToRankIdMap.get(VisibilityAttributes.SERP.getId()).equals(VisibilityRanks.MOUNTAIN.getId())
	                && attributeIdToRankIdMap.get(VisibilityAttributes.MULTIPLICATION.getId())
	                        .equals(VisibilityRanks.TRENCH.getId())) {

	            BaseLevel level = levelFactory.getLevel(LEVEL_3, sellerRelevanceFactors);
	            sellerVisibilityRankingsUpdated = getVisibilityRanksForSellerForAttributesAndRanks(
	                    sellerRelevanceFactors,
	                    level.getAttributeIdToRankIdPairList());

	        }

	        /*
	         * On third level, cannot go lower
	         */
	        if (attributeIdToRankIdMap.get(VisibilityAttributes.SERP.getId()).equals(VisibilityRanks.SEALEVEL.getId())
	                && attributeIdToRankIdMap.get(VisibilityAttributes.MULTIPLICATION.getId())
	                        .equals(VisibilityRanks.TRENCH.getId())) {

	            sellerVisibilityRankingsUpdated = sellerVisibilityRankingsOld;

	        }

	        return sellerVisibilityRankingsUpdated;
	    }

	    private List<SellerVisibilityRankings> levelUp(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            List<SellerVisibilityRankings> sellerVisibilityRankingsOld) {
	        List<SellerVisibilityRankings> sellerVisibilityRankingsUpdated = new ArrayList<>();

	        Map<Integer, Integer> attributeIdToRankIdMap = new HashMap<>();

	        for (SellerVisibilityRankings svr : sellerVisibilityRankingsOld) {
	            attributeIdToRankIdMap.put(svr.getVisibilityAttributeId(), svr.getRankId());
	        }

	        /*
	         * On first level cannot go above
	         */

	        if (attributeIdToRankIdMap.get(VisibilityAttributes.SERP.getId()).equals(VisibilityRanks.MOUNTAIN.getId())
	                && attributeIdToRankIdMap.get(VisibilityAttributes.MULTIPLICATION.getId())
	                        .equals(VisibilityRanks.MOUNTAIN.getId())) {
	            sellerVisibilityRankingsUpdated = sellerVisibilityRankingsOld;
	        }

	        /*
	         * On second level, move to first level
	         */
	        if (attributeIdToRankIdMap.get(VisibilityAttributes.SERP.getId()).equals(VisibilityRanks.MOUNTAIN.getId())
	                && attributeIdToRankIdMap.get(VisibilityAttributes.MULTIPLICATION.getId())
	                        .equals(VisibilityRanks.TRENCH.getId())) {

	            BaseLevel level = levelFactory.getLevel(LEVEL_1, sellerRelevanceFactors);
	            sellerVisibilityRankingsUpdated = getVisibilityRanksForSellerForAttributesAndRanks(
	                    sellerRelevanceFactors,
	                    level.getAttributeIdToRankIdPairList());
	        }

	        /*
	         * On third level, move to second level
	         */
	        if (attributeIdToRankIdMap.get(VisibilityAttributes.SERP.getId()).equals(VisibilityRanks.SEALEVEL.getId())
	                && attributeIdToRankIdMap.get(VisibilityAttributes.MULTIPLICATION.getId())
	                        .equals(VisibilityRanks.TRENCH.getId())) {

	            BaseLevel level = levelFactory.getLevel(LEVEL_2, sellerRelevanceFactors);
	            sellerVisibilityRankingsUpdated = getVisibilityRanksForSellerForAttributesAndRanks(
	                    sellerRelevanceFactors,
	                    level.getAttributeIdToRankIdPairList());

	        }

	        return sellerVisibilityRankingsUpdated;
	    }

	    private List<SellerVisibilityRankings> getVisibilityRanksForSellerForAttributesAndRanks(
	            SellerRelevanceFactors sellerRelevanceFactors,
	            List<Pair<Integer, Integer>> attributeIdToRankIdPairList) {
	        List<SellerVisibilityRankings> sellerVisibilityRankingsList = new ArrayList<>();

	        for (Pair<Integer, Integer> pair : attributeIdToRankIdPairList) {
	            SellerVisibilityRankings sellerVisibilityRankings = new SellerVisibilityRankings();
	            sellerVisibilityRankings.setSellerRelevanceFactorId(sellerRelevanceFactors.getId());
	            sellerVisibilityRankings.setVisibilityAttributeId(pair.getFirst());
	            sellerVisibilityRankings.setRankId(pair.getSecond());
	            sellerVisibilityRankingsList.add(sellerVisibilityRankings);
	        }
	        return sellerVisibilityRankingsList;
	    }


}
