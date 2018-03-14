package com.proptiger.app.service.order;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;
import com.google.gson.Gson;
import com.proptiger.app.model.kira.IvrCallDumpJson;
import com.proptiger.app.mvc.order.SellerCallMessage;
import com.proptiger.core.helper.ICRMHelper;
import com.proptiger.core.helper.MIDLServiceHelper;
import com.proptiger.core.model.icrm.IvrCallDump;
import com.proptiger.core.model.kira.CallEvents;
import com.proptiger.core.model.kira.CallLog;
import com.proptiger.core.service.SQSService;
import com.proptiger.core.util.SerializationUtils;
//import com.proptiger.data.model.kira.IvrCallDumpJson;
//import com.proptiger.data.model.order.SellerCallMessage;
//import com.proptiger.data.service.order.SellerCallService;
//import com.proptiger.marketforce.data.service.SellerActionService;

/**
*
* @author swapnil
*
*/
@Service
public class SellerCallService {
	@Autowired
    private ICRMHelper icrmHelper;

	@Autowired
	@Qualifier("sqsService")
	private SQSService sqsService;
//----Divyanshu
	@Autowired
	private MIDLServiceHelper midlServiceHelper;

//	@Autowired
//	private SellerActionService sellerActionService;

	@Value("${seller.call.response.queue.name}")
	private String sellerCallQueueName;

    @Value("${seller.lead.action.received.call}")
    private Integer sellerLeadActionId;

	private static Logger logger = LoggerFactory.getLogger(SellerCallService.class);

	private List<Message> getMessagesForTracking() {
		return sqsService.readMessagesFromQueue(sellerCallQueueName);
	}

	private void deleteMessages(List<Message> messages) {
		sqsService.deleteMessagesFromQueue(sellerCallQueueName, messages);
	}

    private boolean processMessage(Message message) {
        boolean canDelete = true;
        CallEvents callEvents = getCallEventsFromMessage(message);
        // logic to log corresponding data goes here !
        if (callEvents != null) {
            Integer callLogId = callEvents.getCallLogId();
            if (callEvents.getCallLog().getCallStatus().equals(CallLog.CallStatus.answered)) {
                List<IvrCallDump> ivrCallDumps = icrmHelper.getCallLog(callLogId);
                // track events here !
                Integer masterCallSourceId = callEvents.getCallLog().getCommunicationLog().getMasterCallSourceId();
                // 4: seller
                // 1: buyer
                if (masterCallSourceId == 1) {
                    canDelete = updateSellerAction(ivrCallDumps, callEvents.getCallLog().getCreatedAt());
                }
                else if (masterCallSourceId == 4) {
                    // call initiated by
                    // seller, do nothing
                    canDelete = true;
                }
            }
            else {
                // not related to us
                canDelete = true;
            }
        }
        return canDelete;
    }

    private boolean updateSellerAction(List<IvrCallDump> ivrCallDumps, Date createdAt) {
        Gson gson = new Gson();
        for (IvrCallDump ivrCallDump : ivrCallDumps) {
            if (ivrCallDump.getLeadId() == null) {
                continue;
            }
            IvrCallDumpJson ivrCallDumpJson = null;
            try {
                ivrCallDumpJson = gson.fromJson(ivrCallDump.getJson(), IvrCallDumpJson.class);
            }
            catch (Exception e) {
                logger.error("Unable to format call dump json, error {}", e);
            }
            // lead found, update and break
            if (ivrCallDumpJson != null && ivrCallDumpJson.getSellerId() != null) {
            	midlServiceHelper.trackSellerAction(ivrCallDumpJson.getSellerId(), ivrCallDump.getLeadId(), sellerLeadActionId, createdAt);
//                sellerActionService.trackSellerAction(
//                        ivrCallDumpJson.getSellerId(),
//                        ivrCallDump.getLeadId(),
//                        sellerLeadActionId,
//                        createdAt);
                return true;
            }
        }
        return true;
    }

    private CallEvents getCallEventsFromMessage(Message message) {
        SellerCallMessage sellerCallMessage = null;
        CallEvents callEvents = null;
        Gson gson = new Gson();
        try {
            sellerCallMessage = gson.fromJson(message.getBody(), SellerCallMessage.class);
            callEvents = SerializationUtils.jsonStringToClass(sellerCallMessage.getMessage(), CallEvents.class);
        }
        catch (Exception e) {
            logger.error("Unable to parse message with id: {}, error: {}", message.getMessageId(), e);
        }
        return callEvents;
    }

    private List<Message> processMessageList(List<Message> messages) {
        List<Message> processedMessages = new ArrayList<>();
        for (Message message : messages) {
            if (processMessage(message)) {
                processedMessages.add(message);
            }
        }
        return processedMessages;
    }

    /**
     *
     * @return
     */
    public int processMessagesInQueue() {
        List<Message> messages = null;
        int totalProcessed = 0;
        do {
            // get the messaged from queue
            messages = getMessagesForTracking();
            if (messages.isEmpty()) {
                break;
            }
            // process messages
            List<Message> processedMessages = processMessageList(messages);
            // delete processed messages
            if (!processedMessages.isEmpty()) {
                deleteMessages(processedMessages);
                totalProcessed += processedMessages.size();
            }
        }
        while (totalProcessed < 100);
        return totalProcessed;
    }
}
