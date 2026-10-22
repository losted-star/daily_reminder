package com.dailylist.app.domain

import com.dailylist.app.data.RepeatRule
import com.dailylist.app.data.Task
import java.time.LocalDate

object RepeatLogic {

    /** 任务是否「适用」于某一天（即当天是否需要处理该任务） */
    fun isApplicable(task: Task, date: LocalDate): Boolean {
        val due = DateUtils.parseDate(task.dueDate)
        return when (task.repeatRule) {
            RepeatRule.NONE -> due == date
            RepeatRule.DAILY -> !date.isBefore(due)
            RepeatRule.WEEKLY -> !date.isBefore(due) && task.repeatDays.contains(date.dayOfWeek.value)
            // 每月重复：取「创建日」的日号；对不存在的日号（如 31 号）当月跳过
            RepeatRule.MONTHLY -> !date.isBefore(due) && date.dayOfMonth == due.dayOfMonth
        }
    }
}
