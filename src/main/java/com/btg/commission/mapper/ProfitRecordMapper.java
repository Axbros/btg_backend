package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.ProfitRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProfitRecordMapper extends BaseMapper<ProfitRecord> {

    @Select("SELECT * FROM btg_profit_record WHERE id = #{id} FOR UPDATE")
    ProfitRecord selectByIdForUpdate(@Param("id") Long id);
}
