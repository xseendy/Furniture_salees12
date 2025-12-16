package com.yourname.furnituresales.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourname.furnituresales.R
import com.yourname.furnituresales.ui.components.AuthFieldCard

@Composable
fun AuthScreen(
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
        Text(stringResource(R.string.auth_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = when {
                isResetMode -> stringResource(R.string.auth_subtitle_reset)
                isRegisterMode -> stringResource(R.string.auth_subtitle_register)
                else -> stringResource(R.string.auth_subtitle_welcome)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.padding(12.dp))
        AuthFieldCard {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.field_email)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.padding(6.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        if (isResetMode) stringResource(R.string.field_new_password) else stringResource(
                            R.string.field_password
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) stringResource(R.string.action_hide) else stringResource(R.string.action_show))
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
                    isResetMode -> stringResource(R.string.action_reset_password)
                    isRegisterMode -> stringResource(R.string.action_sign_up)
                    else -> stringResource(R.string.action_sign_in)
                }
            )
        }
        TextButton(onClick = {
            isRegisterMode = !isRegisterMode
            if (isRegisterMode) isResetMode = false
        }) {
            Text(if (isRegisterMode) stringResource(R.string.action_have_account) else stringResource(R.string.action_create_account))
        }
        TextButton(onClick = {
            isResetMode = !isResetMode
            if (isResetMode) isRegisterMode = false
        }) {
            Text(if (isResetMode) stringResource(R.string.action_back_to_sign_in) else stringResource(R.string.action_forgot_password))
        }
        TextButton(onClick = { if (!isLoading) onGuest() }) {
            Text(stringResource(R.string.action_continue_as_guest))
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
