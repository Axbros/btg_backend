package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.UserProfile;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserProfileMapper extends BaseMapper<UserProfile> {

    @Select("SELECT * FROM btg_user_profile WHERE user_id = #{userId} AND deleted_at IS NULL FOR UPDATE")
    UserProfile selectByUserIdForUpdate(@Param("userId") Long userId);
}
