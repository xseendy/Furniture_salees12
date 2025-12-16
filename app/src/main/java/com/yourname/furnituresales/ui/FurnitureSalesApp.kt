package com.yourname.furnituresales.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import coil.compose.AsyncImage
import com.yourname.furnituresales.FurnitureSalesViewModel
import com.yourname.furnituresales.FurnitureUiState
import com.yourname.furnituresales.R
import com.yourname.furnituresales.data.CartItem
import com.yourname.furnituresales.data.Product
import com.yourname.furnituresales.ui.screens.AuthScreen
import com.yourname.furnituresales.ui.screens.AccountScreen
import com.yourname.furnituresales.ui.screens.CatalogScreen
import com.yourname.furnituresales.ui.screens.CartScreen
import com.yourname.furnituresales.ui.theme.FurnitureSalesTheme
import com.yourname.furnituresales.ui.util.USD_TO_RUB
import com.yourname.furnituresales.ui.util.formatPriceRub

private enum class AppTab { HOME, CART, ACCOUNT }

private object Routes {
    const val HOME = "home"
    const val CART = "cart"
    const val ACCOUNT = "account"
    const val PRODUCT = "product"
    const val PRODUCT_ARG_ID = "id"
}

@Composable
fun FurnitureSalesApp(viewModel: FurnitureSalesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthed = uiState.userProfile != null
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()

    val msgAddedToCart = stringResource(R.string.snackbar_added_to_cart)
    val msgAddedToFavorites = stringResource(R.string.snackbar_added_to_favorites)
    val msgRemovedFromFavorites = stringResource(R.string.snackbar_removed_from_favorites)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isAuthed) {
                NavigationBar {
                    val destinationRoute = navController.currentBackStackEntry?.destination?.route
                    NavigationBarItem(
                        selected = destinationRoute == Routes.HOME,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        label = { Text(stringResource(R.string.tab_home)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = stringResource(R.string.tab_home)
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = destinationRoute == Routes.CART,
                        onClick = {
                            navController.navigate(Routes.CART) {
                                launchSingleTop = true
                            }
                        },
                        label = { Text(stringResource(R.string.tab_cart)) },
                        icon = {
                            BadgedBox(badge = {
                                if (uiState.cart.isNotEmpty()) {
                                    Badge { Text(uiState.cart.sumOf { it.quantity }.toString()) }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = stringResource(R.string.tab_cart)
                                )
                            }
                        }
                    )
                    NavigationBarItem(
                        selected = destinationRoute == Routes.ACCOUNT,
                        onClick = {
                            navController.navigate(Routes.ACCOUNT) {
                                launchSingleTop = true
                            }
                        },
                        label = { Text(stringResource(R.string.tab_profile)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = stringResource(R.string.tab_profile)
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        val baseModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 12.dp)

        if (!isAuthed) {
            Column(baseModifier) {
                AuthScreen(
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onSignIn = { email, password -> viewModel.signIn(email, password) },
                    onRegister = { email, password -> viewModel.signUp(email, password) },
                    onResetPassword = { email, password -> viewModel.resetPassword(email, password) },
                    onGuest = { viewModel.signInAnonymously() }
                )
            }
        } else {
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    CatalogScreen(
                        modifier = baseModifier,
                        uiState = uiState,
                        onRefresh = { viewModel.loadProducts() },
                        onAddToCart = {
                            viewModel.addToCart(it)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msgAddedToCart)
                            }
                        },
                        onProductClick = { product ->
                            navController.navigate("${Routes.PRODUCT}/${product.id}")
                        },
                        onToggleFavorite = { id ->
                            viewModel.toggleFavorite(id)
                            val isNowFavorite = !uiState.favorites.contains(id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(if (isNowFavorite) msgAddedToFavorites else msgRemovedFromFavorites)
                            }
                        }
                    )
                }

                composable(Routes.CART) {
                    CartScreen(
                        modifier = baseModifier,
                        uiState = uiState,
                        onIncrement = { viewModel.updateQuantity(it, 1) },
                        onDecrement = { viewModel.updateQuantity(it, -1) },
                        onRemoveItem = { viewModel.removeFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onCheckout = { viewModel.checkout() },
                        onPaymentSelected = { viewModel.setPaymentMethod(it) },
                        onDeliverySelected = { viewModel.setDeliveryMethod(it) },
                        onSelectSavedAddress = { viewModel.selectAddress(it) },
                        onGoToCatalog = { navController.navigate(Routes.HOME) }
                    )
                }

                composable(Routes.ACCOUNT) {
                    AccountScreen(
                        modifier = baseModifier,
                        uiState = uiState,
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onAddressChange = { viewModel.updateShippingAddress(it) },
                        onPhoneChange = { viewModel.updatePhone(it) },
                        onNameChange = { viewModel.updateDisplayName(it) },
                        onSaveProfile = { name, address, phone -> viewModel.saveProfile(name, address, phone) },
                        onChangePassword = { current, new -> viewModel.changePassword(current, new) },
                        profileMessage = uiState.profileMessage,
                        onAddFavoriteToCart = {
                            viewModel.addToCart(it)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msgAddedToCart)
                            }
                        },
                        onToggleFavorite = { id ->
                            val wasFavorite = uiState.favorites.contains(id)
                            viewModel.toggleFavorite(id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(if (!wasFavorite) msgAddedToFavorites else msgRemovedFromFavorites)
                            }
                        }
                    )
                }

                composable(
                    route = "${Routes.PRODUCT}/{${Routes.PRODUCT_ARG_ID}}",
                    arguments = listOf(navArgument(Routes.PRODUCT_ARG_ID) { type = NavType.StringType })
                ) {
                    val id = it.arguments?.getString(Routes.PRODUCT_ARG_ID)
                    val product = uiState.products.firstOrNull { p -> p.id == id }
                    if (product == null) {
                        Column(baseModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.err_load_products_failed, "not found"),
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = { navController.popBackStack() }) {
                                Text(stringResource(R.string.action_back))
                            }
                        }
                        return@composable
                    }

                    ProductDetailScreen(
                        product = product,
                        onBack = { navController.popBackStack() },
                        onAddToCart = { qty ->
                            viewModel.addToCart(product, qty)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msgAddedToCart)
                            }
                            navController.popBackStack()
                        },
                        isFavorite = uiState.favorites.contains(product.id),
                        onToggleFavorite = {
                            val wasFavorite = uiState.favorites.contains(product.id)
                            viewModel.toggleFavorite(product.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(if (!wasFavorite) msgAddedToFavorites else msgRemovedFromFavorites)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: (Int) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        var quantity by rememberSaveable { mutableStateOf(1) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.action_back))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatPriceRub(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.padding(start = 4.dp)) {
                        Text(if (isFavorite) "★" else "☆")
                    }
                }
            }

            val context = LocalContext.current
            val resId = product.imageResName?.let { name ->
                context.resources.getIdentifier(name, "drawable", context.packageName)
            }?.takeIf { it != 0 }
            val model: Any? = resId ?: product.imageUrl

            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onToggleFavorite) {
                            Text(if (isFavorite) "★" else "☆")
                        }
                    }
                    Text(product.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatPriceRub(product.price),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable {
                                    if (quantity > 1) quantity -= 1
                                },
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("−", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Text(
                            text = quantity.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { quantity += 1 },
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("+", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = formatPriceRub(product.price * quantity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { onAddToCart(quantity) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.action_add_to_cart_with_qty, quantity),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.product_specs_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (product.dimensions.isNotBlank()) {
                    Text(stringResource(R.string.product_specs_dimensions, product.dimensions), style = MaterialTheme.typography.bodySmall)
                }
                if (product.material.isNotBlank()) {
                    Text(stringResource(R.string.product_specs_material, product.material), style = MaterialTheme.typography.bodySmall)
                }
                if (product.color.isNotBlank()) {
                    Text(stringResource(R.string.product_specs_color, product.color), style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
private fun AuthPreview() {
    FurnitureSalesTheme {
        AuthScreen(
            isLoading = false,
            error = null,
            onSignIn = { _, _ -> },
            onRegister = { _, _ -> },
            onResetPassword = { _, _ -> },
            onGuest = {}
        )
    }
}

