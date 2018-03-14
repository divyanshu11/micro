package com.proptiger.app.model.relevance;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.proptiger.core.model.cms.MasterSellerVisibilityAttributes.VisibilityAttributes;
import com.proptiger.core.model.cms.MasterSellerVisibiltyRanks.VisibilityRanks;
import com.proptiger.core.pojo.Pair;

@Component
public class Level2 implements BaseLevel{

private List<Pair<Integer, Integer>> attributeIdToRankIdPairList = new ArrayList<>();
    
    Level2(){
        Pair<Integer, Integer> pair1 = new Pair<>(VisibilityAttributes.SERP.getId(), VisibilityRanks.MOUNTAIN.getId());
        Pair<Integer, Integer> pair2 =
                new Pair<>(VisibilityAttributes.MULTIPLICATION.getId(), VisibilityRanks.TRENCH.getId());
        attributeIdToRankIdPairList.add(pair1);
        attributeIdToRankIdPairList.add(pair2);
    }
    
    public List<Pair<Integer, Integer>> getAttributeIdToRankIdPairList() {
        return attributeIdToRankIdPairList;
    }

    public void setAttributeIdToRankIdPairList(List<Pair<Integer, Integer>> attributeIdToRankIdPairList) {
        this.attributeIdToRankIdPairList = attributeIdToRankIdPairList;
    }
}
