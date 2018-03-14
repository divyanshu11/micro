package com.proptiger.app.repo.table;

import java.util.Collection;
import java.util.List;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.proptiger.core.model.cms.TableAttributes;

/*
 * Copied from petra just for the time..@Divyanshu
 */
public interface TableAttributesDao extends PagingAndSortingRepository<TableAttributes, Long> {

List<TableAttributes> findByTableIdAndTableName(int tableId, String tableName);
    
    List<TableAttributes> findByTableIdInAndTableNameAndAttributeNameIn(List<Integer> tableIds, String tableName, Collection<String> attributeNames);
}
