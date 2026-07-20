package com.yumian.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yumian.entity.InterviewQa;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface InterviewQaMapper extends BaseMapper<InterviewQa> {

    @Select("SELECT DATE(iq.created_at) AS date, COUNT(*) AS cnt " +
            "FROM interview_qa iq " +
            "JOIN interview_session s ON iq.session_id = s.id " +
            "WHERE s.user_id = #{userId} " +
            "AND iq.created_at BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY DATE(iq.created_at) " +
            "ORDER BY DATE(iq.created_at)")
    List<Map<String, Object>> countByDateWithUser(@Param("userId") Long userId,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);
}
