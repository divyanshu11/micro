package com.proptiger.app.services.srf;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.proptiger.app.model.srf.SellerRelevanceEvent;
import com.proptiger.app.model.srf.SellerRelevanceEvent.EventCategory;
import com.proptiger.app.model.srf.SellerRelevanceEvent.EventSource;
import com.proptiger.core.dto.cms.support.ContactSupportDTO;
//import com.proptiger.core.dto.cms.support.ContactSupportDTO;
import com.proptiger.core.enums.support.SupportType;
import com.proptiger.core.helper.AthenaServiceHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.athena.GaAccountLockedResponseDTO;
import com.proptiger.core.model.cms.SellerRelevanceFactors;
import com.proptiger.core.model.cms.support.ContactSupport;
import com.proptiger.core.model.cms.MasterTransactionCategoryGroup.TransactionCategoryGroups;
import com.proptiger.core.pojo.FIQLSelector;

//import com.proptiger.data.enums.SupportType;
//import com.proptiger.data.model.cms.support.ContactSupport;
//import com.proptiger.data.model.srf.SellerRelevanceEvent;
//import com.proptiger.data.model.srf.SellerRelevanceEvent.EventCategory;
//import com.proptiger.data.model.srf.SellerRelevanceEvent.EventSource;
//import com.proptiger.data.service.cms.support.ContactSupportService;
//import com.proptiger.data.service.srf.SellerRelevanceFactorsService;
//import com.proptiger.data.service.srf.AccountLockedEventsService.AccountLockedEventEnum;

@Service
public class AccountLockedEventsService implements ISRFEventService{
	 public enum AccountLockedEventEnum {
	        STRONG, NORMAL
	    }

	 	//Divyanshu...
	 	@Autowired
	 	private MIDLServiceHelper midlservicehelper;
	 	//...
	//    @Autowired
	//    private ContactSupportService         contactSupportService;

	    @Autowired
	    private AthenaServiceHelper           athenaServiceHelper;

	    @Autowired
	    private SellerRelevanceFactorsService srfService;

	    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	    @Override
	    public List<SellerRelevanceEvent> getAllEventsBetween(List<Integer> sellerIds, Date startDate, Date endDate) {
	        return Stream.concat(
	                getSellerIntentFromContactSupport(sellerIds, startDate, endDate).stream(),
	                getSellerIntentFromGA(sellerIds, startDate, endDate).stream()).collect(Collectors.toList());
	    }

	    @Override
	    public String getSellerIntent(SellerRelevanceEvent srfEvent) {
	        String intent = "";
	        switch (srfEvent.getSource()) {
	            case CONTACT_SUPPORT:
	                intent = AccountLockedEventEnum.STRONG.name();
	                break;
	            case GA:
	                intent = AccountLockedEventEnum.NORMAL.name();
	                break;
	        }
	        return intent;
	    }

	    @Override
	    public Set<Integer> filterSellers(Set<Integer> sellerIds) {
	        FIQLSelector selector = new FIQLSelector();
	        selector.addAndConditionToFilter("sellerId", sellerIds);
	        selector.addAndConditionToFilter("sellerTransGroupId", TransactionCategoryGroups.ACCOUNT_LOCKED.getId());
	        return srfService.getSellerRelevanceFactors(selector).getResults().stream()
	                .map(SellerRelevanceFactors::getSellerId).collect(Collectors.toSet());
	    }

	    private List<SellerRelevanceEvent> getSellerIntentFromContactSupport(
	            List<Integer> sellerIds,
	            Date startDate,
	            Date endDate) {
	        FIQLSelector selector = new FIQLSelector();
	        selector.addAndConditionToFilter("supportType", SupportType.ACCOUNT_LOCKED);
	        if (CollectionUtils.isNotEmpty(sellerIds)) {
	            selector.addAndConditionToFilter("userId", sellerIds);
	        }

	        selector.addGreaterThan("createdAt", DATE_FORMATTER.format(startDate));
	        selector.addLessThanEqual("createdAt", DATE_FORMATTER.format(endDate));
	        selector.addSortASC("createdAt");
	        
	        List<ContactSupportDTO> x = midlservicehelper.getContactSupportBySelector(selector);
	        return accountLockedIntentDTOBuilder(x
	        		
	        		);
	        //		contactSupportService.getContactSupportDetailsFromSelector(selector).getResults());
	        		
	    }

	    private List<SellerRelevanceEvent> getSellerIntentFromGA(List<Integer> sellerIds, Date startDate, Date endDate) {
	        return accountLockedIntentDTOBuilderFromGA(
	                athenaServiceHelper.getSellerRelevanceGAEvents(
	                        sellerIds,
	                        startDate.getTime(),
	                        endDate.getTime(),
	                        "ACCOUNT_LOCKED"));
	    }

	    private List<SellerRelevanceEvent> accountLockedIntentDTOBuilder(List<ContactSupportDTO> contactSupportList) {
	        List<SellerRelevanceEvent> sellerRelevanceEventsList = new LinkedList<>();
	        for (ContactSupportDTO contactSupport : contactSupportList) {
	            SellerRelevanceEvent sellerRelevanceEvent = new SellerRelevanceEvent();
	            sellerRelevanceEvent.setSellerId(contactSupport.getUserId());
	            sellerRelevanceEvent.setSource(EventSource.CONTACT_SUPPORT);
	            sellerRelevanceEvent.setSourceEventName(EventSource.CONTACT_SUPPORT.name());
	            sellerRelevanceEvent.setEventCategory(EventCategory.ACCOUNT_LOCKED);
	   //         sellerRelevanceEvent.setEventDate(contactSupport.getCreatedAt());
	            sellerRelevanceEvent.setEventDate(contactSupport.getContactSupportDate());
	            sellerRelevanceEventsList.add(sellerRelevanceEvent);
	        }
	        return sellerRelevanceEventsList;
	    }

	    private List<SellerRelevanceEvent> accountLockedIntentDTOBuilderFromGA(
	            List<GaAccountLockedResponseDTO> gaAccountLockedEventList) {
	        List<SellerRelevanceEvent> sellerRelevanceEventsList = new LinkedList<>();
	        for (GaAccountLockedResponseDTO gaAccountLockedResponse : gaAccountLockedEventList) {
	            SellerRelevanceEvent sellerRelevanceEvent = new SellerRelevanceEvent();
	            sellerRelevanceEvent.setSellerId(gaAccountLockedResponse.getSellerId());
	            sellerRelevanceEvent.setSource(EventSource.GA);
	            sellerRelevanceEvent.setSourceEventName(gaAccountLockedResponse.getEventName());
	            sellerRelevanceEvent.setEventCategory(EventCategory.ACCOUNT_LOCKED);
	            sellerRelevanceEvent.setEventDate(gaAccountLockedResponse.getEventDate());

	            sellerRelevanceEventsList.add(sellerRelevanceEvent);
	        }
	        return sellerRelevanceEventsList;
	    }

		

}
