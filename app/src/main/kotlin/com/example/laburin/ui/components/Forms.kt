package com.example.laburin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.laburin.R
import com.example.laburin.domain.model.Category

@Composable
fun FormScreen(title: String, onClose: () -> Unit, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.auth_close))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            Text(subtitle)
        }
        Spacer(Modifier.height(24.dp))
        content()
    }
}

@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    placeholder: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
) {
    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = error != null,
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
fun CategoryField(label: String, selected: Category?, onSelect: (Category) -> Unit, error: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text(selected?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.form_pick_category))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Category.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(stringResource(category.labelRes())) },
                    onClick = {
                        expanded = false
                        onSelect(category)
                    },
                )
            }
        }
    }
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    Spacer(Modifier.height(12.dp))
}

@Composable
fun FormError(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
fun SubmitButton(text: String, isSubmitting: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
        shape = RoundedCornerShape(16.dp),
    ) {
        if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
        }
        Text(text)
    }
    Spacer(Modifier.height(8.dp))
}
