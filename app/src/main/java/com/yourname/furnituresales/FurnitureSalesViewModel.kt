package com.yourname.furnituresales

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.yourname.furnituresales.data.AppDatabase
import com.yourname.furnituresales.data.CartItem
import com.yourname.furnituresales.data.CustomerEntity
import com.yourname.furnituresales.data.Product
import com.yourname.furnituresales.data.UserProfile
import com.yourname.furnituresales.data.Address
import com.yourname.furnituresales.data.Order
import com.yourname.furnituresales.data.OrderItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

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
    val paymentMethod: String = "Card",
    val customers: List<UserProfile> = emptyList(),
    val checkoutMessage: String? = null,
    val error: String? = null,
    val profileMessage: String? = null,
    val deliveryMethod: String = "courier",
    val promoCode: String = "",
    val promoDiscount: Double = 0.0 // 0.0–1.0, e.g. 0.1 = 10%
)

class FurnitureSalesViewModel(application: Application) : AndroidViewModel(application) {
    // Simple in-memory auth; credentials persisted via Room database.
    private val userStore: MutableMap<String, String> = mutableMapOf()
    private val customerProfiles: MutableMap<String, UserProfile> = mutableMapOf()
    private val db: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "furniture_local.db"
    ).fallbackToDestructiveMigration().build()
    private val sampleAddresses = listOf(
        Address(id = "home", label = "Дом", line = "ул. Пушкина, д. 10", phone = "+7 900 123 4567"),
        Address(id = "work", label = "Работа", line = "пр. Ленина, д. 25", phone = "+7 900 765 4321")
    )
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
            addresses = sampleAddresses,
            shippingAddress = "",
            phone = ""
        )
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
                    updateError("Неверный email или пароль.")
                    return@launch
                }
                val role = if (email.trim() == "admin@furniture.test") "admin" else "customer"
                val profile = customerProfiles[email.trim()] ?: UserProfile(uid = email.trim(), email = email.trim(), role = role)
                persistCustomer(profile, _uiState.value.shippingAddress, _uiState.value.phone)
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    isLoading = false,
                    error = null,
                    checkoutMessage = null
                )
                loadCustomerDetails(profile)
                loadProducts()
            } catch (e: Exception) {
                updateError("Sign-in failed: ${e.message}")
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
                    updateError("Этот email уже зарегистрирован.")
                    return@launch
                }
                userStore[trimmedEmail] = password
                val role = "customer"
                val profile = UserProfile(uid = trimmedEmail, email = trimmedEmail, role = role)
                persistCustomer(profile, _uiState.value.shippingAddress, _uiState.value.phone)
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    isLoading = false,
                    error = null,
                    checkoutMessage = null
                )
                loadCustomerDetails(profile)
                loadProducts()
            } catch (e: Exception) {
                updateError("Не удалось создать аккаунт: ${e.message}")
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
                    isLoading = false,
                    error = null,
                    checkoutMessage = null
                )
                loadProducts()
            } catch (e: Exception) {
                updateError("Не удалось войти как гость: ${e.message}")
            }
        }
    }

    fun loadProducts() {
        updateLoading(true)
        viewModelScope.launch {
            try {
                delay(150)
                _uiState.value = _uiState.value.copy(
                    products = demoProducts,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                updateError("Не удалось загрузить товары: ${e.message}")
            }
        }
    }

    fun addToCart(product: Product) {
        val current = _uiState.value.cart.toMutableList()
        val idx = current.indexOfFirst { it.product.id == product.id }
        if (idx >= 0) {
            val existing = current[idx]
            current[idx] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current.add(CartItem(product, 1))
        }
        _uiState.value = _uiState.value.copy(cart = current, checkoutMessage = null)
    }

    fun updateQuantity(productId: String, delta: Int) {
        val current = _uiState.value.cart.toMutableList()
        val idx = current.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            val item = current[idx]
            val newQty = item.quantity + delta
            if (newQty <= 0) current.removeAt(idx) else current[idx] = item.copy(quantity = newQty)
            _uiState.value = _uiState.value.copy(cart = current, checkoutMessage = null)
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
    }

    fun removeFromCart(productId: String) {
        val current = _uiState.value.cart.toMutableList()
        val idx = current.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            current.removeAt(idx)
            _uiState.value = _uiState.value.copy(cart = current, checkoutMessage = null)
        }
    }

    fun checkout() {
        val total = _uiState.value.cart.sumOf { it.product.price * it.quantity }
        if (total <= 0) {
            updateError("Корзина пуста.")
            return
        }
        if (_uiState.value.shippingAddress.isBlank()) {
            updateError("Добавьте адрес доставки, чтобы оформить заказ.")
            return
        }
        val discountFactor = 1.0 - _uiState.value.promoDiscount.coerceIn(0.0, 1.0)
        val totalWithDiscount = total * discountFactor
        val order = Order(
            id = "order-${System.currentTimeMillis()}",
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
                label = "Доставка",
                line = _uiState.value.shippingAddress,
                phone = _uiState.value.phone.ifBlank { "Без телефона" }
            ),
            status = "NEW"
        )
        _uiState.value = _uiState.value.copy(
            cart = emptyList(),
            checkoutMessage = buildString {
                append("Заказ оформлен! Итог: ${formatRub(totalWithDiscount)}")
                if (_uiState.value.promoDiscount > 0.0) {
                    append(" (скидка по промокоду)")
                }
                append(" • ${_uiState.value.paymentMethod}")
                append(" • ${_uiState.value.shippingAddress}")
                append(" • ${_uiState.value.phone.ifBlank { "Без телефона" }}")
            },
            error = null,
            orders = _uiState.value.orders + order
        )
    }

    fun updateShippingAddress(address: String) {
        _uiState.value = _uiState.value.copy(shippingAddress = address, error = null, checkoutMessage = null)
        _uiState.value.userProfile?.let { persistCustomer(it, address, _uiState.value.phone) }
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(phone = phone, error = null, checkoutMessage = null)
        _uiState.value.userProfile?.let { persistCustomer(it, _uiState.value.shippingAddress, phone) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method, error = null, checkoutMessage = null)
    }

    fun setDeliveryMethod(method: String) {
        _uiState.value = _uiState.value.copy(deliveryMethod = method, error = null, checkoutMessage = null)
    }

    fun updateDisplayName(name: String) {
        val currentProfile = _uiState.value.userProfile ?: return
        val updatedProfile = currentProfile.copy(displayName = name.ifBlank { null })
        _uiState.value = _uiState.value.copy(userProfile = updatedProfile, error = null)
        persistCustomer(updatedProfile, _uiState.value.shippingAddress, _uiState.value.phone)
    }

    fun saveProfile(name: String, address: String, phone: String) {
        val currentProfile = _uiState.value.userProfile ?: return
        val updatedProfile = currentProfile.copy(displayName = name.ifBlank { null })
        _uiState.value = _uiState.value.copy(
            userProfile = updatedProfile,
            shippingAddress = address,
            phone = phone,
            error = null,
            profileMessage = "Данные профиля сохранены."
        )
        persistCustomer(updatedProfile, address, phone)
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
                error = "Промокод не найден или недействителен."
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val profile = _uiState.value.userProfile
        if (profile == null) {
            updateError("Сначала войдите в аккаунт.")
            return
        }
        viewModelScope.launch {
            val email = profile.email?.trim().orEmpty()
            val stored = userStore[email]
            when {
                stored == null -> {
                    updateError("Учетная запись не найдена.")
                }

                stored != currentPassword -> {
                    updateError("Текущий пароль указан неверно.")
                }

                newPassword.length < 6 -> {
                    updateError("Новый пароль должен быть не менее 6 символов.")
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
                        profileMessage = "Пароль успешно изменён."
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
                    updateError("Введите корректный email.")
                    return@launch
                }
                if (newPassword.length < 6) {
                    updateError("Новый пароль должен быть не менее 6 символов.")
                    return@launch
                }
                val customer = db.customerDao().findByEmail(trimmedEmail)
                if (customer == null) {
                    updateError("Аккаунт с таким email не найден.")
                    return@launch
                }
                userStore[trimmedEmail] = newPassword
                db.customerDao().upsert(customer.copy(password = newPassword))
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    profileMessage = "Пароль обновлён. Теперь вы можете войти."
                )
            } catch (e: Exception) {
                updateError("Не удалось сбросить пароль: ${e.message}")
            }
        }
    }

    fun toggleFavorite(productId: String) {
        val favs = _uiState.value.favorites.toMutableSet()
        if (favs.contains(productId)) favs.remove(productId) else favs.add(productId)
        _uiState.value = _uiState.value.copy(favorites = favs)
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
                updateError("Укажите email.")
                false
            }

            !trimmedEmail.contains("@") -> {
                updateError("Введите корректный email.")
                false
            }

            password.length < 6 -> {
                updateError("Пароль должен быть не менее 6 символов.")
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

    private fun persistCustomer(profile: UserProfile, address: String, phone: String) {
        viewModelScope.launch {
            db.customerDao().upsert(
                CustomerEntity(
                    uid = profile.uid,
                    email = profile.email,
                    displayName = profile.displayName,
                    role = profile.role,
                    address = address.ifBlank { null },
                    phone = phone.ifBlank { null },
                    password = profile.email?.let { userStore[it.trim()] }
                )
            )
        }
    }

    private fun loadCustomerDetails(profile: UserProfile) {
        val email = profile.email ?: return
        viewModelScope.launch {
            val entity = db.customerDao().findByEmail(email.trim())
            if (entity != null) {
                _uiState.value = _uiState.value.copy(
                    shippingAddress = entity.address ?: "",
                    phone = entity.phone ?: ""
                )
            }
        }
    }

    private fun CustomerEntity.toProfile(): UserProfile =
        UserProfile(uid = uid, email = email, displayName = displayName, role = role)
}

