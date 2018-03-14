package com.proptiger.app.repo.srf;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.app.model.srf.SellerRelevanceEvent;
@Repository
public interface SellerRelevanceEventDao extends JpaRepository<SellerRelevanceEvent,Integer>,SellerRelevanceEventCustomDao
{
	 @Modifying
	    @Transactional
	    @Query("delete from SellerRelevanceEvent sre where sre.eventDate >= ?1 and sre.eventDate <= ?2")
	    public int deleteByEventDateBetween(Date startDate, Date endDate);

	    @Modifying
	    @Transactional
	    @Query("delete from SellerRelevanceEvent sre where sre.eventDate >= ?1")
	    public int deleteByEventDateAtGreaterThanEqual(Date startDate);

	    public List<SellerRelevanceEvent> findBySellerIdInAndEventDateGreaterThanEqualAndEventDateLessThanEqual(
	            List<Integer> sellerIds,
	            Date startDate,
	            Date endDate);

}
