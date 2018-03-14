package com.proptiger.app.services.srf;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.proptiger.app.dto.srf.SellerRelevanceEventDTO;
import com.proptiger.app.dto.srf.SellerRelevanceEventDTO.EventDTO;
import com.proptiger.app.dto.srf.SellerRelevanceEventDTO.EventGroupDTO;
import com.proptiger.app.model.srf.SellerRelevanceEvent;
import com.proptiger.app.model.srf.SellerRelevanceEvent.EventCategory;
import com.proptiger.app.repo.srf.SellerRelevanceEventDao;
import com.proptiger.core.constants.ResponseCodes;
import com.proptiger.core.constants.ResponseErrorMessages;
import com.proptiger.core.dto.internal.ActiveUser;
import com.proptiger.core.exception.UnauthorizedException;
import com.proptiger.core.helper.CompanyUserServiceHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.cms.RawSellerDTO;
import com.proptiger.core.model.companyuser.CompanyUser;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.APIResponse;
import com.proptiger.core.pojo.response.PaginatedResponse;
//import com.proptiger.data.dto.srf.SellerRelevanceEventDTO;
//import com.proptiger.data.dto.srf.SellerRelevanceEventDTO.EventDTO;
//import com.proptiger.data.dto.srf.SellerRelevanceEventDTO.EventGroupDTO;
//import com.proptiger.data.model.srf.SellerRelevanceEvent;
//import com.proptiger.data.model.srf.SellerRelevanceEvent.EventCategory;
//import com.proptiger.data.repo.srf.SellerRelevanceEventDao;
//import com.proptiger.data.service.srf.ISRFEventService;
//import com.proptiger.data.service.srf.SRFEventServiceFactory;
//import com.proptiger.marketforce.data.model.RawSeller;
//import com.proptiger.marketforce.data.repository.RawSellerDao;

@Service
public class SellerRelevanceEventService {

	@Autowired
    private SRFEventServiceFactory        serviceFactory;

    @Autowired
    private SellerRelevanceEventDao       sellerRelevanceEventDao;

    @Autowired
    private CompanyUserServiceHelper      companyUserServiceHelper;

    /*
     * Divyanshu
     * ----------------------------
     */
   // @Autowired
   // private RawSellerDao                  rawSellerDao;
    
    
    @Autowired
    private MIDLServiceHelper midlServiceHelper;
    //----------------------------------

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public APIResponse populateSellerRelevanceFactorsEventsInDB(
            @RequestParam(required = true) List<EventCategory> eventTypes,
            int noOfDays) {
        List<ISRFEventService> serviceObjects = eventTypes.stream()
                .map(e -> serviceFactory.getSRFEventsServiceInstance(e)).collect(Collectors.toList());
        // TODO - Currently only account locked is supported. Once suspended and
        // penalized are integrated, we can call all conccurently
        Date mignightStartDate = new Date(new DateTime().minusDays(noOfDays).withTimeAtStartOfDay().getMillis());
        // Temporary fix - Adding 1 day to current date as fiql selector doesn't
        // support time level precision
        Date endDate = new Date(new DateTime().plusDays(1).withTimeAtStartOfDay().getMillis());
        List<SellerRelevanceEvent> eventsList = serviceObjects.stream()
                .flatMap(obj -> obj.getAllEventsBetween(null, mignightStartDate, endDate).stream())
                .collect(Collectors.toList());
       // List<SellerRelevanceEvent> el= eventsList;
        eventsList.removeIf(e -> e==null || e.getEventDate() == null);
        eventsList.sort((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()));
        // save to DB
        sellerRelevanceEventDao.deleteByEventDateAtGreaterThanEqual(mignightStartDate);
        Lists.partition(eventsList, 30).forEach(subList -> sellerRelevanceEventDao.save(subList));
        return new APIResponse();
    }
    // DONE Tested
    public PaginatedResponse<List<SellerRelevanceEventDTO>> getSellerIntent(
            ActiveUser activeUser,
            List<EventCategory> eventTypes,
            Set<Integer> sellerIds,
            Date startDate,
            Date endDate,
            Integer start,
            Integer rows) {
        Set<Integer> croCompanyUserIds = getAllCroIdsForCro(activeUser.getUserIdentifier());
        //D------------------------------------------
     //   List<RawSeller> allocatedSellers = rawSellerDao.getSellersAllocatedToCro(croCompanyUserIds);
        List<RawSellerDTO> allocatedSellers = midlServiceHelper.getSellersAllocatedToCro(croCompanyUserIds);
        //--------------------------------------------
        
        Set<Integer> allocatedSellerIds =
                allocatedSellers.stream().map(RawSellerDTO::getSellerUserId).collect(Collectors.toSet());
        // Throw exception if seller is not allocated to CRO
        if (CollectionUtils.isNotEmpty(sellerIds)
                && Sets.intersection(sellerIds, allocatedSellerIds).size() != sellerIds.size()) {
            throw new UnauthorizedException(ResponseCodes.UNAUTHORIZED, ResponseErrorMessages.User.UNAUTHORIZED);
        }
        List<SellerRelevanceEvent> srfEvents = getSellerRelevanceEvents(eventTypes, sellerIds, startDate, endDate);
        srfEvents.sort((e1, e2) -> e2.getEventDate().compareTo(e1.getEventDate()));
        List<SellerRelevanceEventDTO> output = sellerIntentDTOListBuilder(
                eventTypes,
                srfEvents.stream().filter(e -> allocatedSellerIds.contains(e.getSellerId()))
                        .collect(Collectors.toList()));
        PaginatedResponse<List<SellerRelevanceEventDTO>> paginatedResponse = new PaginatedResponse<>();

        if (CollectionUtils.isNotEmpty(output)) {
            paginatedResponse
                    .setResults(output.subList(Math.min(start, output.size()), Math.min(start + rows, output.size())));
            paginatedResponse.setTotalCount(output.size());
        }
        return paginatedResponse;
    }

    public PaginatedResponse<List<SellerRelevanceEvent>> getSellerRelevanceEventBySelector(FIQLSelector selector) {
        return sellerRelevanceEventDao.getSellerRelevanceEventsBySelector(selector);
    }

    private List<SellerRelevanceEvent> getSellerRelevanceEvents(
            List<EventCategory> eventTypes,
            Set<Integer> sellerIds,
            Date startDate,
            Date endDate) {
        FIQLSelector selector = new FIQLSelector();
        selector.addAndConditionToFilter("eventCategory", eventTypes);
        if (CollectionUtils.isNotEmpty(sellerIds)) {
            selector.addAndConditionToFilter("sellerId", sellerIds);
        }
        if (startDate != null) {
            selector.addGreaterThan("eventDate", DATE_FORMATTER.format(startDate));
        }
        if (endDate != null) {
            selector.addLessThanEqual("eventDate", DATE_FORMATTER.format(endDate));
        }

        List<SellerRelevanceEvent> finalOutput = new ArrayList<>();
        long totalCount = -1;
        int start = 0;
        final int rows = 500;
        selector.setRows(rows);
        do {
            PaginatedResponse<List<SellerRelevanceEvent>> sellerRelevanceEventsPaginatedResult =
                    sellerRelevanceEventDao.getSellerRelevanceEventsBySelector(selector);
            totalCount = sellerRelevanceEventsPaginatedResult.getTotalCount();
            finalOutput.addAll(sellerRelevanceEventsPaginatedResult.getResults());
            start += rows;
            selector.setStart(start);
        }
        while (start < totalCount);
        return finalOutput;
    }

    private Map<EventCategory, Set<Integer>> getValidSellersMap(
            List<EventCategory> eventTypes,
            List<SellerRelevanceEvent> srfEvents) {
        Map<EventCategory, Set<Integer>> eventCategoryToSellerIdSet = srfEvents.stream().collect(
                Collectors.groupingBy(
                        SellerRelevanceEvent::getEventCategory,
                        Collectors.mapping(SellerRelevanceEvent::getSellerId, Collectors.toSet())));
        Map<EventCategory, Set<Integer>> eventCategoryToValidSellerIdSet = new HashMap<>();
        eventCategoryToSellerIdSet.keySet().forEach(eventCategory -> {
            eventCategoryToValidSellerIdSet.put(
                    eventCategory,
                    serviceFactory.getSRFEventsServiceInstance(eventCategory)
                            .filterSellers(eventCategoryToSellerIdSet.get(eventCategory)));
        });
        return eventCategoryToValidSellerIdSet;
    }

    private List<SellerRelevanceEventDTO> sellerIntentDTOListBuilder(
            List<EventCategory> eventTypes,
            List<SellerRelevanceEvent> srfEvents) {
        Map<Integer, Map<EventCategory, EventGroupDTO>> combinedOutput = new LinkedHashMap<>();
        Map<EventCategory, Set<Integer>> eventCategoryToValidSellerIdSet = getValidSellersMap(eventTypes, srfEvents);
        for (SellerRelevanceEvent srfEvent : srfEvents) {
            if (!eventCategoryToValidSellerIdSet.get(srfEvent.getEventCategory()).contains(srfEvent.getSellerId())) {
                continue;
            }
            Integer sellerId = srfEvent.getSellerId();
            if (combinedOutput.get(sellerId) == null) {
                combinedOutput.put(sellerId, new HashMap<EventCategory, EventGroupDTO>());
            }
            if (combinedOutput.get(sellerId).get(srfEvent.getEventCategory()) == null) {
                EventGroupDTO eventGroupDTO = new EventGroupDTO();
                eventGroupDTO.setEventCategory(srfEvent.getEventCategory());
                eventGroupDTO.setEvents(new TreeSet<EventDTO>((o1, o2) -> {
                    if (o1.getSource().equals(o2.getSource())
                            && DateUtils.isSameDay(o1.getEventDate(), o2.getEventDate())) {
                        return 0;
                    }
                    return o1.getEventDate().compareTo(o2.getEventDate());
                }));
                combinedOutput.get(sellerId).put(srfEvent.getEventCategory(), eventGroupDTO);
            }

            EventDTO event = new EventDTO();
            event.setEventDate(srfEvent.getEventDate());
            event.setIntentLevel(
                    serviceFactory.getSRFEventsServiceInstance(srfEvent.getEventCategory()).getSellerIntent(srfEvent));
            event.setSource(srfEvent.getSource().name());
            combinedOutput.get(sellerId).get(srfEvent.getEventCategory()).getEvents().add(event);
        }
        List<SellerRelevanceEventDTO> output = new ArrayList<>(combinedOutput.size());
        for (Entry<Integer, Map<EventCategory, EventGroupDTO>> entry : combinedOutput.entrySet()) {
            Integer sellerId = entry.getKey();
            SellerRelevanceEventDTO sellerRelevanceEventDTO = new SellerRelevanceEventDTO();
            sellerRelevanceEventDTO.setSellerId(sellerId);
            sellerRelevanceEventDTO.setEventGroups(entry.getValue().values());

            output.add(sellerRelevanceEventDTO);

        }
        return output;
    }

    private Set<Integer> getAllCroIdsForCro(Integer croUserId) {
        List<CompanyUser> allCompanyUserInHierarchy = companyUserServiceHelper.getChildList(croUserId);
        Set<Integer> croCompanyUserIds =
                allCompanyUserInHierarchy.stream().map(CompanyUser::getId).collect(Collectors.toSet());
        croCompanyUserIds.add(companyUserServiceHelper.getCompanyUserOfUserId(croUserId).getId());
        return croCompanyUserIds;
    }
}
