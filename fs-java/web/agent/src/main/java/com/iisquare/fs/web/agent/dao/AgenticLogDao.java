package com.iisquare.fs.web.agent.dao;

import com.iisquare.fs.base.jpa.mvc.DaoBase;
import com.iisquare.fs.web.agent.entity.AgenticLog;

import java.util.List;

public interface AgenticLogDao extends DaoBase<AgenticLog, Integer> {

    /**
     * 会话回看用的轻量投影：日志列表与「回复 ↔ 日志」关联只需要这些小字段，
     * 避免把 inputs / outputs / steps 三个 longtext 大字段整行读出来
     */
    interface LogIndex {
        Integer getId();
        Integer getAnswerId();
        Integer getAgenticId();
        Integer getStatus();
        Long getDuration();
        String getError();
        String getFailures();
        Long getCreatedTime();
    }

    List<LogIndex> findByChatIdAndDeletedTimeOrderByIdAsc(Integer chatId, Long deletedTime);
}
