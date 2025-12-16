package com.yourname.furnituresales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.furnituresales.data.AppDatabase
import com.yourname.furnituresales.data.CartItemEntity
import com.yourname.furnituresales.data.CartItem
import com.yourname.furnituresales.data.CustomerEntity
import com.yourname.furnituresales.data.FavoriteEntity
import com.yourname.furnituresales.data.Product
import com.yourname.furnituresales.data.ProductEntity
import com.yourname.furnituresales.data.UserProfile
import com.yourname.furnituresales.data.Address
import com.yourname.furnituresales.data.Order
import com.yourname.furnituresales.data.OrderItem
import com.yourname.furnituresales.data.OrderEntity
import com.yourname.furnituresales.data.OrderItemEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class FurnitureUiState(
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val cart: List<CartItem> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val addresses: List<Address> = emptyList(),
    val orders: List<Order> = emptyList(),
    val shippingAddress: String = "",
    val phone: String = "",
    val paymentMethod: String = "",
    val customers: List<UserProfile> = emptyList(),
    val checkoutMessage: String? = null,
    val error: String? = null,
    val profileMessage: String? = null,
    val deliveryMethod: String = "courier",
    val promoCode: String = "",
    val promoDiscount: Double = 0.0 // 0.0–1.0, e.g. 0.1 = 10%
)

@HiltViewModel
class FurnitureSalesViewModel @Inject constructor(
    application: Application,
    private val db: AppDatabase
) : AndroidViewModel(application) {
    // Simple in-memory auth; credentials persisted via Room database.
    private val userStore: MutableMap<String, String> = mutableMapOf()
    private val customerProfiles: MutableMap<String, UserProfile> = mutableMapOf()
    private val demoProducts = listOf(
        Product(
            id = "oak-table",
            name = "Обеденный стол из дуба",
            description = "Стол из массива дуба на шесть персон.",
            price = 899.0,
            imageResName = "oak_table",
            dimensions = "180 x 90 x 75 cm",
            material = "Массив дуба",
            color = "Натуральный"
        ),
        Product(
            id = "leather-sofa",
            name = "Кожаный диван",
            description = "Диван на 3 места из натуральной кожи.",
            price = 1299.0,
            imageResName = "leather_sofa",
            dimensions = "220 x 95 x 85 cm",
            material = "Натуральная кожа, деревянный каркас",
            color = "Коньяк"
        ),
        Product(
            id = "walnut-bookshelf",
            name = "Стеллаж из ореха",
            description = "Пятиполочный стеллаж из орехового шпона.",
            price = 299.0,
            imageResName = "walnut_bookshelf",
            dimensions = "80 x 30 x 180 cm",
            material = "Шпон ореха, стальной каркас",
            color = "Орех"
        ),
        Product(
            id = "accent-chair",
            name = "Кресло акцентное",
            description = "Мягкое кресло с каркасом из сухой древесины.",
            price = 459.0,
            imageResName = "accent_chair",
            dimensions = "78 x 80 x 90 cm",
            material = "Ткань букле, ножки из дуба",
            color = "Айвори"
        ),
        Product(
            id = "coffee-table",
            name = "Стеклянный журнальный стол",
            description = "Закалённое стекло и основание из шлифованной стали.",
            price = 389.0,
            imageResName = "coffee_table",
            dimensions = "110 x 60 x 40 cm",
            material = "Закалённое стекло, сталь",
            color = "Прозрачный / Сталь"
        ),
        Product(
            id = "platform-bed",
            name = "Платформенная кровать",
            description = "Кровать Queen с интегрированным изголовьем.",
            price = 999.0,
            imageResName = "platform_bed",
            dimensions = "160 x 200 cm (Queen)",
            material = "Массив ясеня, льняная обивка",
            color = "Светлый ясень"
        ),
        Product(
            id = "floor-lamp",
            name = "Торшер с дугой",
            description = "Регулируемый торшер с текстильным абажуром.",
            price = 199.0,
            imageResName = "floor_lamp",
            dimensions = "210 cm height",
            material = "Сталь, текстиль",
            color = "Матовый чёрный"
        ),
        Product(
            id = "area-rug",
            name = "Ковёр из шерсти",
            description = "Ручная тафтинговая шерсть с мягким рисунком.",
            price = 349.0,
            imageResName = "area_rug",
            dimensions = "240 x 170 cm",
            material = "100% шерсть",
            color = "Песочный"
        ),
        Product(
            id = "standing-desk",
            name = "Стол-трансформер",
            description = "Регулируемый стол с системой кабель-менеджмента.",
            price = 749.0,
            imageResName = "standing_desk",
            dimensions = "140 x 70 cm",
            material = "Бамбук, сталь",
            color = "Натуральный / Чёрный"
        ),
        Product(
            id = "sideboard",
            name = "Комод-буфет",
            description = "Буфет на 4 двери с плавным закрыванием.",
            price = 899.0,
            imageResName = "sideboard",
            dimensions = "180 x 45 x 80 cm",
            material = "Шпон дуба, металлическое основание",
            color = "Копчёный дуб"
        )
    )

    private val usdToRubRate = 90.0

    private fun formatRub(amountUsd: Double): String =
        String.format(Locale("ru", "RU"), "%,.0f ₽", amountUsd * usdToRubRate)

    private val _uiState = MutableStateFlow(FurnitureUiState())
    val uiState: StateFlow<FurnitureUiState> = _uiState.asStateFlow()

    init {
        observeCustomers()
        loadProducts()
        _uiState.value = _uiState.value.copy(
            addresses = emptyList(),
            shippingAddress = "",
            phone = "",
            paymentMethod = getApplication<Application>().getString(R.string.payment_card)
        )
    }

    private fun currentUid(): String? = _uiState.value.userProfile?.uid

    private fun persistCart(uid: String) {
        val cartSnapshot = _uiState.value.cart
        viewModelScope.launch {
            try {
                db.cartDao().clear(uid)
                val entities = cartSnapshot.map { item ->
                    CartItemEntity(uid = uid, productId = item.product.id, quantity = item.quantity)
                }
                if (entities.isNotEmpty()) {
                    db.cartDao().upsertAll(entities)
                }
            } catch (_: Exception) {
                // ignore persistence errors
            }
        }
    }

    private fun persistFavorites(uid: String) {
        val favoriteSnapshot = _uiState.value.favorites
        viewModelScope.launch {
            try {
                db.favoriteDao().clear(uid)
                favoriteSnapshot.forEach { productId ->
                    db.favoriteDao().add(FavoriteEntity(uid = uid, productId = productId))
                }
            } catch (_: Exception) {
                // ignore persistence errors
            }
        }
    }

    private fun restoreCartAndFavorites(uid: String) {
        viewModelScope.launch {
            try {
                val favorites = db.favoriteDao().getFavoriteIds(uid).toSet()
                val cartEntities = db.cartDao().getCartItems(uid)
                val cart = cartEntities.mapNotNull { entity ->
                    val productEntity = db.productDao().findById(entity.productId) ?: return@mapNotNull null
                    val product = Product(
                        id = productEntity.productId,
                        name = productEntity.name,
                        description = productEntity.description,
                        price = productEntity.price,
                        imageUrl = productEntity.imageUrl,
                        imageResName = productEntity.imageResName,
                        dimensions = productEntity.dimensions,
                        material = productEntity.material,
                        color = productEntity.color
                    )
                    CartItem(product = product, quantity = entity.quantity)
                }
                _uiState.value = _uiState.value.copy(
                    cart = cart,
                    favorites = favorites
                )
            } catch (_: Exception) {
                // ignore persistence errors
            }
        }
    }

    private fun restoreOrders(uid: String) {
        viewModelScope.launch {
            try {
                val orders = db.orderDao().getOrders(uid).map { orderEntity ->
                    val items = db.orderDao().getOrderItems(uid, orderEntity.orderId).map { item ->
                        OrderItem(
                            productId = item.productId,
                            name = item.name,
                            price = item.price,
                            quantity = item.quantity
                        )
                    }
                    Order(
                        id = orderEntity.orderId,
                        items = items,
                        total = orderEntity.total,
                        address = Address(
                            id = "selected",
                            label = getApplication<Application>().getString(R.string.delivery_title),
                            line = orderEntity.addressLine,
                            phone = orderEntity.addressPhone
                        ),
                        status = orderEntity.status
                    )
                }
                _uiState.value = _uiState.value.copy(orders = orders)
            } catch (_: Exception) {
                // ignore persistence errors
            }
        }
    }

    fun signOut() {
        _uiState.value = FurnitureUiState()
        loadProducts()
    }

    fun signIn(email: String, password: String) {
        updateLoading(true)
        viewModelScope.launch {
            try {
                if (!validateCredentials(email, password)) return@launch
                val stored = userStore[email.trim()]
                if (stored == null || stored != password) {
                    updateError(getApplication<Application>().getString(R.string.err_invalid_email_or_password))
                    return@launch
                }
                val role = if (email.trim() == "admin@furniture.test") "admin" else "customer"
                val profile = customerProfiles[email.trim()] ?: UserProfile(uid = email.trim(), email = email.trim(), role = role)
                persistCustomer(profile, _uiState.value.shippingAddress, _uiState.value.phone, _uiState.value.paymentMethod, _uiState.value.deliveryMethod)
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    isLoading = false,
                    error = null,
                    checkoutMessage = null,
                    addresses = emptyList()
                )
                loadCustomerDetails(profile)
                loadProducts()
                restoreCartAndFavorites(profile.uid)
                restoreOrders(profile.uid)
            } catch (e: Exception) {
                updateError(getApplication<Application>().getString(R.string.err_sign_in_failed, e.message))
            }
        }
    }

    fun signUp(email: String, password: String) {
        updateLoading(true)
        viewModelScope.launch {
            try {
                if (!validateCredentials(email, password)) return@launch
                val trimmedEmail = email.trim()
                if (userStore.containsKey(trimmedEmail)) {
                    updateError(getApplication<Application>().getString(R.string.err_email_already_registered))
                    return@launch
                }
                userStore[trimmedEmail] = password
                val role = "customer"
                val profile = UserProfile(uid = trimmedEmail, email = trimmedEmail, role = role)
                persistCustomer(profile, _uiState.value.shippingAddress, _uiState.value.phone, _uiState.value.paymentMethod, _uiState.value.deliveryMethod)
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    isLoading = false,
                    error = null,
                    checkoutMessage = null,
                    addresses = emptyList()
                )
                loadCustomerDetails(profile)
                loadProducts()
                restoreCartAndFavorites(profile.uid)
                restoreOrders(profile.uid)
            } catch (e: Exception) {
                updateError(getApplication<Application>().getString(R.string.err_sign_up_failed, e.message))
            }
        }
    }

    fun signInAnonymously() {
        updateLoading(true)
        viewModelScope.launch {
            try {
                delay(100)
                _uiState.value = _uiState.value.copy(
                    userProfile = UserProfile(uid = "guest", email = "guest@offline"),
                    addresses = emptyList(),
                    shippingAddress = "",
                    phone = "",
                    isLoading = false,
                    error = null,
                    checkoutMessage = null
                )
                loadProducts()
                restoreCartAndFavorites("guest")
                restoreOrders("guest")
            } catch (e: Exception) {
                updateError(getApplication<Application>().getString(R.string.err_guest_sign_in_failed, e.message))
            }
        }
    }

    fun loadProducts() {
        updateLoading(true)
        viewModelScope.launch {
            try {
                // Seed DB with demo products (first run). Safe with REPLACE.
                db.productDao().upsertAll(
                    demoProducts.map { p ->
                        ProductEntity(
                            productId = p.id,
                            name = p.name,
                            description = p.description,
                            price = p.price,
                            imageUrl = p.imageUrl,
                            imageResName = p.imageResName,
                            dimensions = p.dimensions,
                            material = p.material,
                            color = p.color
                        )
                    }
                )
                val products = db.productDao().getAll().map { e ->
                    Product(
                        id = e.productId,
                        name = e.name,
                        description = e.description,
                        price = e.price,
                        imageUrl = e.imageUrl,
                        imageResName = e.imageResName,
                        dimensions = e.dimensions,
                        material = e.material,
                        color = e.color
                    )
                }
                _uiState.value = _uiState.value.copy(products = products, isLoading = false, error = null)
            } catch (e: Exception) {
                updateError(getApplication<Application>().getString(R.string.err_load_products_failed, e.message))
            }
        }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        val qty = quantity.coerceAtLeast(1)
        val current = _uiState.value.cart.toMutableList()
        val idx = current.indexOfFirst { it.product.id == product.id }
        if (idx >= 0) {
            val existing = current[idx]
            current[idx] = existing.copy(quantity = existing.quantity + qty)
        } else {
            current.add(CartItem(product, qty))
        }
        _uiState.value = _uiState.value.copy(cart = current, checkoutMessage = null)

        currentUid()?.let { persistCart(it) }
    }

    fun updateQuantity(productId: String, delta: Int) {
        val current = _uiState.value.cart.toMutableList()
        val idx = current.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            val item = current[idx]
            val newQty = item.quantity + delta
            if (newQty <= 0) current.removeAt(idx) else current[idx] = item.copy(quantity = newQty)
            _uiState.value = _uiState.value.copy(cart = current, checkoutMessage = null)

            currentUid()?.let { persistCart(it) }
        }
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(
            cart = emptyList(),
            checkoutMessage = null,
            promoCode = "",
            promoDiscount = 0.0,
            error = null
        )

        currentUid()?.let { persistCart(it) }
    }

    fun removeFromCart(productId: String) {
        val current = _uiState.value.cart.toMutableList()
        val idx = current.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            current.removeAt(idx)
            _uiState.value = _uiState.value.copy(cart = current, checkoutMessage = null)

            currentUid()?.let { persistCart(it) }
        }
    }

    fun checkout() {
        val total = _uiState.value.cart.sumOf { it.product.price * it.quantity }
        if (total <= 0) {
            updateError(getApplication<Application>().getString(R.string.err_cart_empty))
            return
        }
        if (_uiState.value.shippingAddress.isBlank()) {
            updateError(getApplication<Application>().getString(R.string.err_add_delivery_address))
            return
        }
        val discountFactor = 1.0 - _uiState.value.promoDiscount.coerceIn(0.0, 1.0)
        val totalWithDiscount = total * discountFactor
        val orderId = "order-${System.currentTimeMillis()}"
        val order = Order(
            id = orderId,
            items = _uiState.value.cart.map {
                OrderItem(
                    productId = it.product.id,
                    name = it.product.name,
                    price = it.product.price,
                    quantity = it.quantity
                )
            },
            total = total,
            address = Address(
                id = "selected",
                label = getApplication<Application>().getString(R.string.delivery_title),
                line = _uiState.value.shippingAddress,
                phone = _uiState.value.phone.ifBlank { getApplication<Application>().getString(R.string.msg_no_phone) }
            ),
            status = "NEW"
        )
        val app = getApplication<Application>()
        val checkoutTitle = app.getString(R.string.msg_order_created, formatRub(totalWithDiscount))
        val discountSuffix = if (_uiState.value.promoDiscount > 0.0) app.getString(R.string.msg_order_discount_suffix) else ""
        _uiState.value = _uiState.value.copy(
            cart = emptyList(),
            checkoutMessage = buildString {
                append(checkoutTitle)
                append(discountSuffix)
                append(" • ${_uiState.value.paymentMethod}")
                append(" • ${_uiState.value.shippingAddress}")
                append(" • ${_uiState.value.phone.ifBlank { app.getString(R.string.msg_no_phone) }}")
            },
            error = null,
            orders = _uiState.value.orders + order
        )

        currentUid()?.let { persistCart(it) }

        val uid = currentUid() ?: return
        viewModelScope.launch {
            try {
                db.orderDao().upsertOrderWithItems(
                    order = OrderEntity(
                        uid = uid,
                        orderId = orderId,
                        createdAt = System.currentTimeMillis(),
                        total = order.total,
                        status = order.status,
                        addressLine = order.address.line,
                        addressPhone = order.address.phone,
                        paymentMethod = _uiState.value.paymentMethod
                    ),
                    items = order.items.map { item ->
                        OrderItemEntity(
                            uid = uid,
                            orderId = orderId,
                            productId = item.productId,
                            name = item.name,
                            price = item.price,
                            quantity = item.quantity
                        )
                    }
                )
            } catch (_: Exception) {
                // ignore persistence errors
            }
        }
    }

    fun updateShippingAddress(address: String) {
        _uiState.value = _uiState.value.copy(shippingAddress = address, error = null, checkoutMessage = null)
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone, error = null, checkoutMessage = null)
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method, error = null, checkoutMessage = null)
        val profile = _uiState.value.userProfile ?: return
        if (profile.uid == "guest") return
        persistCustomer(profile, _uiState.value.shippingAddress, _uiState.value.phone, method, _uiState.value.deliveryMethod)
    }

    fun setDeliveryMethod(method: String) {
        _uiState.value = _uiState.value.copy(deliveryMethod = method, error = null, checkoutMessage = null)
        val profile = _uiState.value.userProfile ?: return
        if (profile.uid == "guest") return
        persistCustomer(profile, _uiState.value.shippingAddress, _uiState.value.phone, _uiState.value.paymentMethod, method)
    }

    fun updateDisplayName(name: String) {
        val currentProfile = _uiState.value.userProfile ?: return
        val updatedProfile = currentProfile.copy(displayName = name.ifBlank { null })
        _uiState.value = _uiState.value.copy(userProfile = updatedProfile, error = null)
    }

    fun saveProfile(name: String, address: String, phone: String) {
        val currentProfile = _uiState.value.userProfile ?: return

        val app = getApplication<Application>()
        val trimmedName = name.trim()
        val trimmedAddress = address.trim()
        val trimmedPhone = phone.trim()
        val phoneDigits = trimmedPhone.filter { it.isDigit() }
        when {
            trimmedName.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = app.getString(R.string.err_profile_name_required))
                return
            }

            trimmedAddress.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = app.getString(R.string.err_profile_address_required))
                return
            }

            trimmedPhone.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = app.getString(R.string.err_profile_phone_required))
                return
            }

            phoneDigits.length < 11 -> {
                _uiState.value = _uiState.value.copy(error = app.getString(R.string.err_profile_phone_invalid))
                return
            }
        }

        val updatedProfile = currentProfile.copy(displayName = name.ifBlank { null })
        _uiState.value = _uiState.value.copy(
            userProfile = updatedProfile,
            shippingAddress = trimmedAddress,
            phone = trimmedPhone,
            error = null,
            profileMessage = getApplication<Application>().getString(R.string.msg_profile_saved)
        )
        if (updatedProfile.uid != "guest") {
            persistCustomer(updatedProfile, trimmedAddress, trimmedPhone, _uiState.value.paymentMethod, _uiState.value.deliveryMethod)
        }
    }

    fun applyPromoCode(code: String) {
        val normalized = code.trim().uppercase()
        val discount = when (normalized) {
            "SALE10" -> 0.10
            "SALE20" -> 0.20
            else -> 0.0
        }
        _uiState.value = if (discount > 0.0) {
            _uiState.value.copy(
                promoCode = normalized,
                promoDiscount = discount,
                error = null,
                checkoutMessage = null
            )
        } else {
            _uiState.value.copy(
                promoCode = normalized,
                promoDiscount = 0.0,
                error = getApplication<Application>().getString(R.string.err_promo_invalid)
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val profile = _uiState.value.userProfile
        if (profile == null) {
            updateError(getApplication<Application>().getString(R.string.err_sign_in_first))
            return
        }
        viewModelScope.launch {
            val email = profile.email?.trim().orEmpty()
            val stored = userStore[email]
            when {
                stored == null -> {
                    updateError(getApplication<Application>().getString(R.string.err_account_not_found))
                }

                stored != currentPassword -> {
                    updateError(getApplication<Application>().getString(R.string.err_current_password_wrong))
                }

                newPassword.length < 6 -> {
                    updateError(getApplication<Application>().getString(R.string.err_new_password_short))
                }

                else -> {
                    userStore[email] = newPassword
                    // persist updated password into DB
                    val existing = db.customerDao().findByEmail(email)
                    if (existing != null) {
                        db.customerDao().upsert(
                            existing.copy(password = newPassword)
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        error = null,
                        profileMessage = getApplication<Application>().getString(R.string.msg_password_changed)
                    )
                }
            }
        }
    }

    fun resetPassword(email: String, newPassword: String) {
        updateLoading(true)
        viewModelScope.launch {
            try {
                val trimmedEmail = email.trim()
                if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
                    updateError(getApplication<Application>().getString(R.string.err_enter_valid_email))
                    return@launch
                }
                if (newPassword.length < 6) {
                    updateError(getApplication<Application>().getString(R.string.err_new_password_short))
                    return@launch
                }
                val customer = db.customerDao().findByEmail(trimmedEmail)
                if (customer == null) {
                    updateError(getApplication<Application>().getString(R.string.err_email_not_found))
                    return@launch
                }
                userStore[trimmedEmail] = newPassword
                db.customerDao().upsert(customer.copy(password = newPassword))
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    profileMessage = getApplication<Application>().getString(R.string.msg_password_updated)
                )
            } catch (e: Exception) {
                updateError(getApplication<Application>().getString(R.string.err_reset_password_failed, e.message))
            }
        }
    }

    fun toggleFavorite(productId: String) {
        val favs = _uiState.value.favorites.toMutableSet()
        if (favs.contains(productId)) favs.remove(productId) else favs.add(productId)
        _uiState.value = _uiState.value.copy(favorites = favs)

        currentUid()?.let { persistFavorites(it) }
    }

    fun selectAddress(addressId: String) {
        val address = _uiState.value.addresses.find { it.id == addressId } ?: return
        _uiState.value = _uiState.value.copy(
            shippingAddress = address.line,
            phone = address.phone,
            error = null,
            checkoutMessage = null
        )
    }

    private fun updateLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading, error = null)
    }

    private fun updateError(message: String?) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        val trimmedEmail = email.trim()
        return when {
            trimmedEmail.isEmpty() -> {
                updateError(getApplication<Application>().getString(R.string.err_email_required))
                false
            }

            !trimmedEmail.contains("@") -> {
                updateError(getApplication<Application>().getString(R.string.err_enter_valid_email))
                false
            }

            password.length < 6 -> {
                updateError(getApplication<Application>().getString(R.string.err_password_short))
                false
            }

            else -> true
        }
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            db.customerDao().getCustomers().collect { customers ->
                customers.forEach { customer ->
                    val email = customer.email
                    val password = customer.password
                    if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                        userStore[email] = password
                    }
                }
                var newState = _uiState.value.copy(customers = customers.map { it.toProfile() })
                val currentProfile = newState.userProfile
                if (currentProfile != null) {
                    if (currentProfile.uid == "guest") {
                        _uiState.value = newState
                        return@collect
                    }
                    val matched = customers.find { entity ->
                        entity.uid == currentProfile.uid ||
                                (!entity.email.isNullOrBlank() && entity.email == currentProfile.email)
                    }
                    if (matched != null) {
                        newState = newState.copy(
                            shippingAddress = matched.address ?: newState.shippingAddress,
                            phone = matched.phone ?: newState.phone
                        )
                    }
                }
                _uiState.value = newState
            }
        }
    }

    private fun persistCustomer(profile: UserProfile, address: String, phone: String, paymentMethod: String, deliveryMethod: String) {
        viewModelScope.launch {
            db.customerDao().upsert(
                CustomerEntity(
                    uid = profile.uid,
                    email = profile.email,
                    displayName = profile.displayName,
                    role = profile.role,
                    address = address.ifBlank { null },
                    phone = phone.ifBlank { null },
                    paymentMethod = paymentMethod.ifBlank { null },
                    deliveryMethod = deliveryMethod.ifBlank { null },
                    password = profile.email?.let { userStore[it.trim()] }
                )
            )
        }
    }

    private fun loadCustomerDetails(profile: UserProfile) {
        if (profile.uid == "guest") return
        val email = profile.email ?: return
        viewModelScope.launch {
            val entity = db.customerDao().findByEmail(email.trim())
            if (entity != null) {
                _uiState.value = _uiState.value.copy(
                    shippingAddress = entity.address ?: "",
                    phone = entity.phone ?: "",
                    paymentMethod = entity.paymentMethod ?: _uiState.value.paymentMethod,
                    deliveryMethod = entity.deliveryMethod ?: _uiState.value.deliveryMethod,
                    addresses = emptyList()
                )
            }
        }
    }

    private fun CustomerEntity.toProfile(): UserProfile =
        UserProfile(uid = uid, email = email, displayName = displayName, role = role)
}

