package hu.ait.walletify.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun RegistrationCredentialsScreen(
    purpose: String,
    source: String,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val uiState = viewModel.loginUiState

    // Handle registration success
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.RegisterSuccess) {
            onComplete() // Navigate to dashboard
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {

        Text(
            text = "Create your account",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Set up your login credentials",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            enabled = uiState !is LoginUiState.Loading
        )

        var showPassword by remember {mutableStateOf(false)}
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true,
            visualTransformation =
                if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            },
            enabled = uiState !is LoginUiState.Loading
        )

        var showPasswordConfirm by remember {mutableStateOf(false)}
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation =
                if (showPasswordConfirm) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            },
            enabled = uiState !is LoginUiState.Loading,
            isError = confirmPassword.isNotEmpty() && password != confirmPassword
        )
        Spacer(modifier = Modifier.height(16.dp))

        // shows error message if password is not valid and prioritizes requirements
        when {
            password.isNotEmpty() && !isPasswordValid(password) -> {
                Text(
                    text = "Password must contain: uppercase, lowercase, number, and special character (!@?&+#)",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            confirmPassword.isNotEmpty() && password != confirmPassword -> {
                Text(
                    text = "Passwords do not match",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        }

        if (uiState is LoginUiState.Error) {
            Text(
                text = uiState.errorMessage ?: "Registration failed",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = uiState !is LoginUiState.Loading
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    viewModel.registerUser(
                        email, password,
                        purpose,
                        source)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                enabled = uiState !is LoginUiState.Loading &&
                        email.isNotEmpty() &&
                        password.isNotEmpty() &&
                        password == confirmPassword &&
                        isPasswordValid(password)
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Create Account")
                }
            }
        }
    }
}

fun isPasswordValid(password: String): Boolean {
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { it in "!@?&+#" }

    return hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
}