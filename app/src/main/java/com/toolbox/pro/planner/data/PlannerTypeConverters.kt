package com.toolbox.pro.planner.data

import androidx.room.TypeConverter

class PlannerTypeConverters {
    @TypeConverter fun programStatusToString(v: ProgramStatus): String = v.name
    @TypeConverter fun stringToProgramStatus(v: String): ProgramStatus = ProgramStatus.valueOf(v)

    @TypeConverter fun goalStatusToString(v: GoalStatus): String = v.name
    @TypeConverter fun stringToGoalStatus(v: String): GoalStatus = GoalStatus.valueOf(v)

    @TypeConverter fun milestoneStatusToString(v: MilestoneStatus): String = v.name
    @TypeConverter fun stringToMilestoneStatus(v: String): MilestoneStatus = MilestoneStatus.valueOf(v)

    @TypeConverter fun taskStatusToString(v: TaskStatus): String = v.name
    @TypeConverter fun stringToTaskStatus(v: String): TaskStatus = TaskStatus.valueOf(v)

    @TypeConverter fun priorityToString(v: Priority): String = v.name
    @TypeConverter fun stringToPriority(v: String): Priority = Priority.valueOf(v)

    @TypeConverter fun energyLevelToString(v: EnergyLevel): String = v.name
    @TypeConverter fun stringToEnergyLevel(v: String): EnergyLevel = EnergyLevel.valueOf(v)

    @TypeConverter fun reviewTypeToString(v: ReviewType): String = v.name
    @TypeConverter fun stringToReviewType(v: String): ReviewType = ReviewType.valueOf(v)
}
