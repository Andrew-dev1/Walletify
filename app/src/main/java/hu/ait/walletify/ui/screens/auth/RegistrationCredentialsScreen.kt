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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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

@Composable
fun RegistrationCredentialsScreen(
    purpose: String,
    source: String,
    onComplete: () -> Unit,
    onRegisterUser: (String, String, String, String, String) -> Unit,
    onBack: () -> Unit,
    state: LoginUiState
) {
    var name by remember { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }


    // Handle registration success
    LaunchedEffect(state) {
        if (state is LoginUiState.RegisterSuccess) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
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
            value = name,
            onValueChange = { name = it },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                focusedLabelColor = Color(0xFF4CAF50)
            ),
            enabled = state !is LoginUiState.Loading
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            enabled = state !is LoginUiState.Loading
        )

        var showPassword by remember {mutableStateOf(false)}
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
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
            enabled = state !is LoginUiState.Loading
        )

        var showPasswordConfirm by remember {mutableStateOf(false)}
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            visualTransformation =
                if (showPasswordConfirm) VisualTransformation.None
                else PasswordVisualTransformation(),
            trailingIcon = {
                val icon = if (showPasswordConfirm) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = { showPasswordConfirm = !showPasswordConfirm }) {
                    Icon(imageVector = icon, contentDescription = null)
                }
            },
            enabled = state !is LoginUiState.Loading,
            isError = confirmPassword.isNotEmpty() && password != confirmPassword
        )
        Spacer(modifier = Modifier.height(16.dp))




        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = state !is LoginUiState.Loading
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                        onRegisterUser(name.trim(), email.trim(),
                            password.trim(), source, purpose)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                enabled = state !is LoginUiState.Loading &&
                        name.isNotBlank() &&
                        email.isNotBlank() &&
                        password.isNotBlank() &&
                        password == confirmPassword &&
                        isPasswordValid(password)

            ) {
                if (state is LoginUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Create Account")
                }
            }
        }

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

        if (state is LoginUiState.Error) {
            Text(
                text = state.errorMessage ?: "Registration failed",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
        }

    }
}

fun isPasswordValid(password: String): Boolean {
    val isAlphaNumeric = password.matches("^[a-zA-Z0-9]*$".toRegex())
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { it in "!@?&+#" }

    return hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar && isAlphaNumeric
}