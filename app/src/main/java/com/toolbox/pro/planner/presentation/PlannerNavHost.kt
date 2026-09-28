package com.toolbox.pro.planner.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toolbox.pro.core.localization.LocalStrings

private object P {
    const val TODAY = "p_today"
    const val INBOX = "p_inbox"
    const val PROGRAMS = "p_programs"
    const val CALENDAR = "p_calendar"
    const val STATS = "p_stats"
    const val TASK = "p_task/{taskId}"
    const val PROGRAM = "p_program/{programId}"
    fun task(id: Long) = "p_task/$id"
    fun program(id: Long) = "p_program/$id"
}

/**
 * Planner module shell: its own Scaffold + bottom tab bar (Today / Inbox /
 * Programs / Calendar / Stats) with detail routes for task editor and program
 * detail. The bar hides on detail screens.
 */
@Composable
fun PlannerNavHost(onExitBack: (() -> Unit)? = null) {
    val nav = rememberNavController()
    val s = LocalStrings.current
    val vm: PlannerViewModel = hiltViewModel()

    val tabs: List<Triple<String, String, ImageVector>> = listOf(
        Triple(P.TODAY, s.tabToday, Icons.Filled.Today),
        Triple(P.INBOX, s.tabInbox, Icons.Filled.Inbox),
        Triple(P.PROGRAMS, s.tabPrograms, Icons.Filled.Folder),
        Triple(P.CALENDAR, s.tabCalendar, Icons.Filled.CalendarMonth),
        Triple(P.STATS, s.tabAnalytics, Icons.Filled.Assessment)
    )

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isDetail = currentRoute?.let { it.startsWith("p_task/") || it.startsWith("p_program/") } == true

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isDetail) {
                NavigationBar {
                    tabs.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == route,
                            onClick = {
                                nav.navigate(route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = P.TODAY,
            modifier = Modifier.padding(inner)
        ) {
            composable(P.TODAY) { TodayScreen(vm, onOpenTask = { nav.navigate(P.task(it)) }) }
            composable(P.INBOX) { InboxScreen(vm, onOpenTask = { nav.navigate(P.task(it)) }) }
            composable(P.PROGRAMS) { ProgramsScreen(vm, onOpenProgram = { nav.navigate(P.program(it)) }) }
            composable(P.CALENDAR) { CalendarScreen(vm, onOpenTask = { nav.navigate(P.task(it)) }) }
            composable(P.STATS) { StatsScreen(vm) }
            composable(
                P.PROGRAM,
                arguments = listOf(navArgument("programId") { type = NavType.LongType })
            ) { entry ->
                ProgramDetailScreen(
                    vm = vm,
                    programId = entry.arguments?.getLong("programId") ?: 0L,
                    onBack = { nav.popBackStack() },
                    onOpenTask = { nav.navigate(P.task(it)) }
                )
            }
            composable(
                P.TASK,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) { entry ->
                TaskEditorScreen(
                    vm = vm,
                    taskId = entry.arguments?.getLong("taskId") ?: 0L,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
