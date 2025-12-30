package com.phamnhantucode.aicareercoach.ui.payment

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val paymentSheet = rememberPaymentSheet(viewModel::onPaymentResult)

    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady && uiState.publishableKey != null) {
             PaymentConfiguration.init(context, uiState.publishableKey!!)
             
             uiState.customer?.let { customer ->
                 uiState.ephemeralKey?.let { ephemeralKey ->
                     uiState.paymentIntent?.let { paymentIntent ->
                         paymentSheet.presentWithPaymentIntent(
                             paymentIntent,
                             PaymentSheet.Configuration(
                                 merchantDisplayName = "AI Career Coach",
                                 customer = PaymentSheet.CustomerConfiguration(
                                     id = customer,
                                     ephemeralKeySecret = ephemeralKey
                                 )
                             )
                         )
                     }
                 }
             }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.preparePaymentSheet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Processing Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading || (uiState.isReady && uiState.paymentResult == null)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Preparing secure checkout...")
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::preparePaymentSheet) {
                            Text("Retry Payment")
                        }
                    }

                    if (uiState.paymentResult != null) {
                         Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.paymentResult!!,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                         if (uiState.paymentResult!!.contains("failed", ignoreCase = true) || uiState.paymentResult!!.contains("canceled", ignoreCase = true)) {
                             Spacer(modifier = Modifier.height(16.dp))
                             Button(onClick = viewModel::preparePaymentSheet) {
                                 Text("Retry Payment")
                             }
                         }
                    }
                }
            }
        }
    }
}
