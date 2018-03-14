package com.proptiger.app.dto.srf;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;

import com.proptiger.app.model.srf.SellerRelevanceEvent.EventCategory;



public class SellerRelevanceEventDTO implements Serializable{
	 private static final long         serialVersionUID = 6656206743527667129L;

	    private Integer                   sellerId;
	    private Collection<EventGroupDTO> eventGroups;

	    public Integer getSellerId() {
	        return sellerId;
	    }

	    public void setSellerId(Integer sellerId) {
	        this.sellerId = sellerId;
	    }

	    public Collection<EventGroupDTO> getEventGroups() {
	        return eventGroups;
	    }

	    public void setEventGroups(Collection<EventGroupDTO> eventGroups) {
	        this.eventGroups = eventGroups;
	    }

	    public static class EventGroupDTO implements Serializable {

	        /**
	         * 
	         */
	        private static final long serialVersionUID = -5439241887363937829L;
	        private EventCategory     eventCategory;
	        private TreeSet<EventDTO> events;

	        public TreeSet<EventDTO> getEvents() {
	            return events;
	        }

	        public void setEvents(TreeSet<EventDTO> events) {
	            this.events = events;
	        }

	        public EventCategory getEventCategory() {
	            return eventCategory;
	        }

	        public void setEventCategory(EventCategory eventCategory) {
	            this.eventCategory = eventCategory;
	        }
	    }

	    public static class EventDTO implements Serializable {

	        /**
	         * 
	         */
	        private static final long serialVersionUID = 6208464290284520381L;

	        private String            source;
	        private String            intentLevel;
	        private Date              eventDate;

	        public String getSource() {
	            return source;
	        }

	        public void setSource(String source) {
	            this.source = source;
	        }

	        public String getIntentLevel() {
	            return intentLevel;
	        }

	        public void setIntentLevel(String intentLevel) {
	            this.intentLevel = intentLevel;
	        }

	        public Date getEventDate() {
	            return eventDate;
	        }

	        public void setEventDate(Date eventDate) {
	            this.eventDate = eventDate;
	        }

	    }

	
}
