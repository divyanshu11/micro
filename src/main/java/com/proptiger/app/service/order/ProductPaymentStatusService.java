package com.proptiger.app.service.order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//import com.proptiger.app.model.transaction.MasterProductPaymentStatusEnum;
//import com.proptiger.app.model.transaction.ProductPaymentStatusAttributes;
import com.proptiger.app.repo.order.ProductPaymentStatusAttributesDao;
import com.proptiger.app.repo.order.ProductPaymentStatusDao;
//import com.proptiger.app.service.cms.listing.PremiumListingService;
import com.proptiger.core.constants.ResponseCodes;
import com.proptiger.core.constants.ResponseErrorMessages;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.dto.order.LeadsCityDTO;
import com.proptiger.core.dto.order.ProductPrePaymentDto;
import com.proptiger.core.enums.Domain;
import com.proptiger.core.enums.DomainObject;
import com.proptiger.core.enums.MasterEntityTypes;
import com.proptiger.core.enums.security.UserRole;
import com.proptiger.core.exception.ProAPIException;
import com.proptiger.core.exception.UnauthorizedException;
import com.proptiger.core.helper.CyclopsServiceHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.cms.Listing;
import com.proptiger.core.model.cyclops.MasterSellerLeadPrice;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentStatusEnum;
import com.proptiger.core.model.enums.transaction.MasterLeadPaymentTypeEnum;
import com.proptiger.core.model.enums.transaction.MasterProductPaymentStatusEnum;
import com.proptiger.core.model.transaction.ProductPaymentStatus;
import com.proptiger.core.model.transaction.ProductPaymentStatusAttributes;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.Selector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.service.SNSService;
import com.proptiger.core.util.NullAwareBeanUtilsBean;
import com.proptiger.core.util.SecurityContextUtils;
//import com.proptiger.data.dto.order.LeadsCityDTO;
//import com.proptiger.data.dto.order.ProductPrePaymentDto;
// import com.proptiger.data.repo.order.ProductPaymentStatusAttributesDao;
//import com.proptiger.data.repo.order.ProductPaymentStatusDao;
//import com.proptiger.data.service.cms.listing.ListingService;
//import com.proptiger.data.service.cms.listing.PremiumListingService;
//import com.proptiger.data.service.order.ProductPaymentStatus;
//import com.proptiger.data.service.order.ProductPaymentStatusAttributes;
//import com.proptiger.data.service.order.ProductPaymentStatusService;
//import com.proptiger.marketforce.data.service.RawSellerService;

@Service
public class ProductPaymentStatusService {

	//@Divyanshu
	@Autowired
	private MIDLServiceHelper midlServiceHelper;
	@Autowired
    private CyclopsServiceHelper              cyclopsServiceHelper;

    @Autowired
    private ProductPaymentStatusDao           productPaymentStatusDao;

    @Autowired
    private ProductPaymentStatusAttributesDao productPaymentStatusAttributesDao;
    
    @Autowired
    private ApplicationContext                applicationContext;

    @Autowired
    private SNSService                        snsService;
//Api Called..
//    @Autowired
//    private ListingService                    listingService;
    
  //  @Autowired
  //  private PremiumListingService             premiumListingService;

    private ProductPaymentStatusService       productPaymentStatusService;
//Api Called
   // private RawSellerService                  rawSellerService;

    private static Logger                     logger = LoggerFactory.getLogger(ProductPaymentStatusService.class);

    private static final Integer          ROWS                                            = 200;

    @Value("${lead.payment.confirmed.sns.topic.arn}")
    private String                            leadPaymentConfirmedSnsTopic;
    
    @PostConstruct
    private void postConstructFields() {
        productPaymentStatusService = applicationContext.getBean(ProductPaymentStatusService.class);
  //      rawSellerService = applicationContext.getBean(RawSellerService.class);
    }

    
    public List<ProductPaymentStatus> addPrePaymentList(
            List<ProductPrePaymentDto> productPrePaymentDTOList,
            ActiveUser userInfo) {
        List<ProductPaymentStatus> productPaymentStatuses = new ArrayList<>();
        for (ProductPrePaymentDto productPrePaymentDTO : productPrePaymentDTOList) {
            productPaymentStatuses.addAll(addPrePayment(productPrePaymentDTO, userInfo));
        }
        return productPaymentStatuses;
    }

    public List<ProductPaymentStatus> addPrePayment(
            ProductPrePaymentDto productPrePaymentDTO,
            ActiveUser userInfo) {

        List<ProductPaymentStatus> productPaymentStatuses = new ArrayList<>();

        Integer crmUserId;
        if (userInfo == null) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }
        else if (SecurityContextUtils.hasRole(userInfo, UserRole.Finance.name())) {
            crmUserId = productPrePaymentDTO.getCrmUserId();
        }
        else {
            /**
             * get logged in user Id as user is seller
             */
            crmUserId = Integer.parseInt(userInfo.getUserId());
        }

        Integer entryBy = Integer.parseInt(userInfo.getUserId());
        Integer productPaymentStatusId = MasterProductPaymentStatusEnum.PaymentConfirmed.getId();

        if (null != productPrePaymentDTO.getProductPaymentStatusId()) {
            productPaymentStatusId = productPrePaymentDTO.getProductPaymentStatusId();
        }
        Integer productCategoryId =
                MasterLeadPaymentTypeEnum.getMasterLeadPaymentTypeEnumIdByName(productPrePaymentDTO.getSaleType());

        ProductPaymentStatus productPaymentStatus = new ProductPaymentStatus();
        productPaymentStatus.setCrmUserId(crmUserId);
        productPaymentStatus.setStatusId(productPaymentStatusId);
        productPaymentStatus.setCreatedAt(new Date());
        productPaymentStatus.setCreatedBy(entryBy);
        productPaymentStatus.setUpdatedBy(entryBy);
        productPaymentStatus.setUpdatedAt(new Date());
        productPaymentStatus.setProductTypeId(productPrePaymentDTO.getProductType().getId());
        productPaymentStatus.setTransactionId(productPrePaymentDTO.getTransactionId());
        productPaymentStatus.setSaleTypeId(productCategoryId);
        productPaymentStatus.setProductTypeId(productPrePaymentDTO.getProductType().getId());
        productPaymentStatus.setSellerPaymentTypeId(productPrePaymentDTO.getSellerPaymentType().getId());

        if (productPrePaymentDTO.getProductType().equals(MasterEntityTypes.lead)) {
            productPaymentStatuses = processLeadPrePayment(
                    productPaymentStatus,
                    productPrePaymentDTO,
                    crmUserId,
                    entryBy,
                    productPaymentStatusId,
                    productCategoryId);
        }
        else if (productPrePaymentDTO.getProductType().equals(MasterEntityTypes.listing)) {
            productPaymentStatuses = processListingPrePayment(
                    productPaymentStatus,
                    productPrePaymentDTO,
                    crmUserId,
                    entryBy,
                    productPaymentStatusId,
                    productCategoryId);
        }

        return productPaymentStatuses;
    }

    private List<ProductPaymentStatus> processListingPrePayment(
            ProductPaymentStatus productPaymentStatus,
            ProductPrePaymentDto productPrePaymentDTO,
            Integer crmUserId,
            Integer entryBy,
            Integer productPaymentStatusId,
            Integer productCategoryId) {

        Selector selector = new Selector();
        selector.addAndEqualCondition("listingId", productPrePaymentDTO.getProductId());
//@Divyanshu     List<Listing> listingList = listingService.getListing(selector, false, Domain.Makaan).getResults();
        List<Listing> listingList = midlServiceHelper.getListings(selector, false, Domain.Makaan);
        if(CollectionUtils.isEmpty(listingList)){
            throw new ProAPIException("Could not fetch listing from solr");
        }
        productPaymentStatus.setAmount(midlServiceHelper.getPremiumListingDealPrice(listingList.get(0)));
  //@Divyanshu      productPaymentStatus.setAmount(premiumListingService.getPremiumListingDealPrice(listingList.get(0)));
        productPaymentStatus.setProductId(productPrePaymentDTO.getProductId());

        return productPaymentStatusDao.save(Collections.singletonList(productPaymentStatus));
    }

    private List<ProductPaymentStatus> processLeadPrePayment(
            ProductPaymentStatus productPaymentStatus,
            ProductPrePaymentDto productPrePaymentDTO,
            Integer crmUserId,
            Integer entryBy,
            Integer productPaymentStatusId,
            Integer productCategoryId) {

        List<ProductPaymentStatus> productPaymentStatuses = new ArrayList<>();

        for (LeadsCityDTO leadCityDto : productPrePaymentDTO.getLeadCityDtoList()) {

            ProductPaymentStatus productPaymentStatus2 =
                    (ProductPaymentStatus) SerializationUtils.clone(productPaymentStatus);

            MasterSellerLeadPrice masterSellerLeadPrice = cyclopsServiceHelper
                    .getMasterSellerLeadPriceV2(leadCityDto.getCityId(), null, productPrePaymentDTO.getSaleType());

            productPaymentStatus2.setAmount(masterSellerLeadPrice.getPriceBreakupDetails().getBasePrice().intValue());
            productPaymentStatuses.addAll(
                    productPaymentStatusService.saveLeadPrePayment(
                            productPaymentStatus2,
                            leadCityDto,
                            productPrePaymentDTO.getSaleType()));

        }

        return productPaymentStatuses;
    }

    @Transactional
    public List<ProductPaymentStatus> saveLeadPrePayment(
            ProductPaymentStatus productPaymentStatus,
            LeadsCityDTO leadCityDto,
            String saleType) {
        int count = 0;
        List<ProductPaymentStatus> productPaymentStatusList = new ArrayList<>();
        NullAwareBeanUtilsBean nullAwareBeanUtilsBean = new NullAwareBeanUtilsBean();
        do {
            productPaymentStatus.setId(null);
            ProductPaymentStatus productPaymentStatusCopy =
                    (ProductPaymentStatus) SerializationUtils.clone(productPaymentStatus);
            List<ProductPaymentStatus> productPaymentSavedStatuses =
                    productPaymentStatusDao.save(Collections.singletonList(productPaymentStatusCopy));

            ProductPaymentStatus savedProductPaymentStatus = productPaymentSavedStatuses.stream().findFirst().get();
            ProductPaymentStatusAttributes ppsma = new ProductPaymentStatusAttributes();

            ppsma.setLeadPaymentStatusId(savedProductPaymentStatus.getId());
            ppsma.setAttributeKey(DomainObject.city.name());
            ppsma.setAttributeValue(Integer.toString(leadCityDto.getCityId()));
            productPaymentStatusAttributesDao.save(ppsma);

            ProductPaymentStatus tmp = new ProductPaymentStatus();
            try {
                nullAwareBeanUtilsBean.copyProperties(tmp, savedProductPaymentStatus);
            }
            catch (Exception e) {
                logger.error("Unable to copy saved ProductPaymentStatus ", e);
            }

            tmp.setLeadPaymentStatusAttributes(ppsma);
            productPaymentStatusList.add(tmp);
            count++;
        }
        while (count < leadCityDto.getLeadCount());

        return productPaymentStatusList;
    }
    public PaginatedResponse<List<ProductPaymentStatus>> getProductPaymentStatusBySelector(FIQLSelector selector){
        return productPaymentStatusDao.getProductPaymentStatusBySelector(selector);
    }
    
    public List<ProductPaymentStatus> getLeadPaymentListFromDate(
            String dateAfter,
            Integer sellerId,
            Integer saleTypeId) {
        Integer start = 0;
        long totalCount = -1;
        List<ProductPaymentStatus> responseList = new ArrayList<>();
        List<ProductPaymentStatus> productPaymentList = new ArrayList<>();

        FIQLSelector fiqlSelector = new FIQLSelector();
        fiqlSelector.addAndConditionToFilter("statusId", MasterProductPaymentStatusEnum.PaymentConfirmed.getId());
        fiqlSelector.addAndConditionToFilter("paymentDate=gt=" + dateAfter);
        fiqlSelector.addAndConditionToFilter("transactionId!=");

        if (sellerId != null) {
            fiqlSelector.addAndConditionToFilter("crmUserId", sellerId);
        }
        if (saleTypeId != null) {
            fiqlSelector.addAndConditionToFilter("saleTypeId", saleTypeId);
        }

        fiqlSelector.setRows(ROWS);
        // fiqlSelector.setSort("crmUserId,payment.updatedAt");
        fiqlSelector.addSortDESC("crmUserId,paymentDate");
        
        // TODO - add if required
        //fiqlSelector.setFields("crmUserId,productCategoryId,leadId,payment,createdAt,updatedAt");

        do {
            PaginatedResponse<List<ProductPaymentStatus>> response = getProductPaymentStatusBySelector(fiqlSelector);
            totalCount = response.getTotalCount();
            responseList = response.getResults();

            start += ROWS;
            productPaymentList.addAll(responseList);
            fiqlSelector.setStart(start + 1);

        }
        while (start + 1 < totalCount);

        productPaymentList.sort(
                (ProductPaymentStatus l1, ProductPaymentStatus l2) -> l2.getPaymentDate()
                        .compareTo(l1.getPaymentDate()));
        return productPaymentList;
    }
    public List<ProductPaymentStatus> updateLeadStatus(List<ProductPaymentStatus> productPaymentStatuses, Integer crmUserId) {
        return updateLeadStatusbyUserOrFinance(productPaymentStatuses, crmUserId, false);
    }

    public List<ProductPaymentStatus> updateLeadStatusbyUserOrFinance(
            List<ProductPaymentStatus> productPaymentStatuses,
            Integer crmUserId,
            boolean canUpdateLead) {
        boolean updateLeadPayment = false;
        if (!canUpdateLead) {
            for (ProductPaymentStatus leadPaymentStatus : productPaymentStatuses) {
                if (leadPaymentStatus.getCrmUserId().equals(crmUserId)) {
                    updateLeadPayment = true;
                    break;
                }
            }
        }
        if (canUpdateLead || updateLeadPayment) {
            return updateLeadPaymentStatus(productPaymentStatuses, crmUserId, "UserOrFinance");
        }
        return new ArrayList<>();
    }

    private List<ProductPaymentStatus> updateLeadPaymentStatus(
            List<ProductPaymentStatus> productPaymentStatuses,
            Integer crmUserId,
            String sourceFunction) {
        List<ProductPaymentStatus> updateProductPaymentStatus = new ArrayList<>();
        ProductPaymentStatus thisProductPaymentStatus;
        Date d = new Date();
        boolean updateDisposition = false;
        for (ProductPaymentStatus productPaymentStatus : productPaymentStatuses) {
            if (productPaymentStatus.getCrmUserId().equals(crmUserId)) {
                if (sourceFunction.equals("UserOrFinance")) {
                    thisProductPaymentStatus = productPaymentStatusDao.findByProductTypeIdAndProductId(
                            productPaymentStatus.getProductTypeId(),
                            productPaymentStatus.getProductId());
                }
                else {
                    thisProductPaymentStatus = productPaymentStatusDao.findById(productPaymentStatus.getId());
                }
                if (thisProductPaymentStatus != null
                        && productPaymentStatus.getStatusId() != thisProductPaymentStatus.getStatusId()) {
                    thisProductPaymentStatus.setStatusId(productPaymentStatus.getStatusId());
                    thisProductPaymentStatus.setTransactionId(productPaymentStatus.getTransactionId());
                    thisProductPaymentStatus.setUpdatedAt(d);
                    thisProductPaymentStatus.setUpdatedBy(crmUserId);
                    thisProductPaymentStatus.setPaymentDate(productPaymentStatus.getPaymentDate());
                    updateProductPaymentStatus.add(thisProductPaymentStatus);
                    if (productPaymentStatus.getStatusId() == MasterLeadPaymentStatusEnum.PaymentConfirmed.getId()) {
                        updateDisposition = true;
                    }
                }
            }
        }
        return updateSellerDispositionAndNotify(updateProductPaymentStatus, updateDisposition, crmUserId);
    }
//OK TESTED
    private List<ProductPaymentStatus> updateSellerDispositionAndNotify(
            List<ProductPaymentStatus> updateProductPaymentStatus,
            boolean updateDisposition,
            Integer crmUserId) {
        boolean eligibleForHomeLoan = false;
        if (!updateProductPaymentStatus.isEmpty()) {
            updateProductPaymentStatus = productPaymentStatusDao.save(updateProductPaymentStatus);
            productPaymentStatusDao.flush();
            if (updateDisposition) {
                // update seller transaction disposition
 //@Divyanshu          rawSellerService.updateSellerDisposition(crmUserId, true);
                midlServiceHelper.updateSellerDisposition(crmUserId, true);
            	List<Integer> leadIdsToBePublished = new ArrayList<>();
                for (ProductPaymentStatus pps : updateProductPaymentStatus) {
                    if (pps.getStatusId() == MasterLeadPaymentStatusEnum.PaymentConfirmed.getId()
                            && pps.getProductTypeId().equals(MasterEntityTypes.lead.getId())
                            && pps.getProductId() != null) {
                        leadIdsToBePublished.add(pps.getProductId());
                        eligibleForHomeLoan = true;
                    }
                }
                // push paid leadIds to sns
                publishToSns(leadIdsToBePublished);
                // mark this seller as eligible for home loan
                if(eligibleForHomeLoan){
                    cyclopsServiceHelper.sellerEligibleForHomeloan(crmUserId);
                }
            }
        }
        return updateProductPaymentStatus;
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
     * @param productPaymentStatuses
     * @param crmUserId
     * @return
     */
    public List<ProductPaymentStatus> updateLeadStatusbyLeadPaymentStatusId(
            List<ProductPaymentStatus> productPaymentStatuses,
            Integer crmUserId) {
        return updateLeadPaymentStatus(productPaymentStatuses, crmUserId, "PaymentStatusId");
    }


    public List<ProductPaymentStatus> updateProductPaymentStatus(
            List<ProductPaymentStatus> productPaymentStatuses,
            int crmUserId) {
        return updateLeadPaymentStatus(productPaymentStatuses, crmUserId, "PaymentStatusId");
    }
    
    public ProductPaymentStatus findById(Integer id){
        return productPaymentStatusDao.findById(id);
    }
    
    public List<ProductPaymentStatus> findByIdIn(Collection<Integer> ids){
        return productPaymentStatusDao.findByIdIn(ids);
    }
    
    public ProductPaymentStatus findByProductTypeIdAndProductId(Integer productTypeId, Integer productId) {
        return productPaymentStatusDao.findByProductTypeIdAndProductId(productTypeId, productId);
    }
    
    public List<ProductPaymentStatus> saveProductPaymentStatus(List<ProductPaymentStatus> productPaymentStatusList) {
        return productPaymentStatusDao.save(productPaymentStatusList);
    }


    @Transactional
    public void deleteByProductTypeIdAndProductId(int productTypeId, Integer productId) {
        Integer ppsId = productPaymentStatusDao.findByProductTypeIdAndProductId(productTypeId, productId).getId();
        productPaymentStatusAttributesDao.deleteByProductPaymentStatusId(Collections.singletonList(ppsId));
        productPaymentStatusDao.deleteByProductTypeIdAndProductId(productTypeId, productId);
        
    }

    @Transactional
    public void deleteProductPaymentStatusList(List<ProductPaymentStatus> ppsList) {
        List<Integer> ppsIds = ppsList.stream().map(ProductPaymentStatus::getId).collect(Collectors.toList());
        productPaymentStatusAttributesDao.deleteByProductPaymentStatusId(ppsIds);
        productPaymentStatusDao.deleteInBatch(ppsList);
    }

    public void setProductPaymentStatusProductIdsNull(List<Integer> productPaymentIdList) {
        productPaymentStatusDao.setProductIdNull(productPaymentIdList);
    }


    public Integer updateAmountForSellerSaleType(
            Integer sellerId,
            Integer saleTypeId,
            Integer productTypeId,
            Date createdDateLessThan,
            Integer amount) {
        return productPaymentStatusDao
                .updateAmountForSellerSaleType(sellerId, saleTypeId, productTypeId, createdDateLessThan, amount);
    }
}
