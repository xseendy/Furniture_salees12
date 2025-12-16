package com.yourname.furnituresales.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import com.yourname.furnituresales.FurnitureUiState
import com.yourname.furnituresales.R
import com.yourname.furnituresales.data.Product
import com.yourname.furnituresales.ui.components.HeroBanner
import com.yourname.furnituresales.ui.components.ProductCard
import com.yourname.furnituresales.ui.util.USD_TO_RUB

private fun productCategoryKey(product: Product): String {
    val key = (product.id + " " + product.name).lowercase()
    return when {
        key.contains("диван") || key.contains("sofa") -> "sofa"
        key.contains("крес") || key.contains("chair") -> "chair"
        key.contains("кровать") || key.contains("bed") -> "bed"
        key.contains("стол") || key.contains("table") || key.contains("desk") -> "table"
        key.contains("ламп") || key.contains("торшер") || key.contains("lamp") -> "lamp"
        key.contains("ков") || key.contains("rug") -> "rug"
        key.contains("комод") || key.contains("буфет") || key.contains("sideboard") -> "storage"
        key.contains("стел") || key.contains("shelf") || key.contains("bookshelf") -> "shelf"
        else -> "other"
    }
}

@Composable
fun CatalogScreen(
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
    var favoritesOnly by remember { mutableStateOf(false) }
    var sortOrder by rememberSaveable { mutableStateOf("NONE") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var categoryKey by rememberSaveable { mutableStateOf("ALL") }
    var filtersExpanded by rememberSaveable { mutableStateOf(true) }

    val priceMinRub = remember(uiState.products) {
        uiState.products.minOfOrNull { it.price * USD_TO_RUB }?.toFloat() ?: 0f
    }
    val priceMaxRub = remember(uiState.products) {
        uiState.products.maxOfOrNull { it.price * USD_TO_RUB }?.toFloat() ?: 0f
    }
    var priceRangeStart by rememberSaveable { mutableStateOf(0f) }
    var priceRangeEnd by rememberSaveable { mutableStateOf(0f) }

    LaunchedEffect(priceMinRub, priceMaxRub) {
        if (priceRangeStart == 0f && priceRangeEnd == 0f && priceMaxRub > 0f) {
            priceRangeStart = priceMinRub
            priceRangeEnd = priceMaxRub
        }
    }

    val filteredProducts = remember(
        uiState.products,
        priceFilter,
        minPriceInput,
        maxPriceInput,
        favoritesOnly,
        sortOrder,
        uiState.favorites,
        searchQuery,
        categoryKey,
        priceRangeStart,
        priceRangeEnd
    ) {
        uiState.products
            .filter { product ->
                val priceRub = product.price * USD_TO_RUB
                val q = searchQuery.trim().lowercase()
                val searchMatches = q.isBlank() ||
                        product.name.lowercase().contains(q) ||
                        product.description.lowercase().contains(q)
                val categoryMatches = categoryKey == "ALL" || productCategoryKey(product) == categoryKey
                val priceMatches = when {
                    priceFilter == "RANGE" -> priceRub in priceRangeStart.toDouble()..priceRangeEnd.toDouble()
                    minPriceInput.isNotBlank() || maxPriceInput.isNotBlank() -> {
                        val minPrice = minPriceInput.toDoubleOrNull() ?: 0.0
                        val maxPrice = maxPriceInput.toDoubleOrNull() ?: Double.MAX_VALUE
                        priceRub >= minPrice && priceRub <= maxPrice
                    }

                    priceFilter == "LOW" -> priceRub < 40_000
                    priceFilter == "MID" -> priceRub in 40_000.0..80_000.0
                    priceFilter == "HIGH" -> priceRub > 80_000
                    else -> true
                }
                val favoritesMatch =
                    !favoritesOnly || uiState.favorites.contains(product.id)
                searchMatches && categoryMatches && priceMatches && favoritesMatch
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
                    ?: stringResource(R.string.home_guest)
                Text(
                    text = stringResource(R.string.home_hello, nameOrEmail),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.catalog_results_count, filteredProducts.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                Text(stringResource(R.string.action_toggle_filters))
            }
        }
        HeroBanner()

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.catalog_search_label)) },
            placeholder = { Text(stringResource(R.string.catalog_search_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.catalog_search_label)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    TextButton(onClick = { searchQuery = "" }) {
                        Text(stringResource(R.string.action_clear_search))
                    }
                } else {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0f)
                    )
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (categoryKey != "ALL") {
                FilterChip(
                    selected = true,
                    onClick = { categoryKey = "ALL" },
                    label = {
                        val labelRes = when (categoryKey) {
                            "sofa" -> R.string.catalog_category_sofas
                            "chair" -> R.string.catalog_category_chairs
                            "table" -> R.string.catalog_category_tables
                            "bed" -> R.string.catalog_category_beds
                            "lamp" -> R.string.catalog_category_lamps
                            "rug" -> R.string.catalog_category_rugs
                            "storage" -> R.string.catalog_category_storage
                            else -> R.string.catalog_category_all
                        }
                        Text(stringResource(labelRes), maxLines = 1)
                    },
                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) }
                )
            }
            if (searchQuery.isNotBlank()) {
                FilterChip(
                    selected = true,
                    onClick = { searchQuery = "" },
                    label = { Text(searchQuery, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) }
                )
            }
            if (favoritesOnly) {
                FilterChip(
                    selected = true,
                    onClick = { favoritesOnly = false },
                    label = { Text(stringResource(R.string.catalog_only_favorites), maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) }
                )
            }
            if (sortOrder != "NONE") {
                val sortLabelRes = if (sortOrder == "PRICE_ASC") R.string.catalog_sort_price_asc else R.string.catalog_sort_price_desc
                FilterChip(
                    selected = true,
                    onClick = { sortOrder = "NONE" },
                    label = { Text(stringResource(sortLabelRes), maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) }
                )
            }
            if (priceFilter == "LOW" || priceFilter == "MID" || priceFilter == "HIGH" || priceFilter == "RANGE") {
                val priceLabel = when (priceFilter) {
                    "LOW" -> stringResource(R.string.catalog_filter_low)
                    "MID" -> stringResource(R.string.catalog_filter_mid)
                    "HIGH" -> stringResource(R.string.catalog_filter_high)
                    else -> stringResource(R.string.catalog_price_slider_title)
                }
                FilterChip(
                    selected = true,
                    onClick = {
                        priceFilter = "ALL"
                        minPriceInput = ""
                        maxPriceInput = ""
                        priceRangeStart = priceMinRub
                        priceRangeEnd = priceMaxRub
                    },
                    label = { Text(priceLabel, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) }
                )
            }
        }

        if (filtersExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = categoryKey == "ALL",
                    onClick = { categoryKey = "ALL" },
                    label = { Text(stringResource(R.string.catalog_category_all)) }
                )
                FilterChip(
                    selected = categoryKey == "sofa",
                    onClick = { categoryKey = "sofa" },
                    label = { Text(stringResource(R.string.catalog_category_sofas), maxLines = 1) }
                )
                FilterChip(
                    selected = categoryKey == "chair",
                    onClick = { categoryKey = "chair" },
                    label = { Text(stringResource(R.string.catalog_category_chairs), maxLines = 1) }
                )
                FilterChip(
                    selected = categoryKey == "table",
                    onClick = { categoryKey = "table" },
                    label = { Text(stringResource(R.string.catalog_category_tables), maxLines = 1) }
                )
                FilterChip(
                    selected = categoryKey == "bed",
                    onClick = { categoryKey = "bed" },
                    label = { Text(stringResource(R.string.catalog_category_beds), maxLines = 1) }
                )
                FilterChip(
                    selected = categoryKey == "lamp",
                    onClick = { categoryKey = "lamp" },
                    label = { Text(stringResource(R.string.catalog_category_lamps), maxLines = 1) }
                )
                FilterChip(
                    selected = categoryKey == "rug",
                    onClick = { categoryKey = "rug" },
                    label = { Text(stringResource(R.string.catalog_category_rugs), maxLines = 1) }
                )
                FilterChip(
                    selected = categoryKey == "storage",
                    onClick = { categoryKey = "storage" },
                    label = { Text(stringResource(R.string.catalog_category_storage), maxLines = 1) }
                )
            }
        }

        Spacer(modifier = Modifier.padding(top = 4.dp))
        if (filtersExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.catalog_price_filter_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        priceFilter = "ALL"
                        minPriceInput = ""
                        maxPriceInput = ""
                        favoritesOnly = false
                        sortOrder = "NONE"
                        searchQuery = ""
                        categoryKey = "ALL"
                        priceRangeStart = priceMinRub
                        priceRangeEnd = priceMaxRub
                    }
                ) {
                    Text(stringResource(R.string.action_reset_filters))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = priceFilter == "ALL" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                    onClick = {
                        priceFilter = "ALL"
                        minPriceInput = ""
                        maxPriceInput = ""
                    },
                    label = { Text(stringResource(R.string.catalog_filter_all)) }
                )
                FilterChip(
                    selected = priceFilter == "LOW" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                    onClick = {
                        priceFilter = "LOW"
                        minPriceInput = ""
                        maxPriceInput = ""
                    },
                    label = { Text(stringResource(R.string.catalog_filter_low), maxLines = 1) }
                )
                FilterChip(
                    selected = priceFilter == "MID" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                    onClick = {
                        priceFilter = "MID"
                        minPriceInput = ""
                        maxPriceInput = ""
                    },
                    label = { Text(stringResource(R.string.catalog_filter_mid), maxLines = 1) }
                )
                FilterChip(
                    selected = priceFilter == "HIGH" && minPriceInput.isBlank() && maxPriceInput.isBlank(),
                    onClick = {
                        priceFilter = "HIGH"
                        minPriceInput = ""
                        maxPriceInput = ""
                    },
                    label = { Text(stringResource(R.string.catalog_filter_high), maxLines = 1) }
                )
                FilterChip(
                    selected = priceFilter == "RANGE",
                    onClick = {
                        priceFilter = "RANGE"
                        minPriceInput = ""
                        maxPriceInput = ""
                    },
                    label = { Text(stringResource(R.string.catalog_price_slider_title), maxLines = 1) }
                )
            }

            if (priceFilter == "RANGE" && priceMaxRub > 0f) {
                Spacer(modifier = Modifier.padding(top = 6.dp))
                RangeSlider(
                    value = priceRangeStart..priceRangeEnd,
                    onValueChange = { range ->
                        priceRangeStart = range.start
                        priceRangeEnd = range.endInclusive
                    },
                    valueRange = priceMinRub..priceMaxRub
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${priceRangeStart.toInt()} ₽", style = MaterialTheme.typography.bodySmall)
                    Text(text = "${priceRangeEnd.toInt()} ₽", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (filtersExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text(stringResource(R.string.catalog_only_favorites)) }
                )
                FilterChip(
                    selected = sortOrder == "PRICE_ASC",
                    onClick = { sortOrder = if (sortOrder == "PRICE_ASC") "NONE" else "PRICE_ASC" },
                    label = { Text(stringResource(R.string.catalog_sort_price_asc)) }
                )
                FilterChip(
                    selected = sortOrder == "PRICE_DESC",
                    onClick = { sortOrder = if (sortOrder == "PRICE_DESC") "NONE" else "PRICE_DESC" },
                    label = { Text(stringResource(R.string.catalog_sort_price_desc)) }
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

        if (!uiState.isLoading && uiState.error == null && filteredProducts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.catalog_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.catalog_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        priceFilter = "ALL"
                        minPriceInput = ""
                        maxPriceInput = ""
                        favoritesOnly = false
                        sortOrder = "NONE"
                        searchQuery = ""
                        categoryKey = "ALL"
                    }
                ) {
                    Text(stringResource(R.string.action_reset_filters))
                }
            }
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
