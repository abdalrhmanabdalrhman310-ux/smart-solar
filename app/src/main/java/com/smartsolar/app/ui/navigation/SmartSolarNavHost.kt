package com.smartsolar.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartsolar.app.AppContainer
import com.smartsolar.app.domain.model.Product
import com.smartsolar.app.domain.model.UserRole
import com.smartsolar.app.domain.model.UserSession
import com.smartsolar.app.ui.screens.auth.AuthScreen
import com.smartsolar.app.ui.screens.client.CartScreen
import com.smartsolar.app.ui.screens.client.CheckoutScreen
import com.smartsolar.app.ui.screens.client.ClientHomeScreen
import com.smartsolar.app.ui.screens.client.ClientOrdersScreen
import com.smartsolar.app.ui.screens.client.EngineersScreen
import com.smartsolar.app.ui.screens.client.InstallationRequestScreen
import com.smartsolar.app.ui.screens.client.MaintenanceRequestScreen
import com.smartsolar.app.ui.screens.client.NotificationsScreen
import com.smartsolar.app.ui.screens.client.ProductDetailsScreen
import com.smartsolar.app.ui.screens.client.ProductsScreen
import com.smartsolar.app.ui.screens.client.ProfileScreen
import com.smartsolar.app.ui.screens.client.SolarCalculatorScreen
import com.smartsolar.app.ui.screens.engineer.EngineerHomeScreen
import com.smartsolar.app.ui.screens.engineer.EngineerRatingsScreen
import com.smartsolar.app.ui.screens.engineer.EngineerTaskDetailScreen
import com.smartsolar.app.ui.screens.engineer.EngineerTasksScreen
import com.smartsolar.app.ui.screens.onboarding.OnboardingPagerScreen
import com.smartsolar.app.ui.screens.onboarding.PendingApprovalScreen
import com.smartsolar.app.ui.screens.onboarding.SplashScreen
import com.smartsolar.app.ui.screens.onboarding.WelcomeScreen
import com.smartsolar.app.ui.screens.settings.ServerSettingsScreen
import com.smartsolar.app.ui.screens.vendor.VendorHomeScreen
import com.smartsolar.app.ui.screens.vendor.VendorOrdersScreen
import com.smartsolar.app.ui.screens.vendor.VendorProductFormScreen
import com.smartsolar.app.ui.screens.vendor.VendorProductsScreen
import com.smartsolar.app.ui.theme.SolarGreenDark
import com.smartsolar.app.ui.theme.SolarGreenLight
import com.smartsolar.app.ui.theme.SolarTextSecondary
import com.smartsolar.app.ui.theme.SolarWhite
import com.smartsolar.app.ui.viewmodel.AppSessionViewModel
import com.smartsolar.app.ui.viewmodel.AuthViewModel
import com.smartsolar.app.ui.viewmodel.ClientViewModel
import com.smartsolar.app.ui.viewmodel.EngineerViewModel
import com.smartsolar.app.ui.viewmodel.VendorViewModel

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun SmartSolarRoot(container: AppContainer) {
    val navController = rememberNavController()
    val appSessionViewModel: AppSessionViewModel = viewModel(factory = AppSessionViewModel.factory(container))
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val clientViewModel: ClientViewModel = viewModel(factory = ClientViewModel.factory(container))
    val engineerViewModel: EngineerViewModel = viewModel(factory = EngineerViewModel.factory(container))
    val vendorViewModel: VendorViewModel = viewModel(factory = VendorViewModel.factory(container))

    val rootState by appSessionViewModel.rootState.collectAsState()
    val sessionExpiredMsg by appSessionViewModel.sessionExpiredMessage.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var editingVendorProduct by remember { mutableStateOf<Product?>(null) }

    // Global session expiry listener redirects cleanly back to Auth
    LaunchedEffect(sessionExpiredMsg) {
        if (sessionExpiredMsg != null && currentRoute != Routes.AUTH) {
            navController.navigate(Routes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val clientNavItems = remember {
        listOf(
            BottomNavItem(Routes.CLIENT_HOME, "الرئيسية", Icons.Default.Home, "nav_client_home"),
            BottomNavItem(Routes.CLIENT_PRODUCTS, "المنتجات", Icons.Default.ShoppingBag, "nav_client_products"),
            BottomNavItem(Routes.CLIENT_CALCULATOR, "الحاسبة", Icons.Default.Calculate, "nav_client_calc"),
            BottomNavItem(Routes.CLIENT_ORDERS, "طلباتي", Icons.Default.ReceiptLong, "nav_client_orders"),
            BottomNavItem(Routes.CLIENT_PROFILE, "حسابي", Icons.Default.Person, "nav_client_profile")
        )
    }

    val engineerNavItems = remember {
        listOf(
            BottomNavItem(Routes.ENGINEER_HOME, "الرئيسية", Icons.Default.Home, "nav_eng_home"),
            BottomNavItem(Routes.ENGINEER_TASKS, "المهام", Icons.Default.Assignment, "nav_eng_tasks"),
            BottomNavItem(Routes.ENGINEER_RATINGS, "التقييمات", Icons.Default.Star, "nav_eng_ratings"),
            BottomNavItem(Routes.ENGINEER_PROFILE, "حسابي", Icons.Default.Person, "nav_eng_profile")
        )
    }

    val vendorNavItems = remember {
        listOf(
            BottomNavItem(Routes.VENDOR_HOME, "الرئيسية", Icons.Default.Home, "nav_vendor_home"),
            BottomNavItem(Routes.VENDOR_PRODUCTS, "منتجاتي", Icons.Default.Inventory2, "nav_vendor_products"),
            BottomNavItem(Routes.VENDOR_ORDERS, "الطلبات", Icons.Default.ReceiptLong, "nav_vendor_orders"),
            BottomNavItem(Routes.VENDOR_PROFILE, "حسابي", Icons.Default.Person, "nav_vendor_profile")
        )
    }

    val activeBottomItems = when {
        currentRoute in clientNavItems.map { it.route } -> clientNavItems
        currentRoute in engineerNavItems.map { it.route } -> engineerNavItems
        currentRoute in vendorNavItems.map { it.route } -> vendorNavItems
        else -> emptyList()
    }

    fun resolveDashboardRoute(session: UserSession): String {
        if (!session.isApprovedForDashboard) return Routes.PENDING_APPROVAL
        return when (session.role) {
            UserRole.CLIENT -> Routes.CLIENT_HOME
            UserRole.ENGINEER -> Routes.ENGINEER_HOME
            UserRole.VENDOR -> Routes.VENDOR_HOME
        }
    }

    Scaffold(
        bottomBar = {
            if (activeBottomItems.isNotEmpty()) {
                NavigationBar(
                    containerColor = SolarWhite,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    activeBottomItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag(item.testTag),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SolarGreenDark,
                                selectedTextColor = SolarGreenDark,
                                indicatorColor = SolarGreenLight,
                                unselectedIconColor = SolarTextSecondary,
                                unselectedTextColor = SolarTextSecondary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(
                        isInitialized = rootState.isInitialized,
                        onTimeout = {
                            val target = when {
                                rootState.session.isAuthenticated -> resolveDashboardRoute(rootState.session)
                                !rootState.onboardingCompleted -> Routes.WELCOME
                                else -> Routes.AUTH
                            }
                            navController.navigate(target) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.WELCOME) {
                    WelcomeScreen(
                        onStartOnboarding = { navController.navigate(Routes.ONBOARDING) },
                        onGoToLogin = {
                            appSessionViewModel.completeOnboarding()
                            navController.navigate(Routes.AUTH) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.ONBOARDING) {
                    OnboardingPagerScreen(
                        onFinished = {
                            appSessionViewModel.completeOnboarding()
                            navController.navigate(Routes.AUTH) {
                                popUpTo(Routes.WELCOME) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.AUTH) {
                    AuthScreen(
                        authViewModel = authViewModel,
                        serverConfig = rootState.serverConfig,
                        sessionExpiredBanner = sessionExpiredMsg,
                        onDismissSessionExpiredBanner = { appSessionViewModel.consumeSessionExpiredMessage() },
                        onAuthSuccess = { session ->
                            appSessionViewModel.consumeSessionExpiredMessage()
                            val dest = resolveDashboardRoute(session)
                            navController.navigate(dest) {
                                popUpTo(Routes.AUTH) { inclusive = true }
                            }
                        },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.PENDING_APPROVAL) {
                    PendingApprovalScreen(
                        session = rootState.session,
                        onRefreshStatus = {
                            // Re-check approval status from auth.php
                            navController.navigate(Routes.AUTH) {
                                popUpTo(Routes.PENDING_APPROVAL) { inclusive = true }
                            }
                        },
                        onLogout = {
                            appSessionViewModel.logout()
                            navController.navigate(Routes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Routes.SERVER_SETTINGS) {
                    ServerSettingsScreen(
                        appSessionViewModel = appSessionViewModel,
                        serverConfig = rootState.serverConfig,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // ================= CLIENT FLOW =================
                composable(Routes.CLIENT_HOME) {
                    ClientHomeScreen(
                        session = rootState.session,
                        clientViewModel = clientViewModel,
                        cartCount = rootState.cartCount,
                        onOpenProducts = { navController.navigate(Routes.CLIENT_PRODUCTS) },
                        onOpenProductDetail = { product ->
                            clientViewModel.selectProduct(product)
                            navController.navigate(Routes.CLIENT_PRODUCT_DETAIL)
                        },
                        onOpenCart = { navController.navigate(Routes.CLIENT_CART) },
                        onOpenCalculator = { navController.navigate(Routes.CLIENT_CALCULATOR) },
                        onOpenInstallation = { navController.navigate(Routes.CLIENT_INSTALLATION_REQUEST) },
                        onOpenMaintenance = { navController.navigate(Routes.CLIENT_MAINTENANCE_REQUEST) },
                        onOpenEngineers = { navController.navigate(Routes.CLIENT_ENGINEERS) },
                        onOpenNotifications = { navController.navigate(Routes.CLIENT_NOTIFICATIONS) },
                        onOpenOrders = { navController.navigate(Routes.CLIENT_ORDERS) },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.CLIENT_PRODUCTS) {
                    ProductsScreen(
                        clientViewModel = clientViewModel,
                        cartCount = rootState.cartCount,
                        onOpenProductDetail = { product ->
                            clientViewModel.selectProduct(product)
                            navController.navigate(Routes.CLIENT_PRODUCT_DETAIL)
                        },
                        onOpenCart = { navController.navigate(Routes.CLIENT_CART) },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.CLIENT_PRODUCT_DETAIL) {
                    val product by clientViewModel.selectedProduct.collectAsState()
                    ProductDetailsScreen(
                        product = product,
                        clientViewModel = clientViewModel,
                        onBackClick = { navController.popBackStack() },
                        onOpenCart = { navController.navigate(Routes.CLIENT_CART) }
                    )
                }

                composable(Routes.CLIENT_CART) {
                    CartScreen(
                        clientViewModel = clientViewModel,
                        onBackClick = { navController.popBackStack() },
                        onProceedToCheckout = { navController.navigate(Routes.CLIENT_CHECKOUT) },
                        onExploreProducts = { navController.navigate(Routes.CLIENT_PRODUCTS) }
                    )
                }

                composable(Routes.CLIENT_CHECKOUT) {
                    CheckoutScreen(
                        session = rootState.session,
                        clientViewModel = clientViewModel,
                        onBackClick = { navController.popBackStack() },
                        onOrderPlaced = {
                            navController.navigate(Routes.CLIENT_ORDERS) {
                                popUpTo(Routes.CLIENT_HOME)
                            }
                        }
                    )
                }

                composable(Routes.CLIENT_ORDERS) {
                    ClientOrdersScreen(
                        clientViewModel = clientViewModel,
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.CLIENT_CALCULATOR) {
                    SolarCalculatorScreen(
                        clientViewModel = clientViewModel,
                        onRequestInstallationWithDesign = {
                            navController.navigate(Routes.CLIENT_INSTALLATION_REQUEST)
                        }
                    )
                }

                composable(Routes.CLIENT_INSTALLATION_REQUEST) {
                    InstallationRequestScreen(
                        session = rootState.session,
                        clientViewModel = clientViewModel,
                        onSelectEngineer = { navController.navigate(Routes.CLIENT_ENGINEERS) },
                        onBackClick = { navController.popBackStack() },
                        onOpenOrders = {
                            navController.navigate(Routes.CLIENT_ORDERS) {
                                popUpTo(Routes.CLIENT_HOME)
                            }
                        }
                    )
                }

                composable(Routes.CLIENT_ENGINEERS) {
                    EngineersScreen(
                        clientViewModel = clientViewModel,
                        onEngineerSelectedAndReturn = { navController.popBackStack() },
                        onBackClick = { navController.popBackStack() },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.CLIENT_MAINTENANCE_REQUEST) {
                    MaintenanceRequestScreen(
                        session = rootState.session,
                        clientViewModel = clientViewModel,
                        onSelectEngineer = { navController.navigate(Routes.CLIENT_ENGINEERS) },
                        onBackClick = { navController.popBackStack() },
                        onOpenOrders = {
                            navController.navigate(Routes.CLIENT_ORDERS) {
                                popUpTo(Routes.CLIENT_HOME)
                            }
                        }
                    )
                }

                composable(Routes.CLIENT_NOTIFICATIONS) {
                    NotificationsScreen(
                        clientViewModel = clientViewModel,
                        onBackClick = { navController.popBackStack() },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.CLIENT_PROFILE) {
                    ProfileScreen(
                        session = rootState.session,
                        appSessionViewModel = appSessionViewModel,
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) },
                        onLoggedOut = {
                            navController.navigate(Routes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // ================= ENGINEER FLOW =================
                composable(Routes.ENGINEER_HOME) {
                    EngineerHomeScreen(
                        session = rootState.session,
                        engineerViewModel = engineerViewModel,
                        onOpenTasks = { navController.navigate(Routes.ENGINEER_TASKS) },
                        onOpenTaskDetail = { task ->
                            engineerViewModel.selectTask(task)
                            navController.navigate(Routes.ENGINEER_TASK_DETAIL)
                        },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.ENGINEER_TASKS) {
                    EngineerTasksScreen(
                        session = rootState.session,
                        engineerViewModel = engineerViewModel,
                        onOpenTaskDetail = { task ->
                            engineerViewModel.selectTask(task)
                            navController.navigate(Routes.ENGINEER_TASK_DETAIL)
                        },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.ENGINEER_TASK_DETAIL) {
                    EngineerTaskDetailScreen(
                        session = rootState.session,
                        engineerViewModel = engineerViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Routes.ENGINEER_RATINGS) {
                    EngineerRatingsScreen(
                        session = rootState.session,
                        engineerViewModel = engineerViewModel,
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.ENGINEER_PROFILE) {
                    ProfileScreen(
                        session = rootState.session,
                        appSessionViewModel = appSessionViewModel,
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) },
                        onLoggedOut = {
                            navController.navigate(Routes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                // ================= VENDOR FLOW =================
                composable(Routes.VENDOR_HOME) {
                    VendorHomeScreen(
                        session = rootState.session,
                        vendorViewModel = vendorViewModel,
                        onAddProduct = {
                            editingVendorProduct = null
                            navController.navigate(Routes.VENDOR_PRODUCT_FORM)
                        },
                        onManageProducts = { navController.navigate(Routes.VENDOR_PRODUCTS) },
                        onViewOrders = { navController.navigate(Routes.VENDOR_ORDERS) },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.VENDOR_PRODUCTS) {
                    VendorProductsScreen(
                        session = rootState.session,
                        vendorViewModel = vendorViewModel,
                        onAddProduct = {
                            editingVendorProduct = null
                            navController.navigate(Routes.VENDOR_PRODUCT_FORM)
                        },
                        onEditProduct = { product ->
                            editingVendorProduct = product
                            navController.navigate(Routes.VENDOR_PRODUCT_FORM)
                        },
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.VENDOR_PRODUCT_FORM) {
                    VendorProductFormScreen(
                        session = rootState.session,
                        editingProduct = editingVendorProduct,
                        vendorViewModel = vendorViewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Routes.VENDOR_ORDERS) {
                    VendorOrdersScreen(
                        vendorViewModel = vendorViewModel,
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) }
                    )
                }

                composable(Routes.VENDOR_PROFILE) {
                    ProfileScreen(
                        session = rootState.session,
                        appSessionViewModel = appSessionViewModel,
                        onOpenServerSettings = { navController.navigate(Routes.SERVER_SETTINGS) },
                        onLoggedOut = {
                            navController.navigate(Routes.AUTH) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
