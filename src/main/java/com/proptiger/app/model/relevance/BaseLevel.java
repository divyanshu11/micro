package com.proptiger.app.model.relevance;

import java.util.List;

import com.proptiger.core.pojo.Pair;

public interface BaseLevel {

	public List<Pair<Integer, Integer>> getAttributeIdToRankIdPairList();
}
