package com.yourname.furnituresales.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourname.furnituresales.FurnitureUiState
import com.yourname.furnituresales.R
import com.yourname.furnituresales.data.Product
import com.yourname.furnituresales.ui.util.formatPriceRub

private enum class AccountTab { PROFILE, PASSWORD, ORDERS }

private fun normalizePhoneInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    val normalized = when {
        digits.startsWith("8") -> "7" + digits.drop(1)
        digits.startsWith("7") -> digits
        digits.startsWith("9") -> "7$digits"
        else -> digits
    }.take(11)
    if (normalized.isBlank()) return ""

    val rest = normalized.drop(1)
    val a = rest.take(3)
    val b = rest.drop(3).take(3)
    val c = rest.drop(6).take(2)
    val d = rest.drop(8).take(2)

    return buildString {
        append("+7")
        if (a.isNotBlank()) append(" ").append(a)
        if (b.isNotBlank()) append(" ").append(b)
        if (c.isNotBlank()) append("-").append(c)
        if (d.isNotBlank()) append("-").append(d)
    }
}

private fun parseAddressFields(address: String): List<String> {
    val parts = address.split(",").map { it.trim() }.filter { it.isNotBlank() }
    var city = ""
    var street = ""
    var house = ""
    var apartment = ""
    var postal = ""

    parts.forEach { part ->
        val low = part.lowercase()
        when {
            low.startsWith("г ") || low.startsWith("г.") || low.startsWith("город") -> city = part
            low.startsWith("ул ") || low.startsWith("ул.") || low.contains("улиц") -> street = part
            low.startsWith("д ") || low.startsWith("д.") || low.contains("дом") -> house = part
            low.startsWith("кв ") || low.startsWith("кв.") || low.contains("квар") -> apartment = part
            part.any { it.isDigit() } && part.length in 5..7 -> postal = part
            city.isBlank() -> city = part
            street.isBlank() -> street = part
            house.isBlank() -> house = part
        }
    }

    city = city.removePrefix("г.").removePrefix("г ").trim()
    street = street.removePrefix("ул.").removePrefix("ул ").trim()
    house = house.removePrefix("д.").removePrefix("д ").trim()
    apartment = apartment.removePrefix("кв.").removePrefix("кв ").trim()
    postal = postal.trim()

    return listOf(city, street, house, apartment, postal)
}

private fun composeAddress(city: String, street: String, house: String, apartment: String, postal: String): String {
    val parts = mutableListOf<String>()
    if (postal.isNotBlank()) parts.add(postal.trim())
    if (city.isNotBlank()) parts.add(city.trim())
    if (street.isNotBlank()) parts.add("ул. ${street.trim()}")
    if (house.isNotBlank()) parts.add("д. ${house.trim()}")
    if (apartment.isNotBlank()) parts.add("кв. ${apartment.trim()}")
    return parts.joinToString(", ")
}

@Composable
fun AccountScreen(
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
    var city by rememberSaveable { mutableStateOf("") }
    var street by rememberSaveable { mutableStateOf("") }
    var house by rememberSaveable { mutableStateOf("") }
    var apartment by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var attemptedSaveProfile by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.userProfile?.uid) {
        name = uiState.userProfile?.displayName.orEmpty()
        val (c, s, h, a, p) = parseAddressFields(uiState.shippingAddress)
        city = c
        street = s
        house = h
        apartment = a
        postalCode = p
        phone = normalizePhoneInput(uiState.phone)
    }

    val addressValue = composeAddress(city, street, house, apartment, postalCode)
    val canSaveProfile = name.trim().isNotBlank() && city.trim().isNotBlank() && street.trim().isNotBlank() && house.trim().isNotBlank() && phone.trim().isNotBlank()

    val nameError = attemptedSaveProfile && name.trim().isBlank()
    val cityError = attemptedSaveProfile && city.trim().isBlank()
    val streetError = attemptedSaveProfile && street.trim().isBlank()
    val houseError = attemptedSaveProfile && house.trim().isBlank()
    val phoneError = attemptedSaveProfile && phone.trim().isBlank()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                stringResource(
                    R.string.profile_signed_in_as,
                    uiState.userProfile?.email ?: stringResource(R.string.profile_guest)
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                AccountTab.PROFILE to stringResource(R.string.profile_tab_data),
                AccountTab.PASSWORD to stringResource(R.string.profile_tab_password),
                AccountTab.ORDERS to stringResource(R.string.profile_tab_orders)
            )
            val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
            TabRow(selectedTabIndex = selectedIndex, modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, (tab, title) ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        when (selectedTab) {
            AccountTab.PROFILE -> {
                Text(stringResource(R.string.profile_section_account_data), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = uiState.userProfile?.email.orEmpty(),
                    onValueChange = { },
                    label = { Text(stringResource(R.string.field_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = { Text(stringResource(R.string.field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError,
                    supportingText = {
                        if (nameError) {
                            Text("Имя обязательно")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Адрес доставки",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = {
                                city = it
                            },
                            label = { Text(stringResource(R.string.field_city)) },
                            placeholder = { Text(stringResource(R.string.field_city_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = cityError,
                            supportingText = {
                                if (cityError) {
                                    Text("Город обязателен")
                                }
                            }
                        )
                        OutlinedTextField(
                            value = street,
                            onValueChange = {
                                street = it
                            },
                            label = { Text(stringResource(R.string.field_street)) },
                            placeholder = { Text(stringResource(R.string.field_street_placeholder)) },
                            modifier = Modifier.fillMaxWidth(),
                            isError = streetError,
                            supportingText = {
                                if (streetError) {
                                    Text("Улица обязательна")
                                }
                            }
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = house,
                                onValueChange = {
                                    house = it
                                },
                                label = { Text(stringResource(R.string.field_house)) },
                                placeholder = { Text(stringResource(R.string.field_house_placeholder)) },
                                modifier = Modifier.weight(1f),
                                isError = houseError,
                                supportingText = {
                                    if (houseError) {
                                        Text("Дом обязателен")
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = apartment,
                                onValueChange = {
                                    apartment = it
                                },
                                label = { Text(stringResource(R.string.field_apartment)) },
                                placeholder = { Text(stringResource(R.string.field_apartment_placeholder)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = {
                                postalCode = it
                            },
                            label = { Text(stringResource(R.string.field_postal_code)) },
                            placeholder = { Text(stringResource(R.string.field_postal_code_placeholder)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        val formatted = normalizePhoneInput(it)
                        phone = formatted
                    },
                    label = { Text(stringResource(R.string.field_phone)) },
                    placeholder = { Text(stringResource(R.string.field_phone_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = phoneError,
                    supportingText = {
                        if (phoneError) {
                            Text("Телефон обязателен")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        attemptedSaveProfile = true
                        if (canSaveProfile) {
                            onSaveProfile(name, addressValue, phone)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                    ,
                    enabled = canSaveProfile
                ) {
                    Text(stringResource(R.string.action_save_profile))
                }
                if (uiState.error != null) {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                    label = { Text(stringResource(R.string.field_current_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.field_new_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(
                    onClick = { onChangePassword(currentPassword, newPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_change_password))
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
                Text(stringResource(R.string.profile_favorites_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val favoriteProducts = uiState.products.filter { uiState.favorites.contains(it.id) }
                if (favoriteProducts.isEmpty()) {
                    Text(stringResource(R.string.profile_no_favorites), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    favoriteProducts.forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
                                    Text(
                                        stringResource(R.string.action_add_to_cart),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(onClick = { onToggleFavorite(product.id) }) {
                                    Text(stringResource(R.string.action_remove_short))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.profile_orders_history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = ordersFilter == "ALL",
                        onClick = { ordersFilter = "ALL" },
                        label = { Text(stringResource(R.string.orders_filter_all)) }
                    )
                    FilterChip(
                        selected = ordersFilter == "NEW",
                        onClick = { ordersFilter = "NEW" },
                        label = { Text(stringResource(R.string.orders_filter_new)) }
                    )
                    FilterChip(
                        selected = ordersFilter == "IN_PROGRESS",
                        onClick = { ordersFilter = "IN_PROGRESS" },
                        label = { Text(stringResource(R.string.orders_filter_in_progress)) }
                    )
                    FilterChip(
                        selected = ordersFilter == "DONE",
                        onClick = { ordersFilter = "DONE" },
                        label = { Text(stringResource(R.string.orders_filter_done)) }
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
                        text = if (uiState.orders.isEmpty()) stringResource(R.string.orders_empty) else stringResource(R.string.orders_empty_with_filter),
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
                                "NEW" -> stringResource(R.string.order_status_new)
                                "IN_PROGRESS" -> stringResource(R.string.order_status_in_progress)
                                "DONE" -> stringResource(R.string.order_status_done)
                                else -> order.status
                            }
                            Text(stringResource(R.string.order_title, order.id), fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.order_status_label, statusLabel), style = MaterialTheme.typography.bodySmall)
                            Text(
                                stringResource(R.string.order_items_count, order.items.sumOf { it.quantity }),
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
            Text(stringResource(R.string.action_sign_out))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
