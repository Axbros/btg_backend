package com.btg.commission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btg.commission.entity.BtgUser;
import com.btg.commission.vo.TeamMemberTreeRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BtgUserMapper extends BaseMapper<BtgUser> {

    @Select("SELECT * FROM btg_user WHERE id = #{id} AND deleted_at IS NULL FOR UPDATE")
    BtgUser selectByIdForUpdate(@Param("id") Long id);

    List<TeamMemberTreeRow> selectDescendantsForTree(@Param("pathPrefix") String pathPrefix);

    @Select("SELECT COUNT(*) FROM btg_user WHERE referrer_user_id = #{referrerUserId} AND deleted_at IS NULL")
    long countDirectChildren(@Param("referrerUserId") Long referrerUserId);

    long countAllDescendants(@Param("pathPrefix") String pathPrefix);
}
