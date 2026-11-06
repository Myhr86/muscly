package no.muscly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import no.muscly.app.domain.DayType
import no.muscly.app.ui.screens.*
import no.muscly.app.ui.theme.MusclyTheme

data class Tab(val route: String, val label: String, val icon: ImageVector, val iconSel: ImageVector)

val tabs = listOf(
    Tab("home", "Hjem", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("training", "Trening", Icons.Outlined.FitnessCenter, Icons.Filled.FitnessCenter),
    Tab("progress", "Fremgang", Icons.Outlined.Insights, Icons.Filled.Insights),
    Tab("coach", "AI-trener", Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    Tab("profile", "Profil", Icons.Outlined.Person, Icons.Filled.Person),
)

fun NavHostController.startWorkout(type: DayType, minutes: Int? = null) =
    navigate("workout/${type.name}?min=${minutes ?: -1}")

@Composable
fun MusclyRoot(vm: AppViewModel, nav: NavHostController = rememberNavController()) {
    val st by vm.state.collectAsStateWithLifecycle()
    MusclyTheme(st.profile?.theme ?: "dark") {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                !st.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                st.profile?.onboarded != true -> OnboardingScreen(onDone = { p, sa -> vm.completeOnboarding(p, sa) })
                else -> MainScaffold(vm, st, nav)
            }
        }
    }
}

@Composable
private fun MainScaffold(vm: AppViewModel, st: AppState, nav: NavHostController) {
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val showBar = tabs.any { it.route == route }
    Scaffold(
        bottomBar = {
            if (showBar) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                tabs.forEach { t ->
                    val sel = route == t.route
                    NavigationBarItem(selected = sel, onClick = {
                        if (!sel) nav.navigate(t.route) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true }
                    }, icon = { Icon(if (sel) t.iconSel else t.icon, t.label) }, label = { Text(t.label) })
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { pad ->
        NavHost(nav, "home", Modifier.padding(pad)) {
            composable("home") { HomeScreen(st, nav) }
            composable("training") { TrainingScreen(st, nav) }
            composable("progress") { ProgressScreen(st) }
            composable("coach") { CoachScreen(vm, st, nav) }
            composable("profile") { ProfileScreen(vm, st, nav) }
            composable("parent") { ParentScreen(vm, st, nav) }
            composable("exercise/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
                ExerciseDetailScreen(it.arguments?.getString("id") ?: "squat", st, nav)
            }
            composable("workout/{type}?min={min}", listOf(navArgument("type") { type = NavType.StringType }, navArgument("min") { type = NavType.IntType; defaultValue = -1 })) {
                val type = runCatching { DayType.valueOf(it.arguments?.getString("type") ?: "") }.getOrDefault(DayType.HELKROPP)
                val min = it.arguments?.getInt("min")?.takeIf { m -> m > 0 }
                WorkoutScreen(vm, st, type, min, nav)
            }
            composable("summary") { SummaryScreen(vm, st, nav) }
        }
    }
}
