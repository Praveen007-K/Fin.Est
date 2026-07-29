package com.pkoder.finest.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Amount input.
 *
 * The previous version filtered input with `isDigit()`, which silently swallowed the decimal point
 * and made ₹123.50 impossible to type. This keeps digits plus a single separator, capped at two
 * decimals.
 */
@Composable
fun AmountTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(sanitizeAmountInput(raw)) },
        label = { Text(label) },
        prefix = { Text("₹") },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let {
            {
                Text(
                    text = it,
                    color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Keeps digits and at most one `.`, with at most two digits after it. */
internal fun sanitizeAmountInput(raw: String): String {
    val normalized = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
    val firstDot = normalized.indexOf('.')
    if (firstDot < 0) return normalized

    val whole = normalized.substring(0, firstDot)
    val fraction = normalized.substring(firstDot + 1).filter { it.isDigit() }.take(2)
    return "$whole.$fraction"
}
