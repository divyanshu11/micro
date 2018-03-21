package com.proptiger.app.repo.order;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.proptiger.core.dto.order.LeadPaymentStatusDto;
//import com.proptiger.app.dto.order.LeadPaymentStatusDto;
import com.proptiger.core.model.transaction.LeadPaymentStatus;
//import com.proptiger.data.repo.order.LeadPaymentStatusDto;
/**
 * 
 * @author swapnil
 *
 */
@Deprecated
public interface LeadPaymentStatusBaseDao extends JpaRepository<LeadPaymentStatus, Integer>{

	/**
     * 
     * @param userId
     * @return
     */
    @Query(
            value = "select statusId, leadTypeId, count(*) from LeadPaymentStatus where crmUserId = ?1 and leadId IS NOT NULL group by statusId, leadTypeId")
    public List<Object[]> getLeadDistribution(int userId);

    /**
     * 
     * @param userIds
     * @return
     */
    @Query(
            value = "select crmUserId, count(*) from LeadPaymentStatus where statusId = 3 and leadTypeId in (1,2) and crmUserId in (?1) and leadId IS NOT NULL group by crmUserId")
    public List<Object[]> getLeadCounts(List<Integer> userIds);

    @Query(
            value = "select crmUserId, count(*) from LeadPaymentStatus where statusId = 3 and leadTypeId in (1,2) and crmUserId in (?1) group by crmUserId")
    public List<Object[]> getPrepaidAndPostPaidLeadCounts(Set<Integer> sellerIds);

    /**
     * @param leadTypeId
     * @return
     */
    @Query(
            value = "select new com.proptiger.app.dto.order.LeadPaymentStatusDto(lps.crmUserId,max(p.updatedAt)) from LeadPaymentStatus lps join lps.payment p where lps.statusId=3 and lps.leadTypeId = ?1 group by lps.crmUserId")
    public List<LeadPaymentStatusDto> findPaidSellersWithLastPaymentDateForLeadType(int leadTypeId);

    /**
     * 
     * @param userId
     * @param date
     * @return
     */
    @Query(
            value = "select lps.statusId, lps.leadTypeId, count(*) from LeadPaymentStatus lps join lps.payment p where lps.crmUserId = ?1 and p.updatedAt > ?2 group by lps.statusId, lps.leadTypeId")
    public List<Object[]> getLeadDistributionSinceDate(int userId, Date date);

    /**
     * 
     * @param leadId
     * @return
     */
    public List<LeadPaymentStatus> findByLeadId(int leadId);

    /**
     * 
     * @param crmUserId
     * @param statusId
     * @return
     */
    public List<LeadPaymentStatus> findByCrmUserIdAndStatusId(Integer crmUserId, Integer statusId);

    /**
     * 
     * @param leadIds
     * @return
     */
    public List<LeadPaymentStatus> findByLeadIdIn(List<Integer> leadIds);

    /**
     * Delete lead payment status for lead id
     * 
     * @param leadId
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "delete from LeadPaymentStatus where leadId=?1")
    public Integer deleteByLeadId(Integer leadId);

    /**
     * 
     * @param sellerId
     * @param leadTypeId
     * @return
     */
    @Query("select max(p.updatedAt) from LeadPaymentStatus lps join lps.payment p where lps.crmUserId = ?1 and lps.leadTypeId = ?2")
    public Date findLastPaymentDateOfSeller(Integer sellerId, Integer leadTypeId);

    /**
     * Fetch distribution of paid leads between last payment date and 7 days
     * before it
     * 
     * @param userId
     * @param date
     * @return
     */
    @Query(
            nativeQuery = true,
            value = "select lps.master_lead_payment_status_id, lps.master_lead_type_id, count(*) from proptiger.lead_payment_status lps join proptiger.payments p on p.transaction_id = lps.transaction_id where lps.crm_user_id = ?1 and lps.master_lead_payment_status_id = 3 and p.updated_at between DATE_SUB(?2, INTERVAL 7 DAY) and ?2 group by  lps.master_lead_payment_status_id, lps.master_lead_type_id")
    public List<Object[]> getPaidLeadsDistribution(int userId, Date date);

    /**
     * Get leads disclosed by user for sale type within 7 days of last payment
     * date
     * 
     * @param userId
     * @param date
     * @param leadTypeId
     * @return
     */
    @Query(
            nativeQuery = true,
            value = "select count(*) from proptiger.lead_payment_status lps join proptiger.payments p on p.transaction_id = lps.transaction_id where lps.crm_user_id = ?1 and lps.master_lead_payment_status_id = 3 and p.updated_at between DATE_SUB(?2, INTERVAL 7 DAY) and ?2 and lps.lead_id is not null and lps.master_lead_type_id = ?3")
    public BigInteger getCountOfLeadsDisclosed(int userId, Date date, int leadTypeId);

    /**
     * 
     * @param leadPaymentIds
     * @return
     */
    public List<LeadPaymentStatus> findByIdIn(List<Integer> ids);

    /**
     * 
     * @param leadPaymentId
     * @return
     */
    public LeadPaymentStatus findById(Integer id);

    /**
     * @param leadIds
     * @return
     */
    @Modifying
    @Transactional
    @Query(value = "update LeadPaymentStatus LPS set LPS.leadId = NULL WHERE LPS.id IN (?1) ")
    public Integer markLeadIdsAsNull(List<Integer> leadIds);

    @Query("select lps from LeadPaymentStatus lps join lps.leadPaymentStatusMetaAttributes lpsa where lps.crmUserId = ?1 and lpsa.cityId = ?2 and lps.leadTypeId = ?3 and lps.leadId is null")
    public List<LeadPaymentStatus> findByCrmUserIdAndCityIdAndSaleTypeId(int userId, int cityId, int leadTypeId);

    @Query("select lps from LeadPaymentStatus lps left join lps.leadPaymentStatusMetaAttributes lpsa where lps.crmUserId = ?1 and lps.leadTypeId = ?2  and lps.leadId is null and lpsa.cityId is null")
    public List<LeadPaymentStatus> findByCrmUserIdAndSaleTypeId(int userId, int leadTypeId);

    public List<LeadPaymentStatus> findByLeadIdAndStatusId(int leadId, int statusId);

    @Modifying
    @Transactional
    @Query("UPDATE LeadPaymentStatus LPS set LPS.amount=?4 where LPS.crmUserId=?1 and LPS.leadTypeId=?2 and LPS.statusId=1 and LPS.createdAt <=?3 ")
    public Integer updateLeadAmountOfSellerSaleType(
            Integer userId,
            Integer leadTypeId,
            Date createdAtLT,
            Integer amount);
}
