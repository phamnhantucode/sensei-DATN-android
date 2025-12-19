package com.phamnhantucode.aicareercoach.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CreditExhaustedDialog(
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Out of Credits") },
        text = { Text("You have used all your AI credits. Please purchase more to continue using these features.") },
        confirmButton = {
            TextButton(onClick = onPurchase) {
                Text("Get Credits")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
