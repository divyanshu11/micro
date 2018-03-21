package com.proptiger.app.repo.order;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.proptiger.core.dto.order.LeadPaymentStatusDto;
//import com.proptiger.app.dto.order.LeadPaymentStatusDto;
import com.proptiger.core.model.transaction.LeadPaymentStatus;
import com.proptiger.core.pojo.FIQLSelector;
import com.proptiger.core.pojo.response.PaginatedResponse;
import com.proptiger.core.repo.FIQLDao;
import com.proptiger.core.util.Constants;
//import com.proptiger.data.repo.order.LeadPaymentStatusBaseDao;
//import com.proptiger.data.repo.order.LeadPaymentStatusDao;
//import com.proptiger.data.repo.order.LeadPaymentStatusDto;

/**
 * 
 * @author swapnil
 *
 */
@Repository
@Deprecated
public class LeadPaymentStatusDao {

	@Autowired
    private LeadPaymentStatusBaseDao leadPaymentStatusBaseDao;

    @Autowired
    private FIQLDao                  fiqlDao;

    @Autowired
    private EntityManagerFactory     emf;

    private static Logger            logger = LoggerFactory.getLogger(LeadPaymentStatusDao.class);

    /**
     * 
     * @param leadPaymentStatuses
     */
    public List<LeadPaymentStatus> save(List<LeadPaymentStatus> leadPaymentStatuses) {
        return leadPaymentStatusBaseDao.save(leadPaymentStatuses);
    }

    public LeadPaymentStatus save(LeadPaymentStatus leadPaymentStatus) {
        return leadPaymentStatusBaseDao.save(leadPaymentStatus);
    }

    /**
     * 
     * @param selector
     * @return
     */
    public PaginatedResponse<List<LeadPaymentStatus>> getLeadPaymentStatusBySelector(FIQLSelector selector) {
        return fiqlDao.getEntitiesFromDb(selector, emf, LeadPaymentStatus.class);
    }

    /**
     * 
     * @param crmUserId
     * @return
     */
    public List<Object[]> getLeadDistribution(Integer crmUserId) {
        return leadPaymentStatusBaseDao.getLeadDistribution(crmUserId);
    }

    /**
     * 
     * @param crmUserIds
     * @return
     */
    public List<Object[]> getLeadCounts(List<Integer> crmUserIds) {
        return leadPaymentStatusBaseDao.getLeadCounts(crmUserIds);
    }

    /**
     * 
     * @param sellerIds
     * @return
     */
    public List<Object[]> getPrepaidAndPostPaidLeadCounts(Set<Integer> sellerIds) {
        return leadPaymentStatusBaseDao.getPrepaidAndPostPaidLeadCounts(sellerIds);
    }

    /**
     * 
     * @param crmUserId
     * @param date
     * @return
     */
    public List<Object[]> getLeadDistributionSinceDate(Integer crmUserId, Date date) {
        return leadPaymentStatusBaseDao.getLeadDistributionSinceDate(crmUserId, date);
    }

    /**
     * 
     * @param leadId
     * @return
     */
    public LeadPaymentStatus findByLeadId(int leadId) {
        List<LeadPaymentStatus> leadPaymentStatuses = leadPaymentStatusBaseDao.findByLeadId(leadId);
        if (leadPaymentStatuses.isEmpty()) {
            return null;
        }
        if (leadPaymentStatuses.size() > 1) {
            logger.error("Multiple rows returned for leadId: {}", leadId);
        }
        return leadPaymentStatuses.get(0);
    }

    /**
     * 
     * @param selleUserId
     * @param statusId
     * @return
     */
    public List<LeadPaymentStatus> findByCrmUserIdAndStatusId(Integer selleUserId, Integer statusId) {
        return leadPaymentStatusBaseDao.findByCrmUserIdAndStatusId(selleUserId, statusId);
    }

    /**
     * Delete lead payment status for given lead ids
     * 
     * @param leadIds
     */
    public Integer deleteLeadPaymentStatusByLeadId(Integer leadId) {
        return leadPaymentStatusBaseDao.deleteByLeadId(leadId);
    }

    /**
     * Delete lead payment status for given list of lead payment status
     * 
     * @param ids
     */
    public void deleteLeadPaymentStatusByLeadPaymentStatusList(List<LeadPaymentStatus> leadPaymentStatusList) {

        leadPaymentStatusBaseDao.deleteInBatch(leadPaymentStatusList);
    }

    public List<LeadPaymentStatus> findLeadPaymentStatusByIds(List<Integer> ids) {

        return leadPaymentStatusBaseDao.findByIdIn(ids);
    }

    public LeadPaymentStatus findLeadPaymentStatusById(Integer id) {

        return leadPaymentStatusBaseDao.findById(id);
    }

    /**
     * Revert PrePayment Lead Ids from Lead Payment Status
     * 
     * @param leadIds
     */
    public Integer revertPrePaymentLeadIds(List<Integer> leadPaymentIdList) {
        return leadPaymentStatusBaseDao.markLeadIdsAsNull(leadPaymentIdList);
    }

    /**
     * @param leadTypeId
     * @return
     */
    @Cacheable(value = Constants.CacheName.SELLER_PAYMENT_COUNT)
    public List<LeadPaymentStatusDto> findPaidSellersWithLastPaymentDateForLeadType(int leadTypeId) {
        return leadPaymentStatusBaseDao.findPaidSellersWithLastPaymentDateForLeadType(leadTypeId);

    }

    /**
     * 
     * @param sellerId
     * @param leadTypeId
     * @return
     */
    public Date findLastPaymentDateOfSeller(Integer sellerId, Integer leadTypeId) {
        return leadPaymentStatusBaseDao.findLastPaymentDateOfSeller(sellerId, leadTypeId);
    }

    /**
     * 
     * @param userId
     * @param date
     * @return
     */
    public List<Object[]> getPaidLeadsDistribution(int userId, Date date) {
        return leadPaymentStatusBaseDao.getPaidLeadsDistribution(userId, date);
    }

    /**
     * 
     * @param userId
     * @param date
     * @param leadTypeId
     * @return
     */
    public Integer getCountOfLeadsDisclosed(int userId, Date date, int leadTypeId) {
        return leadPaymentStatusBaseDao.getCountOfLeadsDisclosed(userId, date, leadTypeId).intValue();
    }

    /**
     * 
     * @param userId
     * @param cityId
     * @param saleTypeId
     * @return
     */
    public List<LeadPaymentStatus> findByCrmUserIdAndCityIdAndSaleTypeId(int userId, int cityId, int saleTypeId) {
        return leadPaymentStatusBaseDao.findByCrmUserIdAndCityIdAndSaleTypeId(userId, cityId, saleTypeId);
    }

    /**
     * 
     * @param userId
     * @param saleTypeId
     * @return
     */
    public List<LeadPaymentStatus> findByCrmUserIdAndSaleTypeId(int userId, int saleTypeId) {
        return leadPaymentStatusBaseDao.findByCrmUserIdAndSaleTypeId(userId, saleTypeId);
    }

    /**
     * 
     * @param leadId
     * @param statusId
     * @return
     */
    public List<LeadPaymentStatus> findByLeadIdAndStatusId(int leadId, int statusId) {
        return leadPaymentStatusBaseDao.findByLeadIdAndStatusId(leadId, statusId);
    }

    public Integer updateLeadAmount(Integer sellerId, Integer leadTypeId, Date createdAtLT, Integer amount) {        
        return leadPaymentStatusBaseDao.updateLeadAmountOfSellerSaleType(sellerId, leadTypeId, createdAtLT, amount);
    }

}
