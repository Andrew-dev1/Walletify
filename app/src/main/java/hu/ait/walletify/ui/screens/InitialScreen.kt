package hu.ait.walletify.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.ait.walletify.ui.theme.WalletifyTheme

@Composable
fun InitialScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section - App Name
        Text(
            text = "Walletify",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50), // Green color
            modifier = Modifier.padding(top = 48.dp)
        )

        // Middle Section - Image and Description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Placeholder for image - replace with actual resource
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder - replace painterResource with your actual image
                // Image(
                //     painter = painterResource(id = R.drawable.wallet_icon),
                //     contentDescription = "Wallet Icon",
                //     modifier = Modifier.size(200.dp)
                // )

                // Temporary placeholder box
                Surface(
                    modifier = Modifier.size(200.dp),
                    color = Color.LightGray,
                    shape = MaterialTheme.shapes.medium
                ) {}
            }

            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = "Take control of your finances",
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Track spending, set budgets, and achieve your financial goals—all in one place.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 24.sp
            )
        }

        // Bottom Section - Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // Create Account Button (Outlined)
            OutlinedButton(
                onClick = { /* Navigate to sign up */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Log In Button (Text)
            TextButton(
                onClick = { /* Navigate to login */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Log In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InitialScreenPreview() {
    WalletifyTheme {
        InitialScreen()
    }
}