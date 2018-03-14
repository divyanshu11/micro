package com.proptiger.app.model.srf;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.proptiger.core.model.BaseModel;



@Entity
@Table(name = "cms.seller_relevance_events")
@JsonFilter("fieldFilter")
@JsonInclude(Include.NON_NULL)
public class SellerRelevanceEvent extends BaseModel{
	 /**
	 * 
	 */
	private static final long serialVersionUID = -8018094240205497357L;

	/**
	 * 
	 */
	

	public static enum EventCategory {
	        ACCOUNT_LOCKED, SUSPENDED, PARTIAL_PENALIZED
	    }

	    public static enum EventSource {
	        CONTACT_SUPPORT, GA
	    }

	    /**
	     * 
	     */
//	    private static final long serialVersionUID = 5605606171344954084L;

	    @Id
	    @GeneratedValue(strategy = GenerationType.AUTO)
	    private Integer           id;

	    @Column(name = "seller_id", nullable = false)
	    private Integer           sellerId;

	    @Column(name = "event_category", nullable = false)
	    @Enumerated(EnumType.STRING)
	    private EventCategory     eventCategory;

	    @Column(name = "source", nullable = false)
	    @Enumerated(EnumType.STRING)
	    private EventSource       source;

	    @Column(name = "source_event_name")
	    private String            sourceEventName;

	    @Column(name = "event_date", nullable = false)
	    private Date              eventDate;

	    @Column(name = "created_at")
	    private Date              createdAt;

	    @Column(name = "updated_at")
	    private Date              updatedAt;

	    @PrePersist
	    public void prePersist() {
	        this.createdAt = new Date();
	        this.updatedAt = this.createdAt;
	    }

	    @PreUpdate
	    public void preUpdate() {
	        this.updatedAt = new Date();
	    }

	    public Integer getId() {
	        return id;
	    }

	    public void setId(Integer id) {
	        this.id = id;
	    }

	    public Integer getSellerId() {
	        return sellerId;
	    }

	    public void setSellerId(Integer sellerId) {
	        this.sellerId = sellerId;
	    }

	    public EventCategory getEventCategory() {
	        return eventCategory;
	    }

	    public void setEventCategory(EventCategory eventCategory) {
	        this.eventCategory = eventCategory;
	    }

	    public EventSource getSource() {
	        return source;
	    }

	    public void setSource(EventSource source) {
	        this.source = source;
	    }

	    public String getSourceEventName() {
	        return sourceEventName;
	    }

	    public void setSourceEventName(String sourceEventName) {
	        this.sourceEventName = sourceEventName;
	    }

	    public Date getEventDate() {
	        return eventDate;
	    }

	    public void setEventDate(Date eventDate) {
	        this.eventDate = eventDate;
	    }

	    public Date getCreatedAt() {
	        return createdAt;
	    }

	    public void setCreatedAt(Date createdAt) {
	        this.createdAt = createdAt;
	    }

	    public Date getUpdatedAt() {
	        return updatedAt;
	    }

	    public void setUpdatedAt(Date updatedAt) {
	        this.updatedAt = updatedAt;
	    }

}
