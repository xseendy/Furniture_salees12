package com.yourname.furnituresales.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import coil.compose.AsyncImage
import com.yourname.furnituresales.FurnitureSalesViewModel
import com.yourname.furnituresales.FurnitureUiState
import com.yourname.furnituresales.data.CartItem
import com.yourname.furnituresales.data.Product
import com.yourname.furnituresales.ui.theme.FurnitureSalesTheme

private enum class AppTab { HOME, CART, ACCOUNT }

private enum class AccountTab { PROFILE, PASSWORD, ORDERS }

private const val USD_TO_RUB = 90.0

private fun formatPriceRub(usdAmount: Double): String {
    val rub = usdAmount * USD_TO_RUB
    return String.format(Locale("ru", "RU"), "%,.0f ₽", rub)
}

@Composable
fun FurnitureSalesApp(viewModel: FurnitureSalesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthed = uiState.userProfile != null
    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (isAuthed) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == AppTab.HOME,
                        onClick = {
                            selectedProduct = null
                            currentTab = AppTab.HOME
                        },
                        label = { Text("Главная") },
                        icon = {}
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.CART,
                        onClick = {
                            selectedProduct = null
                            currentTab = AppTab.CART
                        },
                        label = { Text("Корзина") },
                        icon = {
                            BadgedBox(badge = {
                                if (uiState.cart.isNotEmpty()) {
                                    Badge { Text(uiState.cart.sumOf { it.quantity }.toString()) }
                                }
                            }) { Text("🛒") }
                        }
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.ACCOUNT,
                        onClick = {
                            selectedProduct = null
                            currentTab = AppTab.ACCOUNT
                        },
                        label = { Text("Профиль") },
                        icon = {}
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
            when (currentTab) {
                AppTab.HOME -> ProductListScreen(
                    modifier = baseModifier,
                    uiState = uiState,
                    onRefresh = { viewModel.loadProducts() },
                    onAddToCart = { 
                        viewModel.addToCart(it)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            snackbarHostState.showSnackbar("Товар добавлен в корзину")
                        }
                    },
                    onProductClick = { selectedProduct = it },
                    onToggleFavorite = { id -> 
                        viewModel.toggleFavorite(id)
                        val isNowFavorite = !uiState.favorites.contains(id)
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            snackbarHostState.showSnackbar(if (isNowFavorite) "Добавлено в избранное" else "Удалено из избранного")
                        }
                    }
                )
                AppTab.CART -> CartScreen(
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
                    onGoToCatalog = { currentTab = AppTab.HOME }
                )
                AppTab.ACCOUNT -> AccountScreen(
                    modifier = baseModifier,
                    uiState = uiState,
                    onSignOut = { viewModel.signOut() },
                    onAddressChange = { viewModel.updateShippingAddress(it) },
                    onPhoneChange = { viewModel.updatePhone(it) },
                    onNameChange = { viewModel.updateDisplayName(it) },
                    onSaveProfile = { name, address, phone -> viewModel.saveProfile(name, address, phone) },
                    onChangePassword = { current, new -> viewModel.changePassword(current, new) },
                    profileMessage = uiState.profileMessage,
                    onAddFavoriteToCart = { 
                        viewModel.addToCart(it)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Товар добавлен в корзину")
                        }
                    },
                    onToggleFavorite = { id -> 
                        val wasFavorite = uiState.favorites.contains(id)
                        viewModel.toggleFavorite(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(if (!wasFavorite) "Добавлено в избранное" else "Удалено из избранного")
                        }
                    }
                )
            }
        }

        selectedProduct?.let { product ->
            ProductDetailScreen(
                product = product,
                onBack = { selectedProduct = null },
                onAddToCart = {
                    viewModel.addToCart(product)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Товар добавлен в корзину")
                    }
                    selectedProduct = null
                },
                isFavorite = uiState.favorites.contains(product.id),
                onToggleFavorite = { 
                    val wasFavorite = uiState.favorites.contains(product.id)
                    viewModel.toggleFavorite(product.id)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (!wasFavorite) "Добавлено в избранное" else "Удалено из избранного")
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthScreen(
    isLoading: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onResetPassword: (String, String) -> Unit,
    onGuest: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isResetMode by remember { mutableStateOf(false) }
    val isFormValid = email.isNotBlank() && password.length >= 6 && email.contains("@")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Магазин мебели", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            text = when {
                isResetMode -> "Восстановление доступа"
                isRegisterMode -> "Создать аккаунт"
                else -> "Добро пожаловать"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.padding(12.dp))
        AuthFieldCard {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.padding(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isResetMode) "Новый пароль" else "Пароль") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Скрыть" else "Показать")
                    }
                }
            )
        }
        Spacer(modifier = Modifier.padding(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && isFormValid,
            onClick = {
                when {
                    isResetMode -> onResetPassword(email, password)
                    isRegisterMode -> onRegister(email, password)
                    else -> onSignIn(email, password)
                }
            }
        ) {
            Text(
                when {
                    isResetMode -> "Сбросить пароль"
                    isRegisterMode -> "Зарегистрироваться"
                    else -> "Войти"
                }
            )
        }
        TextButton(onClick = {
            isRegisterMode = !isRegisterMode
            if (isRegisterMode) isResetMode = false
        }) {
            Text(if (isRegisterMode) "Уже есть аккаунт? Войти" else "Впервые? Создать аккаунт")
        }
        TextButton(onClick = {
            isResetMode = !isResetMode
            if (isResetMode) isRegisterMode = false
        }) {
            Text(if (isResetMode) "Вернуться ко входу" else "Забыли пароль?")
        }
        TextButton(onClick = { if (!isLoading) onGuest() }) {
            Text("Продолжить как гость")
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        if (isLoading) {
            Spacer(modifier = Modifier.padding(8.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ProductListScreen(
    modifier: Modifier = Modifier,
    uiState: FurnitureUiState,
    onRefresh: () -> Unit,
    onAddToCart: (Product) -> Unit,
    onProductClick: (Product) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var priceFilter by rememberSaveable { mutableStateOf("ALL") }
    var minPriceInput by rememberSaveable { mutableStateOf("") }
    var maxPriceInput by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf("NONE") }

    // Учитываем все параметры фильтров и избранное,
    // чтобы список действительно обновлялся при их изменении.
    val filteredProducts = remember(
        uiState.products,
        priceFilter,
        minPriceInput,
        maxPriceInput,
        favoritesOnly,
        sortOrder,
        uiState.favorites
    ) {
        uiState.products
            .filter { product ->
                val priceRub = product.price * USD_TO_RUB
                val priceMatches = when {
                    // Если пользователь ввел диапазон цен, используем его
                    minPriceInput.isNotBlank() || maxPriceInput.isNotBlank() -> {
                        val minPrice = minPriceInput.toDoubleOrNull() ?: 0.0
                        val maxPrice = maxPriceInput.toDoubleOrNull() ?: Double.MAX_VALUE
                        priceRub >= minPrice && priceRub <= maxPrice
                    }
                    // Иначе используем быстрые фильтры
                    priceFilter == "LOW" -> priceRub < 40_000
                    priceFilter == "MID" -> priceRub in 40_000.0..80_000.0
                    priceFilter == "HIGH" -> priceRub > 80_000
                    else -> true
                }
                val favoritesMatch =
                    !favoritesOnly || uiState.favorites.contains(product.id)
                priceMatches && favoritesMatch
            }
            .let { list ->
                when (sortOrder) {
                    "PRICE_ASC" -> list.sortedBy { it.price }
                    "PRICE_DESC" -> list.sortedByDescending { it.price }
                    else -> list
                }
            }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val nameOrEmail = uiState.userProfile?.displayName
                    ?: uiState.userProfile?.email
                    ?: "гость"
                Text(
                    text = "Здравствуйте, $nameOrEmail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Выберите мебель для гостиной, спальни и рабочего кабинета",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HeroBanner()
        Text("Фильтр по цене", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = priceFilter == "ALL" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                onClick = { 
                    priceFilter = "ALL"
                    minPriceInput = ""
                    maxPriceInput = ""
                },
                label = { Text("Все") }
            )
            FilterChip(
                selected = priceFilter == "LOW" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                onClick = { 
                    priceFilter = "LOW"
                    minPriceInput = ""
                    maxPriceInput = ""
                },
                label = { Text("До 40 тыс.", maxLines = 1) }
            )
            FilterChip(
                selected = priceFilter == "MID" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                onClick = { 
                    priceFilter = "MID"
                    minPriceInput = ""
                    maxPriceInput = ""
                },
                label = { Text("40–80 тыс.", maxLines = 1) }
            )
            FilterChip(
                selected = priceFilter == "HIGH" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                onClick = { 
                    priceFilter = "HIGH"
                    minPriceInput = ""
                    maxPriceInput = ""
                },
                label = { Text("80 тыс.+", maxLines = 1) }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = minPriceInput,
                onValueChange = { 
                    minPriceInput = it
                    if (it.isNotBlank()) priceFilter = "CUSTOM"
                },
                label = { Text("От, ₽") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("0") }
            )
            Text("—", modifier = Modifier.padding(horizontal = 4.dp))
            OutlinedTextField(
                value = maxPriceInput,
                onValueChange = { 
                    maxPriceInput = it
                    if (it.isNotBlank()) priceFilter = "CUSTOM"
                },
                label = { Text("До, ₽") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("∞") }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text("Только избранное") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = sortOrder == "PRICE_ASC",
                    onClick = { sortOrder = if (sortOrder == "PRICE_ASC") "NONE" else "PRICE_ASC" },
                    label = { Text("Цена ↑") }
                )
                FilterChip(
                    selected = sortOrder == "PRICE_DESC",
                    onClick = { sortOrder = if (sortOrder == "PRICE_DESC") "NONE" else "PRICE_DESC" },
                    label = { Text("Цена ↓") }
                )
            }
        }
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.padding(8.dp))
            CircularProgressIndicator()
        }
        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(filteredProducts) { product ->
                ProductCard(
                    product = product,
                    onAddToCart = { onAddToCart(product) },
                    onProductClick = { onProductClick(product) },
                    onToggleFavorite = { onToggleFavorite(product.id) },
                    isFavorite = uiState.favorites.contains(product.id)
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onAddToCart: () -> Unit,
    onProductClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean
) {
    val context = LocalContext.current
    val resId = product.imageResName?.let { name ->
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }?.takeIf { it != 0 }
    val model: Any? = resId ?: product.imageUrl

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder(enabled = true),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProductClick() }
            ) {
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = product.name,
                        modifier = Modifier
                            .height(110.dp)
                            .fillMaxWidth(0.35f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .height(110.dp)
                            .fillMaxWidth(0.35f)
                            .clip(RoundedCornerShape(10.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onToggleFavorite) {
                            Text(if (isFavorite) "★" else "☆")
                        }
                    }
                    Text(
                        product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = listOf(product.dimensions, product.material, product.color)
                            .filter { it.isNotBlank() }
                            .joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        formatPriceRub(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = product.material,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        TextButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(if (isFavorite) "★" else "☆", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Button(
                        onClick = onAddToCart,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("В корзину", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    Text("Назад")
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
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Характеристики", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (product.dimensions.isNotBlank()) {
                    Text("Размеры: ${product.dimensions}", style = MaterialTheme.typography.bodySmall)
                }
                if (product.material.isNotBlank()) {
                    Text("Материал: ${product.material}", style = MaterialTheme.typography.bodySmall)
                }
                if (product.color.isNotBlank()) {
                    Text("Цвет: ${product.color}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier.weight(2f)
                    ) {
                        Text("Добавить в корзину")
                    }
                    OutlinedCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        TextButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isFavorite) "В избранном" else "В избранное", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartScreen(
    modifier: Modifier = Modifier,
    uiState: FurnitureUiState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onPaymentSelected: (String) -> Unit,
    onDeliverySelected: (String) -> Unit,
    onSelectSavedAddress: (String) -> Unit,
    onGoToCatalog: () -> Unit
) {
    val total = uiState.cart.sumOf { it.product.price * it.quantity }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Корзина", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // Summary chip for quick glance
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val itemsCount = uiState.cart.sumOf { it.quantity }
                Column {
                    Text(
                        text = if (itemsCount == 0) "Корзина пуста" else "Товаров: $itemsCount",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (itemsCount == 0) "Добавьте товары из каталога" else "Предварительный итог",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPriceRub(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (uiState.checkoutMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = uiState.checkoutMessage,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (uiState.cart.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("В вашей корзине пока нет товаров.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Перейдите в каталог и добавьте понравившиеся позиции.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onGoToCatalog,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("К каталогу")
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.cart.forEachIndexed { index, item ->
                        CartRow(
                            item = item,
                            onIncrement = onIncrement,
                            onDecrement = onDecrement,
                            onRemove = onRemoveItem
                        )
                        if (index < uiState.cart.lastIndex) Divider()
                    }
                }
            }
        }

        if (uiState.cart.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Итого к оплате",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatPriceRub(total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Button(
                        onClick = onCheckout,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.cart.isNotEmpty()
                    ) {
                        Text("Перейти к оформлению")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onClearCart) { Text("Очистить корзину") }
                        Text(
                            text = "Товаров: ${uiState.cart.sumOf { it.quantity }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (uiState.cart.isNotEmpty()) {
            Text("Доставка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Quick selection of saved addresses from аккаунт
            if (uiState.addresses.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.addresses.forEach { address ->
                        FilterChip(
                            selected = uiState.shippingAddress == address.line,
                            onClick = { onSelectSavedAddress(address.id) },
                            label = { Text(address.label) }
                        )
                    }
                }
            }
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Адрес доставки: ${uiState.shippingAddress.ifBlank { "не указан" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Телефон: ${uiState.phone.ifBlank { "не указан" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Изменить адрес и телефон можно в профиле",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text("Способ оплаты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        PaymentSelector(selected = uiState.paymentMethod, onSelect = onPaymentSelected)

        Text("Способ доставки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = uiState.deliveryMethod == "courier", onClick = { onDeliverySelected("courier") })
                Text("Курьер")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = uiState.deliveryMethod == "pickup", onClick = { onDeliverySelected("pickup") })
                Text("Самовывоз")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = uiState.deliveryMethod == "post", onClick = { onDeliverySelected("post") })
                Text("Почта")
            }
        }

        if (uiState.error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    uiState.error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Создайте уютный дом", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Подберите готовые решения для гостиной, спальни, столовой и рабочего кабинета.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuthFieldCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun PaymentSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("Card", "Cash on Delivery", "Bank Transfer")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == option, onClick = { onSelect(option) })
                val label = when (option) {
                    "Card" -> "Карта"
                    "Cash on Delivery" -> "Наличные"
                    "Bank Transfer" -> "Банковский перевод"
                    else -> option
                }
                Text(label)
            }
        }
    }
}

@Composable
private fun AccountScreen(
    modifier: Modifier = Modifier,
    uiState: FurnitureUiState,
    onSignOut: () -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSaveProfile: (String, String, String) -> Unit,
    onChangePassword: (String, String) -> Unit,
    profileMessage: String?,
    onAddFavoriteToCart: (Product) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(AccountTab.PROFILE) }
    var ordersFilter by rememberSaveable { mutableStateOf("ALL") }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }

    // синхронизируем локальные поля при смене пользователя
    androidx.compose.runtime.LaunchedEffect(uiState.userProfile?.uid) {
        name = uiState.userProfile?.displayName.orEmpty()
        address = uiState.shippingAddress
        phone = uiState.phone
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("Профиль", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Вы вошли как ${uiState.userProfile?.email ?: "Гость"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = { selectedTab = AccountTab.PROFILE }) {
                Text(
                    "Данные",
                    fontWeight = if (selectedTab == AccountTab.PROFILE) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            TextButton(onClick = { selectedTab = AccountTab.PASSWORD }) {
                Text(
                    "Пароль",
                    fontWeight = if (selectedTab == AccountTab.PASSWORD) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            TextButton(onClick = { selectedTab = AccountTab.ORDERS }) {
                Text(
                    "Заказы",
                    fontWeight = if (selectedTab == AccountTab.ORDERS) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        when (selectedTab) {
            AccountTab.PROFILE -> {
                Text("Данные аккаунта", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = uiState.userProfile?.email.orEmpty(),
                    onValueChange = { /* email только для просмотра */ },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес") },
                    placeholder = { Text("Улица, город, индекс") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    placeholder = { Text("+7 900 123 4567") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSaveProfile(name, address, phone)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить данные")
                }
                if (profileMessage != null) {
                    Text(
                        text = profileMessage,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            AccountTab.PASSWORD -> {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Текущий пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Новый пароль") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    onClick = {
                        onChangePassword(currentPassword, newPassword)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сменить пароль")
                }
                if (profileMessage != null) {
                    Text(
                        text = profileMessage,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            AccountTab.ORDERS -> {
                Text("Избранное", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val favoriteProducts = uiState.products.filter { uiState.favorites.contains(it.id) }
                if (favoriteProducts.isEmpty()) {
                    Text("Нет избранных товаров.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    favoriteProducts.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    formatPriceRub(product.price),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onAddFavoriteToCart(product) }) {
                                    Text("В корзину")
                                }
                                TextButton(onClick = { onToggleFavorite(product.id) }) {
                                    Text("Убрать")
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("История заказов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                // Фильтры по статусу заказов
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = ordersFilter == "ALL",
                        onClick = { ordersFilter = "ALL" },
                        label = { Text("Все") }
                    )
                    FilterChip(
                        selected = ordersFilter == "NEW",
                        onClick = { ordersFilter = "NEW" },
                        label = { Text("Новые") }
                    )
                    FilterChip(
                        selected = ordersFilter == "IN_PROGRESS",
                        onClick = { ordersFilter = "IN_PROGRESS" },
                        label = { Text("В обработке") }
                    )
                    FilterChip(
                        selected = ordersFilter == "DONE",
                        onClick = { ordersFilter = "DONE" },
                        label = { Text("Завершённые") }
                    )
                }

                val filteredOrders = uiState.orders.filter { order ->
                    when (ordersFilter) {
                        "NEW" -> order.status.uppercase() == "NEW"
                        "IN_PROGRESS" -> order.status.uppercase() == "IN_PROGRESS"
                        "DONE" -> order.status.uppercase() == "DONE"
                        else -> true
                    }
                }

                if (filteredOrders.isEmpty()) {
                    Text(
                        text = if (uiState.orders.isEmpty()) "Вы ещё не оформляли заказы." else "Нет заказов с выбранным статусом.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    filteredOrders.forEach { order ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val statusLabel = when (order.status.uppercase()) {
                                "NEW" -> "Новый"
                                "IN_PROGRESS" -> "В обработке"
                                "DONE" -> "Завершён"
                                else -> order.status
                            }
                            Text("Заказ ${order.id}", fontWeight = FontWeight.Medium)
                            Text("Статус: $statusLabel", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Товаров: ${order.items.sumOf { it.quantity }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text("Выйти из аккаунта")
        }
    }
}

@Composable
private fun CartRow(
    item: CartItem,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = "Цена: ${formatPriceRub(item.product.price)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Количество: ${item.quantity} • Сумма: ${formatPriceRub(item.product.price * item.quantity)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDecrement(item.product.id) }) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onIncrement(item.product.id) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
            TextButton(onClick = { onRemove(item.product.id) }) {
                Text("Удалить", style = MaterialTheme.typography.bodySmall)
            }
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

