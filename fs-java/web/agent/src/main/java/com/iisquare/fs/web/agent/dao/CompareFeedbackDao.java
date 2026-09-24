package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.CompareFeedback;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 反馈的数据访问：单轮回看按 runId 取，模型评分走聚合。
 */
public interface CompareFeedbackDao extends DaoBase<CompareFeedback, Integer> {

    List<CompareFeedback> findByRunIdAndDeletedTime(Integer runId, Long deletedTime);

    CompareFeedback findFirstByCallIdAndCreatedUidAndDeletedTime(Integer callId, Integer createdUid, Long deletedTime);

    /** 模型评分：赞与踩的条数，评分由服务层折算成百分比 */
    interface ModelScore {
        String getModel();
        Long getPositive();
        Long getNegative();
    }

    @Query("select f.model as model, "
            + "sum(case when f.emotion = 'positive' then 1 else 0 end) as positive, "
            + "sum(case when f.emotion = 'negative' then 1 else 0 end) as negative "
            + "from CompareFeedback f "
            + "where f.deletedTime = 0 and f.createdTime >= :beginTime and f.createdTime <= :endTime "
            + "and (:model = '' or f.model = :model) and (:uid = 0 or f.createdUid = :uid) "
            + "group by f.model")
    List<ModelScore> scoreByModel(@Param("beginTime") Long beginTime, @Param("endTime") Long endTime,
                                  @Param("model") String model, @Param("uid") Integer uid);

    /** 用户维度的评价次数：点赞与点踩合起来算一次参与评价 */
    interface UserScore {
        Integer getCreatedUid();
        Long getRated();
    }

    @Query("select f.createdUid as createdUid, count(f) as rated "
            + "from CompareFeedback f "
            + "where f.deletedTime = 0 and f.createdTime >= :beginTime and f.createdTime <= :endTime "
            + "and (:model = '' or f.model = :model) and (:uid = 0 or f.createdUid = :uid) "
            + "group by f.createdUid")
    List<UserScore> scoreByUser(@Param("beginTime") Long beginTime, @Param("endTime") Long endTime,
                                @Param("model") String model, @Param("uid") Integer uid);

}
