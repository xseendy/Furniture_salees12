package com.yourname.furnituresales.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yourname.furnituresales.R

@Composable
fun PaymentSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        stringResource(R.string.payment_card),
        stringResource(R.string.payment_cash),
        stringResource(R.string.payment_bank_transfer)
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == option, onClick = { onSelect(option) })
                Text(option)
            }
        }
    }
}
