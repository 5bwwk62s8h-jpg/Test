package com.payroll.calculator.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.payroll.calculator.R
import com.payroll.calculator.ui.employees.EmployeeEditScreen
import com.payroll.calculator.ui.employees.EmployeeListScreen
import com.payroll.calculator.ui.report.ReportScreen

private object Routes {
    const val EMPLOYEES = "employees"
    const val REPORT = "report"
    const val EMPLOYEE_NEW = "employee/new"
    const val EMPLOYEE_EDIT = "employee/{id}"

    fun employeeEdit(id: String) = "employee/$id"
}

private data class TopLevelDestination(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.EMPLOYEES, R.string.nav_employees, Icons.Filled.Groups),
    TopLevelDestination(Routes.REPORT, R.string.nav_report, Icons.Filled.Receipt),
)

@Composable
fun PayrollNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    Scaffold(
        bottomBar = {
            if (currentRoute == Routes.EMPLOYEES || currentRoute == Routes.REPORT) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.EMPLOYEES,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.EMPLOYEES) {
                EmployeeListScreen(
                    onAddEmployee = { navController.navigate(Routes.EMPLOYEE_NEW) },
                    onOpenEmployee = { id -> navController.navigate(Routes.employeeEdit(id)) },
                )
            }
            composable(Routes.REPORT) {
                ReportScreen()
            }
            composable(Routes.EMPLOYEE_NEW) {
                EmployeeEditScreen(employeeId = null, onDone = { navController.popBackStack() })
            }
            composable(
                route = Routes.EMPLOYEE_EDIT,
                arguments = listOf(navArgument("id") { }),
            ) { entry ->
                val id = entry.arguments?.getString("id")
                EmployeeEditScreen(employeeId = id, onDone = { navController.popBackStack() })
            }
        }
    }
}
