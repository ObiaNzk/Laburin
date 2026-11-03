package com.example.laburin.ui.screens.auth

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.BuildConfig
import com.example.laburin.R
import com.example.laburin.data.fakebackend.FakeBackend
import com.example.laburin.domain.model.SignUpField

@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    onSignUpClick: () -> Unit,
    onClose: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == LoginEffect.LoggedIn) onLoggedIn() }
    }
    // Only while the fake backend is in use: shows a fictional demo account for reviewers.
    val demoHint = if (BuildConfig.DEBUG) {
        stringResource(R.string.login_demo_hint, FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD)
    } else {
        null
    }
    LoginContent(
        state, demoHint, viewModel::onEmailChange, viewModel::onPasswordChange, viewModel::onSubmit, onSignUpClick, onClose,
    )
}

@Composable
fun LoginContent(
    state: LoginUiState,
    demoHint: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSignUpClick: () -> Unit,
    onClose: () -> Unit,
) {
    AuthForm(title = stringResource(R.string.login_title), onClose = onClose) {
        Field(stringResource(R.string.auth_email), state.email, onEmailChange, keyboard = KeyboardType.Email)
        Field(
            stringResource(R.string.auth_password), state.password, onPasswordChange,
            keyboard = KeyboardType.Password, secret = true,
        )
        state.error?.let { ErrorText(stringResource(it.messageRes())) }
        SubmitButton(stringResource(R.string.login_submit), state.isSubmitting, onSubmit)
        TextButton(onClick = onSignUpClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.login_no_account))
        }
        if (demoHint != null) {
            Text(demoHint, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SignUpRoute(
    onSignedUp: () -> Unit,
    onClose: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == SignUpEffect.SignedUp) onSignedUp() }
    }
    SignUpContent(
        state, viewModel::onFirstNameChange, viewModel::onLastNameChange, viewModel::onEmailChange,
        viewModel::onPasswordChange, viewModel::onSubmit, onClose,
    )
}

@Composable
fun SignUpContent(
    state: SignUpUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
) {
    AuthForm(
        title = stringResource(R.string.signup_title),
        subtitle = stringResource(R.string.signup_subtitle),
        onClose = onClose,
    ) {
        val errors = state.fieldErrors
        Field(stringResource(R.string.signup_first_name), state.firstName, onFirstNameChange,
            error = stringResource(R.string.signup_error_first_name).takeIf { SignUpField.FIRST_NAME in errors })
        Field(stringResource(R.string.signup_last_name), state.lastName, onLastNameChange,
            error = stringResource(R.string.signup_error_last_name).takeIf { SignUpField.LAST_NAME in errors })
        Field(stringResource(R.string.auth_email), state.email, onEmailChange, keyboard = KeyboardType.Email,
            placeholder = stringResource(R.string.signup_email_hint),
            error = stringResource(R.string.signup_error_email).takeIf { SignUpField.EMAIL in errors })
        Field(stringResource(R.string.auth_password), state.password, onPasswordChange, keyboard = KeyboardType.Password,
            secret = true, placeholder = stringResource(R.string.signup_password_hint),
            error = stringResource(R.string.signup_error_password).takeIf { SignUpField.PASSWORD in errors })
        state.error?.let { ErrorText(stringResource(it.messageRes())) }
        SubmitButton(stringResource(R.string.signup_submit), state.isSubmitting, onSubmit)
        Text(
            stringResource(R.string.signup_cv_later), fontSize = 12.sp, color = Color.Gray,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AuthForm(title: String, onClose: () -> Unit, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
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
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    placeholder: String? = null,
    error: String? = null,
) {
    Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = error != null,
        placeholder = placeholder?.let { { Text(it) } },
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp))
}

@Composable
private fun SubmitButton(text: String, isSubmitting: Boolean, onClick: () -> Unit) {
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

private fun LoginError.messageRes() = when (this) {
    LoginError.MissingFields -> R.string.login_error_missing
    LoginError.InvalidCredentials -> R.string.login_error_credentials
    LoginError.Network -> R.string.login_error_network
    LoginError.Server -> R.string.login_error_server
}

private fun SignUpError.messageRes() = when (this) {
    SignUpError.EmailTaken -> R.string.signup_error_taken
    SignUpError.Network -> R.string.signup_error_network
    SignUpError.Server -> R.string.signup_error_server
}
