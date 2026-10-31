package com.ngopimalang.app.features.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ngopimalang.app.core.components.AppCard
import com.ngopimalang.app.core.components.AppLogoMark
import com.ngopimalang.app.core.components.AppTextField
import com.ngopimalang.app.core.components.AuthSwitchLink
import com.ngopimalang.app.core.components.GradientBackdrop
import com.ngopimalang.app.core.components.PillButton
import com.ngopimalang.app.core.components.ScreenTitle
import com.ngopimalang.app.core.components.SectionLabel

@Composable
fun LoginScreen(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        GradientBackdrop(height = 280.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AppLogoMark(size = 56.dp)

            Spacer(modifier = Modifier.height(12.dp))

            ScreenTitle(
                title = "NgopiMalang",
                subtitle = "Temukan cafe di Malang sesuai suasana, fasilitas, dan anggaranmu."
            )

            Spacer(modifier = Modifier.height(16.dp))

            AppCard {
                SectionLabel(text = "MASUK KE AKUN")

                AppTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = "Email",
                    icon = Icons.Filled.Email,
                    isError = state.errorMessage != null,
                    keyboardType = KeyboardType.Email
                )

                AppTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    icon = Icons.Filled.Lock,
                    isError = state.errorMessage != null,
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                PillButton(
                    text = "Masuk",
                    onClick = onLoginClick,
                    loading = state.isLoading
                )

                AuthSwitchLink(
                    text = "Belum punya akun? Daftar",
                    onClick = onNavigateToRegister
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
