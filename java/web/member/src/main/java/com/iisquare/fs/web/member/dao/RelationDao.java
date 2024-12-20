package com.iisquare.fs.web.member.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.member.entity.Relation;

import java.util.Collection;
import java.util.List;

public interface RelationDao extends DaoBase<Relation, String> {

    List<Relation> findAllByTypeAndAid(String type, Integer aid);

    List<Relation> findAllByTypeAndAidAndCid(String type, Integer aid, Integer cid);

    List<Relation> findAllByTypeAndAidIn(String type, Collection<Integer> aids);

    List<Relation> findAllByTypeAndAidInAndCidIn(String type, Collection<Integer> aids, Collection<Integer> cids);

    List<Relation> findAllByTypeAndBidIn(String type, Collection<Integer> bids);

}
