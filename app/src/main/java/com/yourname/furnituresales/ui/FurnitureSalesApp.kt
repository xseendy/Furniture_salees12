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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

@Composable
fun FurnitureSalesApp(viewModel: FurnitureSalesViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val isAuthed = uiState.userProfile != null
    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        bottomBar = {
            if (isAuthed) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == AppTab.HOME,
                        onClick = { currentTab = AppTab.HOME },
                        label = { Text("Главная") },
                        icon = {}
                    )
                    NavigationBarItem(
                        selected = currentTab == AppTab.CART,
                        onClick = { currentTab = AppTab.CART },
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
                        onClick = { currentTab = AppTab.ACCOUNT },
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
                    onGuest = { viewModel.signInAnonymously() }
                )
            }
        } else {
            when (currentTab) {
                AppTab.HOME -> ProductListScreen(
                    modifier = baseModifier,
                    uiState = uiState,
                    onRefresh = { viewModel.loadProducts() },
                    onAddToCart = { viewModel.addToCart(it) },
                    onProductClick = { selectedProduct = it }
                )
                AppTab.CART -> CartScreen(
                    modifier = baseModifier,
                    uiState = uiState,
                    onIncrement = { viewModel.updateQuantity(it, 1) },
                    onDecrement = { viewModel.updateQuantity(it, -1) },
                    onClearCart = { viewModel.clearCart() },
                    onCheckout = { viewModel.checkout() },
                    onAddressChange = { viewModel.updateShippingAddress(it) },
                    onPhoneChange = { viewModel.updatePhone(it) },
                    onPaymentSelected = { viewModel.setPaymentMethod(it) }
                )
                AppTab.ACCOUNT -> AccountScreen(
                    modifier = baseModifier,
                    uiState = uiState,
                    onSignOut = { viewModel.signOut() },
                    onAddressChange = { viewModel.updateShippingAddress(it) },
                    onPhoneChange = { viewModel.updatePhone(it) }
                )
            }
        }

        selectedProduct?.let { product ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { selectedProduct = null },
                title = { Text(product.name, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(product.description)
                        val specs = listOf(
                            "Размеры: ${product.dimensions}".takeIf { product.dimensions.isNotBlank() },
                            "Материал: ${product.material}".takeIf { product.material.isNotBlank() },
                            "Цвет: ${product.color}".takeIf { product.color.isNotBlank() }
                        ).filterNotNull()
                        specs.forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("Цена: $${"%.2f".format(product.price)}", fontWeight = FontWeight.SemiBold)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.addToCart(product)
                        selectedProduct = null
                    }) { Text("В корзину") }
                },
                dismissButton = {
                    TextButton(onClick = { selectedProduct = null }) { Text("Закрыть") }
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
    onGuest: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val isFormValid = email.isNotBlank() && password.length >= 6 && email.contains("@")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Furniture Sales", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            text = if (isRegisterMode) "Создать аккаунт" else "Добро пожаловать",
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
                label = { Text("Пароль") },
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
                if (isRegisterMode) onRegister(email, password) else onSignIn(email, password)
            }
        ) {
            Text(if (isRegisterMode) "Зарегистрироваться" else "Войти")
        }
        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "Уже есть аккаунт? Войти" else "Впервые? Создать аккаунт")
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
    onProductClick: (Product) -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Привет, ${uiState.userProfile?.email ?: "гость"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Новинки, подборки и популярное",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HeroBanner()
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
            items(uiState.products) { product ->
                ProductCard(
                    product = product,
                    onAddToCart = { onAddToCart(product) },
                    onProductClick = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onAddToCart: () -> Unit, onProductClick: () -> Unit) {
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
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = listOf(product.dimensions, product.material, product.color).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$${"%.2f".format(product.price)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = product.material,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Button(onClick = onAddToCart, shape = RoundedCornerShape(10.dp)) {
                    Text("В корзину")
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
    onClearCart: () -> Unit,
    onCheckout: () -> Unit,
    onAddressChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPaymentSelected: (String) -> Unit
) {
    val total = uiState.cart.sumOf { it.product.price * it.quantity }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Корзина", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (uiState.checkoutMessage != null) {
            Text(
                text = uiState.checkoutMessage,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (uiState.cart.isEmpty()) {
            Text("Корзина пуста.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.cart.forEachIndexed { index, item ->
                        CartRow(item = item, onIncrement = onIncrement, onDecrement = onDecrement)
                        if (index < uiState.cart.lastIndex) Divider()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Итого", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("$${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onClearCart) { Text("Очистить корзину") }
                }
            }
        }

        Text("Доставка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.shippingAddress,
            onValueChange = onAddressChange,
            label = { Text("Адрес доставки") },
            placeholder = { Text("Улица, город, индекс") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.phone,
            onValueChange = onPhoneChange,
            label = { Text("Телефон для курьера") },
            placeholder = { Text("+7 900 123 4567") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Способ оплаты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        PaymentSelector(selected = uiState.paymentMethod, onSelect = onPaymentSelected)
        if (uiState.error != null) {
            Text(uiState.error, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.cart.isNotEmpty()
        ) { Text("Оформить заказ") }
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
            Text("Новинки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Подборка для гостиной, столовой и кабинета.",
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
    onPhoneChange: (String) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Профиль", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Вы вошли как ${uiState.userProfile?.email ?: "Гость"}", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("Адрес по умолчанию", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = uiState.shippingAddress,
            onValueChange = onAddressChange,
            label = { Text("Адрес") },
            placeholder = { Text("Улица, город, индекс") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.phone,
            onValueChange = onPhoneChange,
            label = { Text("Телефон") },
            placeholder = { Text("+7 900 123 4567") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors()
        ) { Text("Выйти") }
    }
}

@Composable
private fun CartRow(
    item: CartItem,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text("$${"%.2f".format(item.product.price)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDecrement(item.product.id) }) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onIncrement(item.product.id) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Preview
@Composable
private fun AuthPreview() {
    FurnitureSalesTheme {
        AuthScreen(isLoading = false, error = null, onSignIn = { _, _ -> }, onRegister = { _, _ -> }, onGuest = {})
    }
}

