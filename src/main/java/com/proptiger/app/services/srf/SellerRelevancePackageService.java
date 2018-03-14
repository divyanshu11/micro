package com.proptiger.app.services.srf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtilsBean;
//import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.proptiger.app.enums.MasterSellerRelevanceAttributesEnum;
import com.proptiger.app.repo.srf.SellerRelevanceFactorsDao;
import com.proptiger.app.repo.srf.SellerRelevancePackageComponentsDao;
import com.proptiger.app.repo.srf.SellerRelevancePackageDao;
import com.proptiger.core.exception.BadRequestException;
import com.proptiger.core.exception.ProAPIException;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.cms.RawSellerDTO;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.SellerRelevancePackage;
import com.proptiger.core.model.cms.SellerRelevancePackageComponents;
import com.proptiger.core.model.cms.MasterSellerRelevancePackageType.MasterSellerRelevancePackageTypeEnum;
import com.proptiger.core.model.cms.MasterTransactionCategoryGroup.TransactionCategoryGroups;
import com.proptiger.core.util.ExclusionAwareNullBeanUtisBean;
//import com.proptiger.data.enums.MasterSellerRelevanceAttributesEnum;
//import com.proptiger.data.service.srf.SellerRelevanceFactorsDao;
//import com.proptiger.data.service.srf.SellerRelevancePackageDao;
//import com.proptiger.data.service.srf.SellerRelevancePackageService;

//import com.proptiger.marketforce.data.model.RawSeller;
//import com.proptiger.marketforce.data.model.RawSeller.SellerType;
//import com.proptiger.marketforce.data.service.RawSellerService;


@Service
public class SellerRelevancePackageService {
	 @Autowired
	    private SellerRelevancePackageDao           sellerRelevancePackageDao;

	    @Autowired
	    private SellerRelevancePackageComponentsDao sellerRelevancePackageComponentsDao;

	    @Autowired
	    private SellerRelevanceFactorsDao           sellerRelevanceFactorsDao;

	    @Autowired
	    MasterSellerRelevanceAttributesEnum         masterSellerRelevanceAttributesEnum;
// @Divyanshu
	    @Autowired
	    private MIDLServiceHelper midlServiceHelper;
	    
	    
	    // // DO NOT AUTOWIRE. USE GETTER
//	    RawSellerService                            rawSellerService;

	    @Autowired
	    ApplicationContext                          applicationContext;
//@Divyanshu commented for the time....
	    @Value("${city.expert.max.city.count}")
	    private int                                 cityExpertMaxCityCount;

	    private final static Set<Integer>           PACKAGE_ENABLED_GROUPS = new HashSet<>(
	            Arrays.asList(TransactionCategoryGroups.PREMIUM_SELLER.getId(),
	                    TransactionCategoryGroups.EXPERT_DEAL_MAKER.getId(),
	                    TransactionCategoryGroups.DEAL_MAKER.getId(),
	                    TransactionCategoryGroups.PREPAID_SELLER.getId()));
	    Logger                                      logger                 =
	            LoggerFactory.getLogger(SellerRelevancePackageService.class);
//@Divyanshu
//	    public RawSellerService getRawSellerService() {
//	        if (this.rawSellerService == null) {
//	            this.rawSellerService = applicationContext.getBean(RawSellerService.class);
//	        }
//	        return this.rawSellerService;
//	    }

	    @Transactional
	    public Collection<SellerRelevancePackage> saveSellerRelevancePackage(
	            Collection<SellerRelevancePackage> sellerRelevancePackages,
	            Integer sellerRelevanceId) {
	        if (CollectionUtils.isEmpty(sellerRelevancePackages))
	            return null;
	        SellerRelevanceFactors sellerRelevanceFactor = sellerRelevanceFactorsDao.findOne(sellerRelevanceId);
	        sellerRelevancePackages.forEach(srp -> srp.setSellerRelevanceId(sellerRelevanceId));
	        validateSellerRelevancePackages(sellerRelevancePackages, sellerRelevanceFactor);

	        for (SellerRelevancePackage sellerRelevancePackage : sellerRelevancePackages) {
	            if (sellerRelevancePackage.getId() == null) {
	                applicationContext.getBean(SellerRelevancePackageService.class).saveSellerPackage(
	                        sellerRelevancePackage,
	                        sellerRelevancePackage.getSellerRelevnacePackageComponents());
	            }
	            else {
	                updateSellerRelevancePackage(sellerRelevancePackage, sellerRelevanceId, sellerRelevancePackage.getId());
	            }
	        }

	        return sellerRelevancePackages;
	    }

	    @Transactional
	    public SellerRelevancePackage updateSellerRelevancePackage(
	            SellerRelevancePackage sellerRelevancePackage,
	            int sellerRelevanceId,
	            int sellerRelevancePackageId) {
	        BeanUtilsBean beanUtilsBean = new ExclusionAwareNullBeanUtisBean();
	        SellerRelevanceFactors sellerRelevanceFactor = sellerRelevanceFactorsDao.findOne(sellerRelevanceId);
	        SellerRelevancePackage sellerRelevancePackageDB =
	                sellerRelevancePackageDao.findByIdAndSellerRelevanceId(sellerRelevancePackageId, sellerRelevanceId);
	        if (null == sellerRelevancePackageDB) {
	            throw new IllegalArgumentException(
	                    "Seller Relevance Package not present for Id " + sellerRelevancePackage.getId());
	        }
	        Integer sellerUserId = sellerRelevanceFactor.getSellerId();
	        try {
	            beanUtilsBean.copyProperties(sellerRelevancePackageDB, sellerRelevancePackage);
	            // sellerRelevancePackageDB
	            // .setSellerRelevnacePacakgeComponent(sellerRelevancePackage.getSellerRelevnacePackageComponents());
	        }
	        catch (Exception e) {
	            throw new ProAPIException(
	                    "Unable to copy old seller package to new seller package for id "
	                            + sellerRelevancePackageDB.getId());
	        }

	        validateDataOnAttributeConditions(sellerRelevancePackageDB, sellerUserId);
	        updateSellerPackage(sellerRelevancePackageDB, sellerRelevancePackage.getSellerRelevnacePackageComponents());

	        return sellerRelevancePackageDB;
	    }

	    private void updateSellerPackage(
	            SellerRelevancePackage sellerRelevancePackage,
	            Set<SellerRelevancePackageComponents> sellerRelevancePackageComponents) {
	        sellerRelevancePackageComponentsDao
	                .deleteSellerRelevancePackageComponentsByPackageId(sellerRelevancePackage.getId());
	        saveSellerPackage(sellerRelevancePackage, sellerRelevancePackageComponents);
	    }

	    @Transactional
	    public void saveSellerPackage(
	            SellerRelevancePackage sellerRelevancePackage,
	            Set<SellerRelevancePackageComponents> sellerRelevancePackageComponents) {
	        validateAttributeValues(sellerRelevancePackageComponents);
	        SellerRelevancePackage updatedSellerRelevancePackage = sellerRelevancePackageDao.save(sellerRelevancePackage);
	        Set<SellerRelevancePackageComponents> sellerRelevancePackageComponentsUpdates = new HashSet<>();
	        for (SellerRelevancePackageComponents srfam : sellerRelevancePackageComponents) {
	            SellerRelevancePackageComponents srfamClone = new SellerRelevancePackageComponents();
	            srfamClone.setSellerRelevanceId(updatedSellerRelevancePackage.getSellerRelevanceId());
	            srfamClone.setSellerPackageId(updatedSellerRelevancePackage.getId());
	            srfamClone.setAttributeId(srfam.getAttributeId());
	            srfamClone.setAttributeValues(srfam.getAttributeValues());
	            sellerRelevancePackageComponentsUpdates.add(srfamClone);
	        }
	        sellerRelevancePackageComponentsDao.save(sellerRelevancePackageComponentsUpdates);
	    }

	    private boolean validateAttributeValues(Set<SellerRelevancePackageComponents> sellerRelevancePackageComponents) {
	        Map<Integer, List<Integer>> attributeIdToValueMap = sellerRelevancePackageComponents.stream().collect(
	                Collectors.groupingBy(
	                        SellerRelevancePackageComponents::getAttributeId,
	                        Collectors.mapping(SellerRelevancePackageComponents::getAttributeValues, Collectors.toList())));
	        // TODO - validate

	        return true;
	    }

	    private void validateSellerRelevancePackages(
	            Collection<SellerRelevancePackage> sellerRelevancePackages,
	            SellerRelevanceFactors sellerRelevanceFactor) {
	        if (sellerRelevanceFactor == null) {
	            throw new BadRequestException("Seller relevance factor not saved");
	        }
	        int sellerUserId = sellerRelevanceFactor.getSellerId();

	        if (!PACKAGE_ENABLED_GROUPS.contains(sellerRelevanceFactor.getSellerTransGroupId())) {
	            throw new BadRequestException("Seller Package Type Id cannot be null for seller Id" + sellerUserId);
	        }
	        for (SellerRelevancePackage sellerRelevancePackage : sellerRelevancePackages) {
	            if (null == sellerRelevancePackage.getPackageTypeId()) {
	                throw new BadRequestException("Seller Package Type Id cannot be null for seller Id" + sellerUserId);
	            }

	            if (null == sellerRelevancePackage.getSellerRelevanceId()
	                    || !sellerRelevancePackage.getSellerRelevanceId().equals(sellerRelevanceFactor.getId())) {
	                throw new BadRequestException("Invalid seller relevance ID for :" + sellerUserId);
	            }

	            if (null == sellerRelevancePackage.getSellerRelevnacePackageComponents()
	                    || sellerRelevancePackage.getSellerRelevnacePackageComponents().isEmpty()) {
	                throw new BadRequestException(
	                        "Seller Relevance Package Components cannot be null for seller Id" + sellerUserId);
	            }
	            validateDataOnAttributeConditions(sellerRelevancePackage, sellerUserId);
	        }

	    }

	    @Transactional
	    public void deleteSellerRelevancePackage(List<Integer> sellerPackageIdList) {
	        List<Integer> sellerAttributeIds =
	                sellerRelevancePackageComponentsDao.getSellerFactorAttributeIds(sellerPackageIdList);
	        sellerRelevancePackageComponentsDao.deleteMasterSellerRelevanceFactorsAttributeMappingByIds(sellerAttributeIds);
	        sellerRelevancePackageDao.deleteSellerPackage(sellerPackageIdList);
	    }

	    public void deleteSellerRelevanceMapping(List<Integer> attributeMappingIds) {
	        sellerRelevancePackageComponentsDao
	                .deleteMasterSellerRelevanceFactorsAttributeMappingByIds(attributeMappingIds);
	    }

	    private void validateDataOnAttributeConditions(
	            SellerRelevancePackage sellerRelevancePackage,
	            Integer sellerUserId) {

	        Integer cityCount = 0;
	        boolean isCityExpert = false;

	        Set<SellerRelevancePackageComponents> masterSellerRelevnaceFactorsAttributeMappings =
	                sellerRelevancePackage.getSellerRelevnacePackageComponents();

	        if (null != masterSellerRelevnaceFactorsAttributeMappings) {

	            for (SellerRelevancePackageComponents msrfa : masterSellerRelevnaceFactorsAttributeMappings) {

	                if (msrfa.getAttributeId().equals(
	                        masterSellerRelevanceAttributesEnum.getMasterSellerRelevanceAttributeMap()
	                                .get(MasterSellerRelevancePackageTypeEnum.CITY.getName()))) {

	                    if (sellerRelevancePackage.getPackageTypeId().equals(
	                            masterSellerRelevanceAttributesEnum.getMasterSellerRelevanceAttributeMap()
	                                    .get(MasterSellerRelevancePackageTypeEnum.CITY.getName()))) {

	                        isCityExpert = true;
	                        cityCount++;
	                    }
	                    else {
	                        throw new ProAPIException("Cannot add city in other experts");
	                    }
	                }
	            }

	            if (isCityExpert) {

	//@divyanshu           RawSellerDTO rawSeller = getRawSellerService().getRawSellerByUserId(sellerUserId);
	            	// OK TESTED..
	            	RawSellerDTO rawSeller = midlServiceHelper.getRawSellerByUserId(sellerUserId);
	                if (cityCount > cityExpertMaxCityCount) {
	                    throw new ProAPIException(
	                            "City count cannot be greater than " + cityExpertMaxCityCount + " for city experts");
	                }

	                if (rawSeller.getCompanyUser().getSellerType().equals(com.proptiger.core.enums.external.mbridge.SellerType.Owner)) {
	                    throw new ProAPIException("Owners cannot become city experts");
	                }
	            }
	        }
	    }

	    public List<Integer> getPackageIdsBySellerRelevanceId(Integer sellerRelevanceId) {

	        return sellerRelevancePackageDao.getPackageIdBySellerRelevanceId(sellerRelevanceId);
	    }

	    public List<SellerRelevancePackageComponents> getPackageComponentsByEnitySaleTypeId(
	            List<String> entityTypes,
	            Integer saleTypeId) {

	        List<Integer> entityTypeIds = new ArrayList<>();
	        entityTypes.stream().forEach(type -> {
	            entityTypeIds.add(masterSellerRelevanceAttributesEnum.getMasterSellerRelevanceAttributeMap().get(type));
	        });

	        return sellerRelevancePackageComponentsDao.getPackageComponentsByEntitySaleType(entityTypeIds, saleTypeId);
	    }
}
