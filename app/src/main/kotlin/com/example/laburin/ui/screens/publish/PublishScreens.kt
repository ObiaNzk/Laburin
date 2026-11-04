package com.example.laburin.ui.screens.publish

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.AlertDialog
import com.example.laburin.domain.publish.isClosing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.laburin.R
import com.example.laburin.data.publish.QrCode
import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.model.ApplicantStatus
import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.ui.components.CategoryField
import com.example.laburin.ui.components.FormError
import com.example.laburin.ui.components.FormField
import com.example.laburin.ui.components.FormScreen
import com.example.laburin.ui.components.SubmitButton

// --- Publish form ---

@Composable
fun PublishRoute(
    onPublished: (offerId: String) -> Unit,
    onNeedsBusinessAccount: () -> Unit,
    onBack: () -> Unit,
    viewModel: PublishViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PublishEffect.NeedsBusinessAccount -> onNeedsBusinessAccount()
                is PublishEffect.Published -> onPublished(effect.offerId)
            }
        }
    }
    PublishContent(
        state, viewModel::onTitleChange, viewModel::onCategoryChange, viewModel::onScheduleChange,
        viewModel::onDescriptionChange, viewModel::onPublish, onBack,
    )
}

@Composable
fun PublishContent(
    state: PublishUiState,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onScheduleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
) {
    FormScreen(title = stringResource(R.string.publish_title), onClose = onBack) {
        OfferFields(state.title, state.category, state.schedule, state.description, state.fieldErrors,
            onTitleChange, onCategoryChange, onScheduleChange, onDescriptionChange)
        Text(stringResource(R.string.publish_location_note), fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        SubmitButton(stringResource(R.string.publish_submit), state.isPublishing, onPublish)
    }
}

/** The offer's fields, shared by publishing and editing. */
@Composable
private fun OfferFields(
    title: String,
    category: Category?,
    schedule: String,
    description: String,
    errors: Set<PublishField>,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onScheduleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    FormField(stringResource(R.string.publish_job), title, onTitleChange,
        placeholder = stringResource(R.string.publish_job_hint),
        error = stringResource(R.string.publish_error_title).takeIf { PublishField.TITLE in errors })
    CategoryField(stringResource(R.string.publish_category), category, onCategoryChange,
        error = stringResource(R.string.publish_error_category).takeIf { PublishField.CATEGORY in errors })
    FormField(stringResource(R.string.publish_schedule), schedule, onScheduleChange,
        placeholder = stringResource(R.string.publish_schedule_hint),
        error = stringResource(R.string.publish_error_schedule).takeIf { PublishField.SCHEDULE in errors })
    FormField(stringResource(R.string.publish_description), description, onDescriptionChange,
        placeholder = stringResource(R.string.publish_description_hint), singleLine = false,
        error = stringResource(R.string.publish_error_description).takeIf { PublishField.DESCRIPTION in errors })
}

// --- Business sign-up ---

@Composable
fun BusinessSignUpRoute(
    onSignedUp: () -> Unit,
    onLogin: () -> Unit,
    onClose: () -> Unit,
    viewModel: BusinessSignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == BusinessSignUpEffect.SignedUp) onSignedUp() }
    }
    BusinessSignUpContent(
        state, viewModel::onNameChange, viewModel::onCategoryChange, viewModel::onScheduleChange, viewModel::onAddressChange,
        viewModel::onEmailChange, viewModel::onPasswordChange, viewModel::onSubmit, onLogin, onClose,
    )
}

@Composable
fun BusinessSignUpContent(
    state: BusinessSignUpUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onScheduleChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onLogin: () -> Unit,
    onClose: () -> Unit,
) {
    val errors = state.fieldErrors
    FormScreen(
        title = stringResource(R.string.business_signup_title),
        subtitle = stringResource(R.string.business_signup_subtitle),
        onClose = onClose,
    ) {
        FormField(stringResource(R.string.business_name), state.name, onNameChange,
            error = stringResource(R.string.business_error_name).takeIf { BusinessSignUpField.NAME in errors })
        CategoryField(stringResource(R.string.publish_category), state.category, onCategoryChange,
            error = stringResource(R.string.publish_error_category).takeIf { BusinessSignUpField.CATEGORY in errors })
        FormField(stringResource(R.string.business_schedule), state.schedule, onScheduleChange,
            error = stringResource(R.string.publish_error_schedule).takeIf { BusinessSignUpField.SCHEDULE in errors })
        FormField(stringResource(R.string.business_address), state.address, onAddressChange,
            error = stringResource(R.string.business_error_address).takeIf { BusinessSignUpField.ADDRESS in errors })
        FormField(stringResource(R.string.auth_email), state.email, onEmailChange, keyboard = KeyboardType.Email,
            error = stringResource(R.string.signup_error_email).takeIf { BusinessSignUpField.EMAIL in errors })
        FormField(stringResource(R.string.auth_password), state.password, onPasswordChange, keyboard = KeyboardType.Password,
            secret = true, placeholder = stringResource(R.string.signup_password_hint),
            error = stringResource(R.string.signup_error_password).takeIf { BusinessSignUpField.PASSWORD in errors })
        state.error?.let {
            FormError(
                stringResource(
                    when (it) {
                        BusinessSignUpError.EmailTaken -> R.string.signup_error_taken
                        BusinessSignUpError.Network -> R.string.signup_error_network
                        BusinessSignUpError.Server -> R.string.signup_error_server
                    }
                )
            )
        }
        SubmitButton(stringResource(R.string.business_submit), state.isSubmitting, onSubmit)
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.business_have_account))
        }
    }
}

// --- Published offer ---

@Composable
fun PublishedOfferRoute(
    onBack: () -> Unit,
    onEdit: (offerId: String) -> Unit,
    viewModel: PublishedOfferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTemplate = stringResource(R.string.published_share_text, "%1\$s", "%2\$s")
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PublishedOfferEffect.Share -> {
                    val text = String.format(shareTemplate, effect.title.lowercase(), effect.link)
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                }
            }
        }
    }
    PublishedOfferContent(
        state = state, onShare = viewModel::onShare, onBack = onBack,
        onApprove = viewModel::onApprove, onReject = viewModel::onReject,
        onEdit = onEdit, onCloseOffer = viewModel::onCloseOffer,
    )
}

@Composable
fun PublishedOfferContent(
    state: PublishedOfferUiState,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onApprove: (applicationId: String) -> Unit = {},
    onReject: (applicationId: String) -> Unit = {},
    onEdit: (offerId: String) -> Unit = {},
    onCloseOffer: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(stringResource(R.string.detail_back))
        }
        when (state) {
            PublishedOfferUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            PublishedOfferUiState.NotFound -> Text(stringResource(R.string.published_not_found), fontWeight = FontWeight.Bold)
            is PublishedOfferUiState.Content -> {
                Text(state.offer.title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(stringResource(state.offer.status.labelRes()), fontWeight = FontWeight.Bold)
                when (state.offer.status) {
                    PublishedOfferStatus.PENDING_PUBLISH ->
                        Text(stringResource(R.string.published_pending_hint), color = Color.Gray, fontSize = 12.sp)
                    PublishedOfferStatus.EDIT_PENDING_SEND, PublishedOfferStatus.CLOSE_PENDING_SEND ->
                        Text(stringResource(R.string.published_change_pending_hint), color = Color.Gray, fontSize = 12.sp)
                    PublishedOfferStatus.ACTIVE, PublishedOfferStatus.CLOSED -> Unit
                }
                if (!state.offer.status.isClosing()) {
                    ManageButtons(onEdit = { onEdit(state.offer.id) }, onCloseOffer = onCloseOffer)
                }
                Text(stringResource(R.string.published_qr_title), fontWeight = FontWeight.Bold)
                QrImage(state.qrLink, Modifier.align(Alignment.CenterHorizontally))
                Text(
                    stringResource(R.string.published_qr_hint), fontSize = 12.sp, color = Color.Gray,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.published_share))
                }
                ApplicantsSection(state.applicants, onApprove, onReject)

            }
        }
    }
}

@Composable
private fun ManageButtons(onEdit: () -> Unit, onCloseOffer: () -> Unit) {
    var confirmClose by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.published_edit)) }
        OutlinedButton(onClick = { confirmClose = true }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.published_close))
        }
    }
    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text(stringResource(R.string.published_close_confirm_title)) },
            text = { Text(stringResource(R.string.published_close_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = false
                    onCloseOffer()
                }) { Text(stringResource(R.string.published_close_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClose = false }) { Text(stringResource(R.string.published_close_cancel)) }
            },
        )
    }
}

@Composable
private fun ApplicantsSection(applicants: List<Applicant>, onApprove: (String) -> Unit, onReject: (String) -> Unit) {
    Text(stringResource(R.string.applicants_title, applicants.size), fontWeight = FontWeight.Bold, fontSize = 18.sp)
    if (applicants.isEmpty()) {
        Text(stringResource(R.string.applicants_empty), color = Color.Gray)
        return
    }
    applicants.forEach { applicant ->
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(applicant.candidateName, fontWeight = FontWeight.Bold)
            applicant.cvFileName?.let { Text(stringResource(R.string.applicants_cv, it)) }
            // RNF-05: the contact is shown only once the server has confirmed the match.
            val contact = applicant.contactEmail
            Text(
                if (contact != null) stringResource(R.string.applicants_contact, contact) else stringResource(R.string.applicants_contact_hidden),
                color = if (contact != null) Color.Unspecified else Color.Gray,
                fontSize = 13.sp,
            )
            when (applicant.status) {
                ApplicantStatus.PENDING_REVIEW -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onApprove(applicant.applicationId) }, colors = ButtonDefaults.buttonColors(Color.DarkGray)) {
                        Text(stringResource(R.string.applicants_approve))
                    }
                    OutlinedButton(onClick = { onReject(applicant.applicationId) }) {
                        Text(stringResource(R.string.applicants_reject))
                    }
                }
                ApplicantStatus.APPROVAL_PENDING_SEND -> Text(stringResource(R.string.applicants_approval_pending), color = Color.Gray)
                ApplicantStatus.REJECTION_PENDING_SEND -> Text(stringResource(R.string.applicants_rejection_pending), color = Color.Gray)
                ApplicantStatus.APPROVED -> Text(stringResource(R.string.applicants_approved), fontWeight = FontWeight.Bold)
                ApplicantStatus.REJECTED -> Text(stringResource(R.string.applicants_rejected), color = Color.Gray)
            }
        }
    }
}

@Composable
private fun QrImage(link: String, modifier: Modifier = Modifier) {
    val matrix = remember(link) { QrCode.encode(link, QR_MODULES) }
    val description = stringResource(R.string.published_qr_description)
    Canvas(
        modifier
            .size(220.dp)
            .semantics { contentDescription = description }
    ) {
        val cell = size.width / matrix.size
        drawRect(Color.White)
        matrix.forEachIndexed { y, row ->
            row.forEachIndexed { x, dark ->
                if (dark) drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = Size(cell, cell))
            }
        }
    }
}

/** Matrix resolution; the Canvas scales it to the on-screen size. */
private const val QR_MODULES = 220

private fun PublishedOfferStatus.labelRes() = when (this) {
    PublishedOfferStatus.PENDING_PUBLISH -> R.string.published_status_pending
    PublishedOfferStatus.EDIT_PENDING_SEND -> R.string.published_status_edit_pending
    PublishedOfferStatus.CLOSE_PENDING_SEND -> R.string.published_status_close_pending
    PublishedOfferStatus.ACTIVE -> R.string.published_status_active
    PublishedOfferStatus.CLOSED -> R.string.published_status_closed
}

// --- Edit offer ---

@Composable
fun EditOfferContent(
    state: EditOfferUiState,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onScheduleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    FormScreen(title = stringResource(R.string.edit_title), onClose = onBack) {
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            EditOfferForm(state, onTitleChange, onCategoryChange, onScheduleChange, onDescriptionChange, onSave)
        }
    }
}

@Composable
private fun EditOfferForm(
    state: EditOfferUiState,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onScheduleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (state.error != EditOfferError.NotFound) {
            OfferFields(state.title, state.category, state.schedule, state.description, state.fieldErrors,
                onTitleChange, onCategoryChange, onScheduleChange, onDescriptionChange)
        }
        state.error?.let {
            FormError(
                stringResource(
                    when (it) {
                        EditOfferError.Closed -> R.string.edit_error_closed
                        EditOfferError.NotFound -> R.string.edit_error_not_found
                        EditOfferError.NotAllowed -> R.string.edit_error_not_allowed
                    }
                )
            )
        }
        if (state.error == null || state.error == EditOfferError.NotAllowed) {
            Spacer(Modifier.height(12.dp))
            SubmitButton(stringResource(R.string.edit_submit), state.isSaving, onSave)
        }
    }
}

@Composable
fun EditOfferRoute(onDone: () -> Unit, viewModel: EditOfferViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect -> if (effect == EditOfferEffect.Saved) onDone() }
    }
    EditOfferContent(
        state, viewModel::onTitleChange, viewModel::onCategoryChange, viewModel::onScheduleChange,
        viewModel::onDescriptionChange, viewModel::onSave, onDone,
    )
}
