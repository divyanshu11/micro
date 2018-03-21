package com.proptiger.app.service.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableSet;
import com.proptiger.core.dto.order.LeadPaymentDocument;
//import com.proptiger.app.dto.order.LeadPaymentDocument;
import com.proptiger.app.dto.order.LeadPaymentDto;
//import com.proptiger.app.dto.order.LeadPaymentStatusDto;
import com.proptiger.app.dto.order.LeadPrePaymentDTO;
import com.proptiger.app.repo.order.LeadPaymentStatusAttributeDao;
import com.proptiger.app.repo.order.LeadPaymentStatusDao;
//import com.proptiger.app.repo.transaction.PaymentAttributeDao;
import com.proptiger.core.config.AppCachingConfig;
import com.proptiger.core.constants.ResponseCodes;
import com.proptiger.core.constants.ResponseErrorMessages;
import com.proptiger.core.dto.external.ICRMLead;
import com.proptiger.core.dto.external.LeadPaymentStatusPatchDTO;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.dto.internal.LeadFinanceDto;
import com.proptiger.core.dto.order.LeadPaymentStatusDto;
import com.proptiger.core.dto.order.LeadsCityDTO;
import com.proptiger.core.dto.order.ProductPrePaymentDto;
import com.proptiger.core.enums.MasterEntityTypes;
import com.proptiger.core.enums.SaleTypeEnum;
import com.proptiger.core.enums.marketforce.Access;
import com.proptiger.core.exception.BadRequestException;
import com.proptiger.core.exception.ProAPIException;
import com.proptiger.core.exception.UnauthorizedException;
import com.proptiger.core.helper.CyclopsServiceHelper;
import com.proptiger.core.helper.ICRMHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.helper.UserServiceHelper;
import com.proptiger.core.model.cyclops.SellerDealPrice;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentStatusEnum;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentTypeEnum;
import com.proptiger.core.model.enums.transaction.SellerPaymentType;
import com.proptiger.core.model.enums.transaction.TransactionType;
import com.proptiger.core.model.transaction.LeadPaymentStatus;
import com.proptiger.core.model.transaction.LeadPaymentStatusMetaAttributes;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.model.user.User;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.Selector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.service.SNSService;
import com.proptiger.core.util.Constants;
import com.proptiger.core.util.NullAwareBeanUtilsBean;
import com.proptiger.core.util.SecurityContextUtils;
import com.proptiger.cyclops.dto.SellerDealPriceResponseDTO;
//import com.proptiger.data.dto.order.LeadPaymentDocument;
//import com.proptiger.data.dto.order.LeadPaymentDto;
//import com.proptiger.data.dto.order.LeadPaymentStatusDto;
//import com.proptiger.data.dto.order.LeadPrePaymentDTO;
//import com.proptiger.data.repo.order.LeadPaymentStatusAttributeDao;
//import com.proptiger.data.repo.order.LeadPaymentStatusDao;
//import com.proptiger.data.repo.transaction.PaymentAttributeDao;
//import com.proptiger.data.service.CouponNotificationService;
//import com.proptiger.data.service.order.LeadPaymentStatusService;
//import com.proptiger.data.service.order.ProductPaymentStatusService;
//import com.proptiger.data.service.transaction.TransactionService;
//import com.proptiger.marketforce.data.enums.Access;
//import com.proptiger.marketforce.data.service.RawSellerService;

/**
 * 
 * @author swapnil
 *
 */
/**
 * @author sahebsinghjohar
 * This class is deprecated as functionaly and works on lead_payment_status mysql view. 
 * Instead we should use productPaymentService now. All data from lead_payment_service is also migrated to product_payment_status 
 */
@Service
@Deprecated
public class LeadPaymentStatusService {

    private static final String              LEAD_ID         = "leadId";
    private static final String              SALE_TYPE_ID    = "saleTypeId";
    private static final String              FIELD_CITY_ID   = "cityId";

	@Autowired
    private LeadPaymentStatusDao          leadPaymentStatusDao;

    // @Autowired
    // DO NOT AUTOWIRE
  //  private TransactionService            transactionService;

//    @Autowired
//    private PaymentAttributeDao           paymentAttributeDao;
    
    //@Divyanshu
    @Autowired
    private MIDLServiceHelper			 midlServiceHelper;

    @Autowired
    private ICRMHelper                    icrmHelper;

//    @Autowired
//    private CouponNotificationService     couponNotificationService;

    @Autowired
    private UserServiceHelper             userServiceHelper;

    @Autowired
    private ApplicationContext            applicationContext;

    @Autowired
    private LeadPaymentStatusAttributeDao leadPaymentStatusAttributeDao;

    @Autowired
    private CyclopsServiceHelper          cyclopsServiceHelper;

    @Autowired
    private SNSService                    snsService;

    @Autowired
    private ProductPaymentStatusService   productPaymentStatusService;

    @Value("${lead.payment.confirmed.sns.topic.arn}")
    private String                        leadPaymentConfirmedSnsTopic;

    private static final String           FINANCE_TEAM_ROLE                               = "Finance";
    private static final String           SELLER_ROLE                                     = "Seller";

    private static final String           PAYMENT_STATUS_REVERSAL_SUCCESS_MESSAGE_FORMAT  =
            "Lead payment status reverted for lead id %d";

    private static final String           LEAD_PAYMENT_STATUS_REVERSAL_BY_LEAD_PAYMENT_ID =
            "Lead payment status reverted for lead payment status ids %s";

    private static final String           LEAD_ID_REVERSAL_FOR_PREPAYMENT_LEADS           =
            "Lead Ids %s reverted for lead payment status ids";

    private static final int              REST_OF_INDIA_DEFAULT_CITY_ID                   = -1;

    public static final Integer           COMMENT_ID_FOR_PAYMENT_STATUS_REVERSAL          = 327;

 //   private RawSellerService              rawSellerService;

    private static Logger                 logger                                          =
            LoggerFactory.getLogger(LeadPaymentStatusService.class);

    private static final String           SELLER_USER_ID                                  = "crmUserId";

    private static final Integer          ROWS                                            = 200;

    @PostConstruct
    public void postConstructFields() {
 //       rawSellerService = applicationContext.getBean(RawSellerService.class);
 //       transactionService = applicationContext.getBean(TransactionService.class);
    }

    /**
     * 
     * @param leadId
     * @return
     */
    public ICRMLead getCityIdAndSaleTypeFromLead(Integer leadId) {
        if (leadId != null) {
            Selector selector = new Selector();
            selector.addAndEqualCondition(LEAD_ID, leadId);
            Set<String> fields = new HashSet<>();
            fields.add(SALE_TYPE_ID);
            fields.add(FIELD_CITY_ID);
            selector.setFields(fields);
            List<ICRMLead> leadDocuments = icrmHelper.getLeadDocuments(selector);

            if (CollectionUtils.isNotEmpty(leadDocuments)) {
                return leadDocuments.stream().findFirst().get();
            }
        }
        return null;
    }
    /**
     * 
     * @param leadPaymentDto
     * @param userInfo
     * @return
     */
    public List<LeadPaymentStatus> addLeadPaymentStatus(LeadPaymentDto leadPaymentDto, ActiveUser userInfo) {
        List<Integer> productIds = leadPaymentDto.getProductIds();
        Integer crmUserId;
        if (userInfo == null) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }
        else if (SecurityContextUtils.hasRole(userInfo, FINANCE_TEAM_ROLE)) {
            crmUserId = leadPaymentDto.getCrmUserId();
        }
        else if (SecurityContextUtils.hasRole(userInfo, SELLER_ROLE)) {
            /**
             * get logged in user Id as user is seller
             */
            crmUserId = Integer.parseInt(userInfo.getUserId());
        }
        else {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }
        List<LeadPaymentStatus> leadPaymentStatuses = new ArrayList<>();
        Date d = new Date();
        int amount;
        for (Integer t : productIds) {
            LeadPaymentStatus leadPaymentStatus = new LeadPaymentStatus();
            leadPaymentStatus.setLeadId(t);
            Integer cityId = null;
            Integer leadTypeId = null;
            amount = 0;
            if (SecurityContextUtils.hasRole(userInfo, FINANCE_TEAM_ROLE)) {
                LeadFinanceDto leadFinanceDto = getcityIdAndSaleTypeForLead(t);
                cityId = leadFinanceDto.getCityId();
                leadTypeId = getLeadBuyRentSaleType(leadFinanceDto.getSaleTypeId());
            }
            else {
                ICRMLead icrmLead = getCityIdAndSaleTypeFromLead(t);
                cityId = icrmLead.getCityId();
                leadTypeId = getLeadBuyRentSaleType(icrmLead.getSaleTypeId());
            }

            amount = getSellerDealPriceWithRetries(crmUserId, leadTypeId, cityId).getPrice().intValue();

            leadPaymentStatus.setAmount(amount);
            leadPaymentStatus.setLeadTypeId(leadTypeId);
            leadPaymentStatus.setAmount(amount);
            leadPaymentStatus.setCrmUserId(crmUserId);
            leadPaymentStatus.setStatusId(MasterLeadPaymentStatusEnum.PaymentPending.getId());
            leadPaymentStatus.setCreatedAt(d);
            leadPaymentStatus.setCreatedBy(Integer.parseInt(userInfo.getUserId()));
            leadPaymentStatus.setUpdatedAt(d);
            // leadPaymentStatus.setUpdatedBy(Integer.parseInt(userInfo.getUserId()));
            leadPaymentStatus.setSellerPaymentTypeId(SellerPaymentType.SellerPayment.getId());
            leadPaymentStatuses.add(leadPaymentStatus);
        }
        saveLeadPaymentStatusAsProductPaymentStatus(leadPaymentStatuses);
        if (SecurityContextUtils.hasRole(userInfo, FINANCE_TEAM_ROLE)) {
            applicationContext.getBean(LeadPaymentStatusService.class)
                    .sendPaymentPendingEmailAfterLeadClosed(crmUserId);
        }
        // update raw seller transaction dispositions
        // update the corresponding history
   //     rawSellerService.updateSellerDisposition(crmUserId, false);
        //----Divyanshu
        midlServiceHelper.updateSellerDisposition(crmUserId, false);
        return leadPaymentStatuses;
    }

    private int getLeadBuyRentSaleType(Integer saleTypeId) {
        int saleType = MasterLeadPaymentTypeEnum.Other.getId();
        if (SaleTypeEnum.isPrimary(saleTypeId) || SaleTypeEnum.isResale(saleTypeId)) {
            saleType = MasterLeadPaymentTypeEnum.Buy.getId();
        }
        else if (SaleTypeEnum.isRent(saleTypeId)) {
            saleType = MasterLeadPaymentTypeEnum.Rent.getId();
        }
        return saleType;
    }

    /**
     * @param leadPrePaymentDto
     * @param userInfo
     * @return
     */
    public List<LeadPaymentStatus> addPrePaymentLeadPaymentStatus(
            LeadPrePaymentDTO leadPrePaymentDto,
            ActiveUser userInfo) {

        ProductPrePaymentDto productPrePaymentDto = convertLeadPrePaymentDTOToProductPrePaymentDTO(leadPrePaymentDto);
        List<ProductPaymentStatus> productPaymentStatusList =
                productPaymentStatusService.addPrePayment(productPrePaymentDto, userInfo);
        return convertProductPaymentStatusListToLeadPaymentStatusList(productPaymentStatusList);
    }




    /**
     * @param leadPaymentStatus
     * @param leadCityDto
     * @param saleType
     * @return
     */
    @Transactional
    public List<LeadPaymentStatus> saveLeadPrePayment(
            LeadPaymentStatus leadPaymentStatus,
            LeadsCityDTO leadCityDto,
            String saleType) {

        int count = 0;
        List<LeadPaymentStatus> leadPaymentStatusList = new ArrayList<>();
        NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
        do {
            leadPaymentStatus.setId(null);
            LeadPaymentStatus leadPaymentStatusCopy = (LeadPaymentStatus) SerializationUtils.clone(leadPaymentStatus);
            List<LeadPaymentStatus> leadPaymentSavedStatuses =
                    leadPaymentStatusDao.save(Collections.singletonList(leadPaymentStatusCopy));

            LeadPaymentStatus savedLeadPaymentStatus = leadPaymentSavedStatuses.stream().findFirst().get();

            LeadPaymentStatusMetaAttributes lpsma = new LeadPaymentStatusMetaAttributes();
            lpsma.setLeadPaymentStatusId(savedLeadPaymentStatus.getId());

            lpsma.setCityId(leadCityDto.getCityId());

            leadPaymentStatusAttributeDao.save(lpsma);

            LeadPaymentStatus tmp = new LeadPaymentStatus();
            try {
                nullAwareBeanUtilsBean.copyProperties(tmp, savedLeadPaymentStatus);
            }
            catch (Exception e) {
                logger.error("Unable to copy saved lead Payment Status ", e);
            }

            tmp.setLeadPaymentStatusMetaAttributes(lpsma);
            leadPaymentStatusList.add(tmp);
            count++;
        }
        while (count < leadCityDto.getLeadCount());

        return leadPaymentStatusList;
    }

    /**
     * @param leadPaymentStatus
     * @param sellerUserId
     * @return
     */
    public List<LeadPaymentStatus> batchUpdatePrePaymentWithLeads(
            List<LeadPaymentStatus> leadPaymentStatuses,
            ActiveUser activeUser) {

        Set<Integer> leadPaymentIdSet = new HashSet<>();
        List<LeadPaymentStatus> leadStatusesToBePatched = new ArrayList<>(leadPaymentStatuses.size());
        for (LeadPaymentStatus leadPaymentStatus : leadPaymentStatuses) {
            int leadPaymentTypeId = MasterLeadPaymentTypeEnum
                    .getMasterLeadPaymentTypeEnumIdByName(leadPaymentStatus.getLeadPaymentType().name());
            int sellerUserId;
            if (SecurityContextUtils.hasRole(activeUser, FINANCE_TEAM_ROLE)) {
                sellerUserId = leadPaymentStatus.getCrmUserId();
            }
            else {
                /**
                 * get logged in user Id as user is seller
                 */
                sellerUserId = Integer.parseInt(activeUser.getUserId());
            }

            int leadCityId = getLeadById(leadPaymentStatus.getLeadId()).getCityId();
            SellerDealPrice sellerDealPrice = getSellerDealPriceWithRetries(
                    sellerUserId,
                    leadPaymentStatus.getLeadPaymentType().getId(),
                    leadCityId);
            
            validatePrepaymentLeadAssignment(leadPaymentStatus);

            int cityId = sellerDealPrice.getCityId();
            
            List<LeadPaymentStatus> availablePrepaymentForLeads =
                    leadPaymentStatusDao.findByCrmUserIdAndCityIdAndSaleTypeId(sellerUserId, cityId, leadPaymentTypeId);

            if (CollectionUtils.isEmpty(availablePrepaymentForLeads))
                throw new BadRequestException(
                        "Pre-Payment not done by seller for lead id : " + leadPaymentStatus.getLeadId());

            LeadPaymentStatus leadStatusToBePatched = null;
            for (LeadPaymentStatus availablePrepaymentForLead : availablePrepaymentForLeads) {
                if (!leadPaymentIdSet.contains(availablePrepaymentForLead.getId())) {
                    leadStatusToBePatched = availablePrepaymentForLead;
                    leadPaymentIdSet.add(availablePrepaymentForLead.getId());
                    break;
                }
            }
            if (leadStatusToBePatched == null){
                throw new BadRequestException("Duplicate lead Id: " + leadPaymentStatus.getLeadId());
            }
            leadStatusToBePatched.setLeadId(leadPaymentStatus.getLeadId());

            leadStatusesToBePatched.add(leadStatusToBePatched);
        }
        List<LeadPaymentStatus> savedLeadPaymentStatuses =
                saveLeadPaymentStatusAsProductPaymentStatus(leadStatusesToBePatched);

        // publish paid leads to sns
        if (CollectionUtils.isNotEmpty(savedLeadPaymentStatuses)) {
            List<Integer> leadIdsToBePublished = new ArrayList<>();
            for (LeadPaymentStatus lps : savedLeadPaymentStatuses) {
                if (lps.getStatusId() == MasterLeadPaymentStatusEnum.PaymentConfirmed.getId()
                        && lps.getLeadId() != null) {
                    leadIdsToBePublished.add(lps.getLeadId());
                }
            }
            publishToSns(leadIdsToBePublished);
        }

        return savedLeadPaymentStatuses;

    }

    public LeadPaymentStatus patchLeadWithPrepayment(
            LeadPaymentStatus leadPaymentStatus,
            int sellerUserId,
            ActiveUser activeUser) {
        leadPaymentStatus.setCrmUserId(sellerUserId);
        return batchUpdatePrePaymentWithLeads(Arrays.asList(leadPaymentStatus), activeUser).get(0);
    }

    private ICRMLead getLeadById(int leadId) {
        FIQLSelector fiqlSelector = new FIQLSelector();
        fiqlSelector.addAndConditionToFilter("id", leadId);
        List<ICRMLead> leads = icrmHelper.getLeadBySelector(fiqlSelector);
        if (CollectionUtils.isEmpty(leads))
            throw new BadRequestException("Invalid leadId");
        return leads.get(0);
    }

    /**
     * @param leadPaymentStatus
     * @return
     */
    private boolean validatePrepaymentLeadAssignment(LeadPaymentStatus leadPaymentStatus) {
        if (CollectionUtils.isNotEmpty(
                leadPaymentStatusDao.findByLeadIdAndStatusId(
                        leadPaymentStatus.getLeadId(),
                        MasterLeadPaymentStatusEnum.PaymentConfirmed.getId()))) {
            throw new BadRequestException("Payment already done for lead: " + leadPaymentStatus.getLeadId());
        }
        return true;
    }

    /**
     *
     * @param leadPaymentStatuses
     * @param crmUserId
     * @return
     */
    public List<LeadPaymentStatus> updateLeadStatus(List<LeadPaymentStatus> leadPaymentStatuses, Integer crmUserId) {
        return updateLeadStatusbyUserOrFinance(leadPaymentStatuses, crmUserId, false);
    }

    /**
     *
     * @param leadPaymentStatuses
     * @param crmUserId
     * @return
     */
    public List<LeadPaymentStatus> updateLeadStatus(
            List<LeadPaymentStatus> leadPaymentStatuses,
            ActiveUser activeUser) {
        int crmUserId;
        if (activeUser != null) {
            crmUserId = Integer.parseInt(activeUser.getUserId());
        }
        else {
            throw new BadRequestException(ResponseCodes.AUTHENTICATION_ERROR, "loggedIn user not found");
        }
        return convertProductPaymentStatusListToLeadPaymentStatusList(
                productPaymentStatusService.updateLeadStatusbyUserOrFinance(
                        convertLeadPaymentStatusListToProductPaymentStatusList(leadPaymentStatuses),
                        crmUserId,
                        SecurityContextUtils.hasRole(activeUser, FINANCE_TEAM_ROLE)));                
    }

    /**
     *
     * @param leadPaymentStatuses
     * @param crmUserId
     * @param hasRole
     * @return
     */
    private List<LeadPaymentStatus> updateLeadStatusbyUserOrFinance(
            List<LeadPaymentStatus> leadPaymentStatuses,
            Integer crmUserId,
            boolean canUpdateLead) {
        boolean updateLeadPayment = false;
        if (!canUpdateLead) {
            for (LeadPaymentStatus leadPaymentStatus : leadPaymentStatuses) {
                if (leadPaymentStatus.getCrmUserId().equals(crmUserId)) {
                    updateLeadPayment = true;
                    break;
                }
            }
        }
        if (canUpdateLead || updateLeadPayment) {
            return updateLeadPaymentStatus(leadPaymentStatuses, crmUserId, "UserOrFinance");
        }
        return new ArrayList<>();
    }

    /**
     *
     * @param leadPaymentStatuses
     * @param crmUserId
     * @return
     */
    public List<LeadPaymentStatus> updateLeadStatusbyLeadPaymentStatusId(
            List<LeadPaymentStatus> leadPaymentStatuses,
            Integer crmUserId) {
        return updateLeadPaymentStatus(leadPaymentStatuses, crmUserId, "PaymentStatusId");
    }

    private List<LeadPaymentStatus> updateLeadPaymentStatus(
            List<LeadPaymentStatus> leadPaymentStatuses,
            Integer crmUserId,
            String sourceFunction) {
        List<LeadPaymentStatus> updateLeadPaymentStatus = new ArrayList<>();
        LeadPaymentStatus thisLeadPaymentStatus;
        Date d = new Date();
        boolean updateDisposition = false;
        for (LeadPaymentStatus leadPaymentStatus : leadPaymentStatuses) {
            if (leadPaymentStatus.getCrmUserId().equals(crmUserId)) {
                if (sourceFunction.equals("UserOrFinance")) {
                    thisLeadPaymentStatus = leadPaymentStatusDao.findByLeadId(leadPaymentStatus.getLeadId());
                }
                else {
                    thisLeadPaymentStatus = leadPaymentStatusDao.findLeadPaymentStatusById(leadPaymentStatus.getId());
                }
                if (thisLeadPaymentStatus != null
                        && leadPaymentStatus.getStatusId() != thisLeadPaymentStatus.getStatusId()) {
                    thisLeadPaymentStatus.setStatusId(leadPaymentStatus.getStatusId());
                    thisLeadPaymentStatus.setTransactionId(leadPaymentStatus.getTransactionId());
                    thisLeadPaymentStatus.setUpdatedAt(d);
                    thisLeadPaymentStatus.setUpdatedBy(crmUserId);
                    updateLeadPaymentStatus.add(thisLeadPaymentStatus);
                    if (leadPaymentStatus.getStatusId() == MasterLeadPaymentStatusEnum.PaymentConfirmed.getId()) {
                        updateDisposition = true;
                    }
                }
            }
        }
        return updateDispositionAndNotify(updateLeadPaymentStatus, updateDisposition, crmUserId);
    }

    private List<LeadPaymentStatus> updateDispositionAndNotify(
            List<LeadPaymentStatus> updateLeadPaymentStatus,
            boolean updateDisposition,
            Integer crmUserId) {
        if (!updateLeadPaymentStatus.isEmpty()) {
            updateLeadPaymentStatus = leadPaymentStatusDao.save(updateLeadPaymentStatus);
            if (updateDisposition) {
                // update seller transaction disposition
       //         rawSellerService.updateSellerDisposition(crmUserId, true);
            	//------Divyanshu
            		midlServiceHelper.updateSellerDisposition(crmUserId, true);
            	List<Integer> leadIdsToBePublished = new ArrayList<>();
                for (LeadPaymentStatus lps : updateLeadPaymentStatus) {
                    if (lps.getStatusId() == MasterLeadPaymentStatusEnum.PaymentConfirmed.getId()
                            && lps.getLeadId() != null) {
                        leadIdsToBePublished.add(lps.getLeadId());
                    }
                }
                // push paid leadIds to sns
                publishToSns(leadIdsToBePublished);
                // mark this seller as eligible for home loan
                cyclopsServiceHelper.sellerEligibleForHomeloan(crmUserId);
            }
        }
        return updateLeadPaymentStatus;
    }

    private void publishToSns(List<Integer> leadIds) {
        if (CollectionUtils.isNotEmpty(leadIds)) {
            for (Integer leadId : leadIds) {
                try {
                    snsService.publishToTopic(leadPaymentConfirmedSnsTopic, leadId.toString());
                }
                catch (Exception e) {
                    logger.error("Failed to publish leadid : {} to sns", leadId);
                }
            }
        }
    }

    /**
     * 
     * @param leadId
     * @return
     */
    public LeadPaymentStatus getLeadPaymentStatusByLeadId(int leadId) {
        return leadPaymentStatusDao.findByLeadId(leadId);
    }

    /**
     * 
     * @param selector
     * @return
     */
    public PaginatedResponse<List<LeadPaymentStatus>> getDataBySelector(FIQLSelector selector) {
        PaginatedResponse<List<LeadPaymentStatus>> paginatedLeadPaymentStatuses =
                leadPaymentStatusDao.getLeadPaymentStatusBySelector(selector);
        List<LeadPaymentStatus> leadPaymentStatuses = paginatedLeadPaymentStatuses.getResults();
        setPaymentNumber(leadPaymentStatuses);
        setPrePaymentNumber(leadPaymentStatuses);
        setBuyerNumberAndName(leadPaymentStatuses);
        return new PaginatedResponse<>(leadPaymentStatuses, paginatedLeadPaymentStatuses.getTotalCount());
    }

    /**
     * 
     * @param leadPaymentStatuses
     */
    private void setBuyerNumberAndName(List<LeadPaymentStatus> leadPaymentStatuses) {
        if (CollectionUtils.isEmpty(leadPaymentStatuses)) {
            return;
        }
        List<Integer> leadIds = leadPaymentStatuses.stream().filter(lps -> lps.getLeadId() != null)
                .map(l -> l.getLeadId()).collect(Collectors.toList());
        List<LeadFinanceDto> financeDtos = getLeadFinaceDto(leadIds);
        Map<Integer, LeadFinanceDto> leadIdToFinance = new HashMap<>();
        financeDtos.forEach(f -> leadIdToFinance.put(f.getLeadId(), f));
        List<Integer> userIds = financeDtos.stream().map(f -> f.getBuyerId()).collect(Collectors.toList());
        Map<Integer, User> idToUser = new HashMap<>();
        if (!userIds.isEmpty()) {
            Optional<List<User>> users = userServiceHelper
                    .findUserByIdWithAllEmailsAndContactNumberWithRoles(userIds, ImmutableSet.of("contactNumbers"));

            if (users.isPresent() && !users.get().isEmpty()) {
                users.get().forEach(u -> idToUser.put(u.getId(), u));
            }
        }
        LeadFinanceDto financeDto;
        User user = null;
        for (LeadPaymentStatus leadPaymentStatus : leadPaymentStatuses) {
            user = null;
            financeDto = leadIdToFinance.get(leadPaymentStatus.getLeadId());
            if (financeDto != null) {
                user = idToUser.get(financeDto.getBuyerId());
            }
            if (user != null) {
                leadPaymentStatus.setBuyerName(user.getFullName());
                if (CollectionUtils.isNotEmpty(user.getContactNumbers())) {
                    List<String> buyerNumbers = user.getContactNumbers().stream()
                            .map(contactNumber -> contactNumber.getContactNumber()).collect(Collectors.toList());
                    leadPaymentStatus.setBuyerNumbers(buyerNumbers);
                }
            }
        }
    }

    /**
     * 
     * @param leadIds
     * @return
     */
    private List<LeadFinanceDto> getLeadFinaceDto(List<Integer> leadIds) {
        if (CollectionUtils.isNotEmpty(leadIds)) {
            return icrmHelper.getFinaceLeadDto(leadIds);
        }
        return Collections.emptyList();
    }

    /**
     * 
     * @param selector
     * @param activeUser
     * @return
     */
    public PaginatedResponse<List<LeadPaymentStatus>> getLeadPaymentDataUnderCro(
            FIQLSelector selector,
            ActiveUser activeUser) {
        if (!SecurityContextUtils.hasRole(activeUser, Access.Calling.getRoleName())) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }

        if (!SecurityContextUtils.hasRole(activeUser, Access.TeamManagement.getRoleName())
                && !isSelectorModifiedForCallingRole(selector, activeUser)) {
            return new PaginatedResponse<>();
        }
        return getDataBySelector(selector);
    }

    /**
     * 
     * @param selector
     * @param activeUser
     */
    private boolean isSelectorModifiedForCallingRole(FIQLSelector selector, ActiveUser activeUser) {
      //  List<Integer> userIds = rawSellerService.getAllSellerUserIdsUnderCro(activeUser);
        List<Integer> userIds = midlServiceHelper.getAllSellerUserIdsUnderCro(activeUser);
    
    	if (CollectionUtils.isNotEmpty(userIds)) {
            selector.addAndConditionToFilter(SELLER_USER_ID, userIds);
            return true;
        }
        return false;
    }

    private void setPaymentNumber(List<LeadPaymentStatus> leadPaymentStatuses) {

        List<Integer> leadIds = leadPaymentStatuses.stream().filter(lps -> lps.getLeadId() != null)
                .map(LeadPaymentStatus::getLeadId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(leadIds)) {
            return;
        }
        List<LeadPaymentDocument> leadPaymentsDocuments =
       //   Divyanshu      paymentAttributeDao.getPaymentNumberFromLeadIds(leadIds, TransactionType.SellerPayment.getId());
        		midlServiceHelper.getPaymentNumberFromLeadIds(leadIds,TransactionType.SellerPayment.getId() );

        if (CollectionUtils.isEmpty(leadPaymentsDocuments)) {
            return;
        }

        Map<Integer, String> leadIdwithPaymentNumber = new HashMap<>();
        Map<Integer, String> leadIdWithPaymentType = new HashMap<>();
        for (LeadPaymentDocument leadPaymentDocument : leadPaymentsDocuments) {
            Integer leadId = leadPaymentDocument.getLeadId();
            String paymentNumber = leadPaymentDocument.getPaymentNumber();
            if (paymentNumber != null) {
                leadIdwithPaymentNumber.put(leadId, paymentNumber);
                String paymentType = leadPaymentDocument.getPaymentType();
                leadIdWithPaymentType.put(leadId, paymentType);
            }

        }
        for (LeadPaymentStatus leadPaymentStatus : leadPaymentStatuses) {
            if (leadIdwithPaymentNumber.get(leadPaymentStatus.getLeadId()) != null) {
                leadPaymentStatus.setPaymentType(leadIdWithPaymentType.get(leadPaymentStatus.getLeadId()));
                leadPaymentStatus.setPaymentNumber(leadIdwithPaymentNumber.get(leadPaymentStatus.getLeadId()));
            }
        }

    }

    private void setPrePaymentNumber(List<LeadPaymentStatus> leadPaymentStatuses) {

        List<Integer> leadPaymentStatusIds =
                leadPaymentStatuses.stream().map(LeadPaymentStatus::getId).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(leadPaymentStatusIds)) {
            return;
        }
  //Divyanshu      List<LeadPaymentDocument> leadPaymentsDocuments = paymentAttributeDao.getPaymentNumberFromLeadPaymentStatusIds(
        List<LeadPaymentDocument> leadPaymentsDocuments = midlServiceHelper.getPaymentNumberFromLeadPaymentStatusIds(     
        leadPaymentStatusIds,
                TransactionType.SellerPrePayment.getId());

        if (CollectionUtils.isEmpty(leadPaymentsDocuments)) {
            return;
        }

        Map<Integer, String> leadIdwithPaymentNumber = new HashMap<>();
        Map<Integer, String> leadIdWithPaymentType = new HashMap<>();
        for (LeadPaymentDocument leadPaymentDocument : leadPaymentsDocuments) {
            Integer leadPaymentStatusId = leadPaymentDocument.getLeadId();
            String paymentNumber = leadPaymentDocument.getPaymentNumber();
            if (paymentNumber != null) {
                leadIdwithPaymentNumber.put(leadPaymentStatusId, paymentNumber);
                String paymentType = leadPaymentDocument.getPaymentType();
                leadIdWithPaymentType.put(leadPaymentStatusId, paymentType);
            }

        }
        for (LeadPaymentStatus leadPaymentStatus : leadPaymentStatuses) {
            if (leadIdwithPaymentNumber.get(leadPaymentStatus.getId()) != null) {
                leadPaymentStatus.setPaymentType(leadIdWithPaymentType.get(leadPaymentStatus.getId()));
                leadPaymentStatus.setPaymentNumber(leadIdwithPaymentNumber.get(leadPaymentStatus.getId()));
            }
        }

    }

    /**
     * 
     * @param selector
     * @param activeUser
     * @return
     */
    public PaginatedResponse<List<LeadPaymentStatus>> getLeadPaymentStatusBySelector(
            FIQLSelector selector,
            ActiveUser activeUser) {
        if (activeUser == null) {
            throw new BadRequestException(ResponseCodes.AUTHENTICATION_ERROR, "loggedIn user not found");
        }
        selector.addAndConditionToFilter("crmUserId==" + activeUser.getUserId());
        return leadPaymentStatusDao.getLeadPaymentStatusBySelector(selector);
    }

    /**
     * 
     * @param crmUserId
     * @return
     */
    @Cacheable(
            value = Constants.CacheName.SELLER_PAYMENT_CATEGORY,
            cacheManager = AppCachingConfig.SHORT_DURATION_CACHE)
    public List<LeadPaymentStatusDto> getLeadCountDistribution(Integer crmUserId) {
        List<Object[]> response = leadPaymentStatusDao.getLeadDistribution(crmUserId);
        return getLeadPaymentStatusDto(response);
    }
//@Divyanshu copying to productPaymentStatus 
    public HashMap<Integer, Integer> getLeadCounts(List<Integer> crmUserIds) {
        List<Object[]> response = leadPaymentStatusDao.getLeadCounts(crmUserIds);
        return getLeadPaymentCounts(response);
    }
  //@Divyanshu copying to productPaymentStatus
    public HashMap<Integer, Integer> getPrepaidAndPostPaidLeadCounts(Set<Integer> sellerIds) {
        List<Object[]> response = leadPaymentStatusDao.getPrepaidAndPostPaidLeadCounts(sellerIds);
        return getLeadPaymentCounts(response);
    }

    @Cacheable(
            value = Constants.CacheName.SELLER_PAYMENT_CATEGORY,
            cacheManager = AppCachingConfig.SHORT_DURATION_CACHE)
    public List<LeadPaymentStatusDto> getLeadCountDistributionSinceDate(Integer crmUserId, Date date) {
        List<Object[]> response = leadPaymentStatusDao.getLeadDistributionSinceDate(crmUserId, date);
        return getLeadPaymentStatusDto(response);
    }

    private List<LeadPaymentStatusDto> getLeadPaymentStatusDto(List<Object[]> objectArray) {
        List<LeadPaymentStatusDto> leadPaymentStatusDtos = new ArrayList<>();
        for (Object[] o : objectArray) {
            LeadPaymentStatusDto l = new LeadPaymentStatusDto();
            l.setId(Integer.parseInt(String.valueOf(o[0])));
            l.setStatusName(
                    MasterLeadPaymentStatusEnum
                            .getMasterLeadPaymentStatusEnumById(Integer.parseInt(String.valueOf(o[0]))).name());
            l.setLeadSaleType(
                    MasterLeadPaymentTypeEnum.getMasterLeadPaymentTypeEnumById(Integer.parseInt(String.valueOf(o[1])))
                            .name());
            l.setLeadCount(Long.parseLong(String.valueOf(o[2])));
            leadPaymentStatusDtos.add(l);
        }
        return leadPaymentStatusDtos;
    }

//Copied to pPSService
    private HashMap<Integer, Integer> getLeadPaymentCounts(List<Object[]> objectArray) {
        HashMap<Integer, Integer> leadPaymentsCount = new HashMap<>();
        for (Object[] o : objectArray) {
            int UserId = Integer.parseInt(String.valueOf(o[0]));
            int leadCount = Integer.parseInt(String.valueOf(o[1]));
            leadPaymentsCount.put(UserId, leadCount);
        }
        return leadPaymentsCount;
    }

    private LeadFinanceDto getcityIdAndSaleTypeForLead(Integer leadId) {
        if (leadId != null) {
            List<LeadFinanceDto> financeDto = icrmHelper.getFinaceLeadDto(Collections.singletonList(leadId));

            if (CollectionUtils.isNotEmpty(financeDto)) {
                return financeDto.stream().findFirst().get();
            }
        }
        return null;
    }

    /**
     * 
     * @param sellerUserId
     */
    @Async
    public void sendPaymentPendingEmailAfterLeadClosed(Integer sellerUserId) {
        sendEmailForRemainingLeadPayment(sellerUserId);
    }

    /**
     * 
     * @param sellerUserId
     * @return
     */
    public void sendEmailForRemainingLeadPayment(Integer sellerUserId) {
        List<LeadPaymentStatus> leadpaymentSatus = leadPaymentStatusDao
                .findByCrmUserIdAndStatusId(sellerUserId, MasterLeadPaymentStatusEnum.PaymentPending.getId());
        if (CollectionUtils.isNotEmpty(leadpaymentSatus)) {
        	//@Divyanshu TODO API already there.. can be called...
        	//		midlServiceHelper.sendNotificationForRemainingPayment(leadpaymentSatus, sellerUserId);
        	//         couponNotificationService.sendNotificationForRemainingPayment(leadpaymentSatus, sellerUserId);
        }
    }

    /**
     * Delete payment status for a lead
     * 
     * @param leadId
     * @param userInfo
     * @return
     */
    public String deleteLeadPaymentStatus(Integer leadId, ActiveUser userInfo) {

        if (userInfo == null || !SecurityContextUtils.hasRole(userInfo, FINANCE_TEAM_ROLE)) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }

        LeadPaymentStatus leadPaymentStatus = leadPaymentStatusDao.findByLeadId(leadId);

        if (leadPaymentStatus == null) {
            throw new BadRequestException("No payment status set for given lead");
        }
        else {

            if (leadPaymentStatus.getStatusId() != MasterLeadPaymentStatusEnum.PaymentPending.getId()) {
                throw new BadRequestException(
                        "Only those leads can be reverted which has payments status PaymentPending");
            }
        }

        icrmHelper.updateLead(leadId, COMMENT_ID_FOR_PAYMENT_STATUS_REVERSAL);
        deleteLeadPaymentStatusByLeadId(leadId);

        return String.format(PAYMENT_STATUS_REVERSAL_SUCCESS_MESSAGE_FORMAT, leadId);
    }

    /**
     * Delete payment status for lead payment status id
     * 
     * @param leadId
     * @param userInfo
     * @return
     */
    public String deleteLeadPaymentStatusByLeadPaymentId(List<Integer> leadPaymentStatusIds, ActiveUser userInfo) {

        if (userInfo == null) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }

        List<LeadPaymentStatus> leadPaymentStatusList =
                leadPaymentStatusDao.findLeadPaymentStatusByIds(leadPaymentStatusIds);

        deleteProductPaymentListForLeadPaymentList(leadPaymentStatusList);

        return String.format(
                LEAD_PAYMENT_STATUS_REVERSAL_BY_LEAD_PAYMENT_ID,
                Arrays.toString(
                        leadPaymentStatusList.stream().map(LeadPaymentStatus::getId).collect(Collectors.toList())
                                .toArray()));
    }


    /**
     * @param leadPaymentStatusList
     * @param userInfo
     * @return
     */
    public boolean updateLeadPaymentStatusByLeadPaymentId(
            List<LeadPaymentStatus> leadPaymentStatusList,
            ActiveUser userInfo) {

        if (userInfo == null) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }

        leadPaymentStatusList.stream().forEach(leadPaymentStatus -> {

            LeadPaymentStatus savedLeadPaymentStatus =
                    leadPaymentStatusDao.findLeadPaymentStatusById(leadPaymentStatus.getId());

            leadPaymentStatus.setLeadTypeId(savedLeadPaymentStatus.getLeadTypeId());

            NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
            try {
                nullAwareBeanUtilsBean.copyProperties(savedLeadPaymentStatus, leadPaymentStatus);
            }
            catch (Exception e) {
                logger.error("Unable to copy saved lead Payment Status ", e);
            }
            saveLeadPaymentStatusAsProductPaymentStatus(Collections.singletonList(leadPaymentStatus));
        });

        return true;
    }

    /**
     * @param id
     * @return
     */
    public LeadPaymentStatus findLeadPaymentStatusByLeadPaymentStatusId(Integer id) {

        return leadPaymentStatusDao.findLeadPaymentStatusById(id);
    }

    /**
     * @param leadIds
     * @param userInfo
     * @return
     */
    public String revertPrePaymentLeadIds(List<Integer> leadPaymentIdList, ActiveUser userInfo) {

        if (userInfo == null || !SecurityContextUtils.hasRole(userInfo, FINANCE_TEAM_ROLE)) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }

        revertPPSProductIdForLPSLeadIds(leadPaymentIdList);
        return String.format(LEAD_ID_REVERSAL_FOR_PREPAYMENT_LEADS, Arrays.toString(leadPaymentIdList.toArray()));
    }


    /**
     * Find list of all paid sellers with last payment date for lead type
     * 
     * @param leadTypeId
     * @return
     */
    public Map<Integer, Date> findPaidSellersWithLastPaymentDateForLeadType(int leadTypeId) {
        List<LeadPaymentStatusDto> lpsDTOList =
                leadPaymentStatusDao.findPaidSellersWithLastPaymentDateForLeadType(leadTypeId);
        return lpsDTOList.stream()
                .collect(Collectors.toMap(LeadPaymentStatusDto::getSellerId, LeadPaymentStatusDto::getPaymentDate));
    }

    /**
     * 
     * @param sellerId
     * @param leadTypeId
     * @return
     */
    @Cacheable(value = Constants.CacheName.SELLER_PAYMENT_CATEGORY)
    public Date findLastPaymentDateOfSeller(Integer sellerId, Integer leadTypeId) {
        return leadPaymentStatusDao.findLastPaymentDateOfSeller(sellerId, leadTypeId);
    }

    /**
     * 
     * @param userId
     * @param date
     * @return
     */
    @Cacheable(value = Constants.CacheName.SELLER_PAYMENT_CATEGORY)
    public List<LeadPaymentStatusDto> getPaidLeadsDistribution(int userId, Date date) {
        List<Object[]> response = leadPaymentStatusDao.getPaidLeadsDistribution(userId, date);
        return getLeadPaymentStatusDto(response);
    }

    /**
     * 
     * @param selector
     * @return
     */
    public PaginatedResponse<List<LeadPaymentStatus>> getLeadPaymentStatusBySelector(FIQLSelector selector) {
        PaginatedResponse<List<LeadPaymentStatus>> paginatedLeadPaymentStatuses =
                leadPaymentStatusDao.getLeadPaymentStatusBySelector(selector);
        List<LeadPaymentStatus> leadPaymentStatuses = paginatedLeadPaymentStatuses.getResults();
        return new PaginatedResponse<>(leadPaymentStatuses, paginatedLeadPaymentStatuses.getTotalCount());
    }

    /**
     * 
     * @param userId
     * @param date
     * @param leadTypeId
     * @return
     */
    public Integer getCountOfLeadsDisclosed(int userId, Date date, int leadTypeId) {
        return leadPaymentStatusDao.getCountOfLeadsDisclosed(userId, date, leadTypeId);
    }

    private SellerDealPrice getSellerDealPriceWithRetries(
            Integer crmUserId,
            Integer saleTypeId,
            Integer cityId) {
        List<SellerDealPrice> sdpList = null;
        boolean dataFetched = false;

        SellerDealPriceResponseDTO results = null;
        int retryAttempts = 0;
        while (!dataFetched && retryAttempts++ < 3) {
            try {
                results = cyclopsServiceHelper.getSellerDealPrice(crmUserId);
            }
            catch (Exception e) {
                logger.error("Error fetching deal price from Cyclops", e);
            }
            if (results != null) {
                sdpList = results.getSellerDealPriceList();
                dataFetched = true;
            }
        }
        if(CollectionUtils.isEmpty(sdpList)){
            throw new ProAPIException("Could not fetch deal price from Cyclops");
        }
        return CyclopsServiceHelper.getSellerDealPriceForCityIdSaleTypeId(sdpList, cityId, saleTypeId);
    }
    
    public List<LeadPaymentStatus> getLeadPaymentListFromDate(
            String previousDate,
            Integer sellerId,
            Integer saleTypeId) {
        Integer start = 0;
        long totalCount = -1;
        List<LeadPaymentStatus> responseList = new ArrayList<>();
        List<LeadPaymentStatus> leadPaymentList = new ArrayList<>();

        FIQLSelector fiqlSelector = new FIQLSelector();
        fiqlSelector.addAndConditionToFilter("statusId", MasterLeadPaymentStatusEnum.PaymentConfirmed.getId());
        fiqlSelector.addAndConditionToFilter("payment.updatedAt=gt=" + previousDate);
        fiqlSelector.addAndConditionToFilter("transactionId!=");

        if (sellerId != null) {
            fiqlSelector.addAndConditionToFilter("crmUserId", sellerId);
        }
        if (saleTypeId != null) {
            fiqlSelector.addAndConditionToFilter("leadTypeId", saleTypeId);
        }

        fiqlSelector.setRows(ROWS);
        // fiqlSelector.setSort("crmUserId,payment.updatedAt");
        fiqlSelector.addSortDESC("crmUserId,payment.updatedAt");
        fiqlSelector.setFields("crmUserId,leadTypeId,leadId,payment,createdAt,updatedAt");

        do {
            PaginatedResponse<List<LeadPaymentStatus>> response = getLeadPaymentStatusBySelector(fiqlSelector);
            totalCount = response.getTotalCount();
            responseList = response.getResults();

            start += ROWS;
            leadPaymentList.addAll(responseList);
            fiqlSelector.setStart(start + 1);

        }
        while (start + 1 < totalCount);

        leadPaymentList.sort(
                (LeadPaymentStatus l1, LeadPaymentStatus l2) -> l2.getPayment().getUpdatedAt()
                        .compareTo(l1.getPayment().getUpdatedAt()));
        return leadPaymentList;
    }

    public List<ProductPaymentStatus> convertLeadPaymentStatusListToProductPaymentStatusList(
            List<LeadPaymentStatus> leadPaymentStatusList) {
        List<ProductPaymentStatus> productPaymentStatusList = new ArrayList<>();

        for (LeadPaymentStatus lps : leadPaymentStatusList) {
            productPaymentStatusList.add(convertLeadPaymentStatusToProductPaymentStatus(lps));
        }
        return productPaymentStatusList;
    }

    public ProductPaymentStatus convertLeadPaymentStatusToProductPaymentStatus(LeadPaymentStatus lps) {
        ProductPaymentStatus pps;
        if(lps.getId() != null){
            pps = productPaymentStatusService.findById(lps.getId());
        }
        else{
            pps = new ProductPaymentStatus();    
        }
        if(lps.getSellerPaymentTypeId() != null){
            pps.setSellerPaymentTypeId(lps.getSellerPaymentTypeId());  
        }
        pps.setId(lps.getId());
        pps.setCrmUserId(lps.getCrmUserId());
        pps.setTransactionId(lps.getTransactionId());
        pps.setStatusId(lps.getStatusId());
        pps.setProductTypeId(MasterEntityTypes.lead.getId());
        pps.setProductId(lps.getLeadId());
        pps.setSaleTypeId(lps.getLeadTypeId());
        pps.setAmount(lps.getAmount());
        pps.setCreatedAt(lps.getCreatedAt());
        pps.setCreatedBy(lps.getCreatedBy());
        pps.setUpdatedAt(lps.getUpdatedAt());
        pps.setUpdatedBy(lps.getUpdatedBy());

        return pps;
    }
    
    public List<LeadPaymentStatus> convertProductPaymentStatusListToLeadPaymentStatusList(
            List<ProductPaymentStatus> productPaymentStatusList) {
        List<LeadPaymentStatus> leadPaymentStatusList = new ArrayList<>();
        for (ProductPaymentStatus pps : productPaymentStatusList) {
            leadPaymentStatusList.add(convertProductPaymentStatusToLeadPaymentStatus(pps));
        }
        return leadPaymentStatusList;
    }

    public LeadPaymentStatus convertProductPaymentStatusToLeadPaymentStatus(ProductPaymentStatus pps) {
        LeadPaymentStatus lps = new LeadPaymentStatus();
        lps.setId(pps.getId());
        lps.setCrmUserId(pps.getCrmUserId());
        lps.setTransactionId(pps.getTransactionId());
        lps.setStatusId(pps.getStatusId());
        lps.setLeadId(pps.getProductId());
        lps.setLeadTypeId(pps.getSaleTypeId());
        lps.setAmount(pps.getAmount());
        lps.setCreatedAt(pps.getCreatedAt());
        lps.setCreatedBy(pps.getCreatedBy());
        lps.setUpdatedAt(pps.getUpdatedAt());
        lps.setUpdatedBy(pps.getUpdatedBy());

        return lps;
    }

    public List<LeadPaymentStatus> saveLeadPaymentStatusAsProductPaymentStatus(List<LeadPaymentStatus> leadPaymentStatus) {
        List<ProductPaymentStatus> ppsList = convertLeadPaymentStatusListToProductPaymentStatusList(leadPaymentStatus);
        return convertProductPaymentStatusListToLeadPaymentStatusList(
                productPaymentStatusService.saveProductPaymentStatus(ppsList));
    }

    /**
     * 
     * @param leadId
     * @return
     */
    public LeadPaymentStatus findByLeadId(int leadId) {
        return leadPaymentStatusDao.findByLeadId(leadId);
    }
    
    private void deleteLeadPaymentStatusByLeadId(Integer leadId) {
        productPaymentStatusService.deleteByProductTypeIdAndProductId(MasterEntityTypes.lead.getId(), leadId);
    }
    
    private void deleteProductPaymentListForLeadPaymentList(List<LeadPaymentStatus> leadPaymentStatusList) {
        List<ProductPaymentStatus> ppsList = convertLeadPaymentStatusListToProductPaymentStatusList(leadPaymentStatusList);
        productPaymentStatusService.deleteProductPaymentStatusList(ppsList);
    }    

    private void revertPPSProductIdForLPSLeadIds(List<Integer> productPaymentIdList) {
        productPaymentStatusService.setProductPaymentStatusProductIdsNull(productPaymentIdList);
    }

    private ProductPrePaymentDto convertLeadPrePaymentDTOToProductPrePaymentDTO(LeadPrePaymentDTO leadPrePaymentDto) {
        ProductPrePaymentDto productPrePaymentDTO = new ProductPrePaymentDto();
        productPrePaymentDTO.setCrmUserId(leadPrePaymentDto.getCrmUserId());
        productPrePaymentDTO.setLeadCityDtoList(leadPrePaymentDto.getLeadCityDtoList());
        productPrePaymentDTO.setProductPaymentStatusId(leadPrePaymentDto.getLeadPaymentStatusId());
        productPrePaymentDTO.setSaleType(leadPrePaymentDto.getSaleType());
        productPrePaymentDTO.setTransactionId(leadPrePaymentDto.getTransactionId());
        productPrePaymentDTO.setProductType(MasterEntityTypes.lead);
        productPrePaymentDTO.setSellerPaymentType(SellerPaymentType.SellerPrePayment);
        return productPrePaymentDTO;
        
    }
    /**
     * TODO : Move to new table product_payment_status
     * @param leadPaymentStatusPatchDTO
     * @return
     */

    @Deprecated
    public List<LeadPaymentStatus> getLeadPaymentHistoryBySeller(Integer sellerId) {
        Integer start = 0;
        long totalCount = -1;
        List<LeadPaymentStatus> responseList = new ArrayList<>();
        List<LeadPaymentStatus> leadPaymentList = new ArrayList<>();
        FIQLSelector fiqlSelector = new FIQLSelector();
        fiqlSelector.addAndConditionToFilter("statusId", MasterLeadPaymentStatusEnum.PaymentConfirmed.getId());
        fiqlSelector.addAndConditionToFilter("crmUserId", sellerId);
        fiqlSelector.setRows(ROWS);
        do {
            PaginatedResponse<List<LeadPaymentStatus>> response = getLeadPaymentStatusBySelector(fiqlSelector);
            totalCount = response.getTotalCount();
            responseList = response.getResults();
            start += ROWS;
            leadPaymentList.addAll(responseList);
            fiqlSelector.setStart(start + 1);
        }
        while (start + 1 < totalCount);
        return leadPaymentList;
    }


    public Integer updateLeadAmount(LeadPaymentStatusPatchDTO leadPaymentStatusPatchDTO) {
        return productPaymentStatusService.updateAmountForSellerSaleType(
                leadPaymentStatusPatchDTO.getSellerId(),
                leadPaymentStatusPatchDTO.getLeadTypeId(),
                MasterEntityTypes.lead.getId(),
                leadPaymentStatusPatchDTO.getCreatedDateLessThan(),
                leadPaymentStatusPatchDTO.getAmount());
    }
}
