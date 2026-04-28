package com.btg.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.btg.commission.entity.TodoReminder;
import com.btg.commission.enums.ReminderStateEnum;
import com.btg.commission.enums.ReminderTodoTypeEnum;
import com.btg.commission.mapper.TodoReminderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TodoReminderService {

    private final TodoReminderMapper todoReminderMapper;

    public void upsertOpen(
            ReminderTodoTypeEnum todoType,
            String businessType,
            Long businessId,
            Long ownerUserId,
            String sourceStatus,
            LocalDateTime sourceUpdatedAt) {
        if (todoType == null || businessId == null || ownerUserId == null || !hasText(businessType)) {
            return;
        }
        String dedupeKey = dedupeKey(todoType, businessType, businessId, ownerUserId);
        TodoReminder existing = todoReminderMapper.selectOne(new LambdaQueryWrapper<TodoReminder>()
                .eq(TodoReminder::getDedupeKey, dedupeKey)
                .eq(TodoReminder::getReminderState, ReminderStateEnum.OPEN)
                .last("LIMIT 1"));
        if (existing != null) {
            TodoReminder patch = new TodoReminder();
            patch.setId(existing.getId());
            patch.setSourceStatus(sourceStatus);
            patch.setSourceUpdatedAt(sourceUpdatedAt);
            // 兼容旧字段
            patch.setStatus("PENDING");
            patch.setTaskType(todoType.getCode());
            patch.setRelatedId(businessId);
            patch.setUserId(ownerUserId);
            todoReminderMapper.updateById(patch);
            return;
        }
        // 若历史存在 DONE/CANCELLED，优先复用同一条记录重新打开，避免唯一键 (dedupe_key, reminder_state) 冲突
        TodoReminder closed = todoReminderMapper.selectOne(new LambdaQueryWrapper<TodoReminder>()
                .eq(TodoReminder::getDedupeKey, dedupeKey)
                .in(TodoReminder::getReminderState, ReminderStateEnum.DONE, ReminderStateEnum.CANCELLED)
                .orderByDesc(TodoReminder::getUpdatedAt)
                .last("LIMIT 1"));
        if (closed != null) {
            TodoReminder patch = new TodoReminder();
            patch.setId(closed.getId());
            patch.setTodoType(todoType.getCode());
            patch.setBusinessType(businessType.trim());
            patch.setBusinessId(businessId);
            patch.setOwnerUserId(ownerUserId);
            patch.setReminderState(ReminderStateEnum.OPEN);
            patch.setSourceStatus(sourceStatus);
            patch.setSourceUpdatedAt(sourceUpdatedAt);
            patch.setResolvedAt(null);
            patch.setDedupeKey(dedupeKey);
            // 兼容旧字段
            patch.setStatus("PENDING");
            patch.setTaskType(todoType.getCode());
            patch.setRelatedId(businessId);
            patch.setUserId(ownerUserId);
            todoReminderMapper.updateById(patch);
            return;
        }
        TodoReminder row = new TodoReminder();
        row.setTodoType(todoType.getCode());
        row.setBusinessType(businessType.trim());
        row.setBusinessId(businessId);
        row.setOwnerUserId(ownerUserId);
        row.setReminderState(ReminderStateEnum.OPEN);
        row.setSourceStatus(sourceStatus);
        row.setSourceUpdatedAt(sourceUpdatedAt);
        row.setDedupeKey(dedupeKey);
        // 兼容旧字段
        row.setStatus("PENDING");
        row.setTaskType(todoType.getCode());
        row.setRelatedId(businessId);
        row.setUserId(ownerUserId);
        todoReminderMapper.insert(row);
    }

    public void resolveDone(ReminderTodoTypeEnum todoType, String businessType, Long businessId, Long ownerUserId) {
        close(todoType, businessType, businessId, ownerUserId, ReminderStateEnum.DONE);
    }

    public void resolveCancelled(ReminderTodoTypeEnum todoType, String businessType, Long businessId, Long ownerUserId) {
        close(todoType, businessType, businessId, ownerUserId, ReminderStateEnum.CANCELLED);
    }

    private void close(
            ReminderTodoTypeEnum todoType,
            String businessType,
            Long businessId,
            Long ownerUserId,
            ReminderStateEnum closedState) {
        if (todoType == null || businessId == null || ownerUserId == null || !hasText(businessType)) {
            return;
        }
        String key = dedupeKey(todoType, businessType, businessId, ownerUserId);
        TodoReminder open = todoReminderMapper.selectOne(new LambdaQueryWrapper<TodoReminder>()
                .eq(TodoReminder::getDedupeKey, key)
                .eq(TodoReminder::getReminderState, ReminderStateEnum.OPEN)
                .last("LIMIT 1"));
        if (open == null) {
            return;
        }
        todoReminderMapper.update(null, new LambdaUpdateWrapper<TodoReminder>()
                .set(TodoReminder::getReminderState, closedState)
                .set(TodoReminder::getResolvedAt, LocalDateTime.now())
                .set(TodoReminder::getStatus, "COMPLETED")
                .eq(TodoReminder::getId, open.getId())
                .eq(TodoReminder::getReminderState, ReminderStateEnum.OPEN));
    }

    private static String dedupeKey(ReminderTodoTypeEnum todoType, String businessType, Long businessId, Long ownerUserId) {
        return todoType.getCode() + ":" + businessType.trim() + ":" + businessId + ":" + ownerUserId;
    }

    private static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
