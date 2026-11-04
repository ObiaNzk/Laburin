package com.example.laburin.ui.screens.publish

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.model.ApplicantStatus
import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.model.SalaryPeriod
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PublishContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    // --- Publish form ---

    private fun showForm(state: PublishUiState) = composeRule.setContent {
        LaburinTheme {
            PublishContent(
                state = state, onTitleChange = {}, onCategoryChange = { clicks += "category:$it" }, onScheduleChange = {},
                onDescriptionChange = {}, onPublish = { clicks += "publish" }, onBack = { clicks += "back" },
                onSalaryMinChange = { clicks += "min:$it" }, onSalaryMaxChange = { clicks += "max:$it" },
                onSalaryPeriodChange = { clicks += "period:$it" },
            )
        }
    }

    @Test
    fun formPublishes() {
        showForm(PublishUiState())

        composeRule.onNodeWithText("Nueva búsqueda").assertIsDisplayed()
        composeRule.onNodeWithText("Publicar búsqueda").performScrollTo().performClick()

        assertEquals(listOf("publish"), clicks)
    }

    @Test
    fun categoryIsChosenFromTheList() {
        showForm(PublishUiState())

        composeRule.onNodeWithText("Elegí un rubro").performClick()
        composeRule.onNodeWithText("Gastronomía").performClick()

        assertEquals(listOf("category:GASTRONOMY"), clicks)
    }

    @Test
    fun missingFieldsAreExplained() {
        showForm(PublishUiState(fieldErrors = setOf(PublishField.TITLE, PublishField.CATEGORY)))

        composeRule.onNodeWithText("Indicá el puesto").assertExists()
        composeRule.onNodeWithText("Elegí el rubro").assertExists()
        composeRule.onNodeWithText("Indicá el horario").assertDoesNotExist()
    }

    // --- Business sign-up ---

    @Test
    fun businessSignUpExplainsErrorsAndSubmits() {
        composeRule.setContent {
            LaburinTheme {
                BusinessSignUpContent(
                    state = BusinessSignUpUiState(fieldErrors = setOf(BusinessSignUpField.ADDRESS), error = BusinessSignUpError.EmailTaken),
                    onNameChange = {}, onCategoryChange = {}, onScheduleChange = {}, onAddressChange = {}, onEmailChange = {},
                    onPasswordChange = {}, onSubmit = { clicks += "submit" }, onLogin = { clicks += "login" }, onClose = {},
                )
            }
        }

        composeRule.onNodeWithText("Registrá tu comercio").assertIsDisplayed()
        composeRule.onNodeWithText("Indicá la dirección del comercio").assertExists()
        composeRule.onNodeWithText("Ya existe una cuenta con ese email").assertExists()
        composeRule.onNodeWithText("Crear cuenta de comercio").performScrollTo().performClick()
        composeRule.onNodeWithText("Ya tengo cuenta").performScrollTo().performClick()

        assertEquals(listOf("submit", "login"), clicks)
    }

    // --- Published offer ---

    private fun showPublished(status: PublishedOfferStatus, applicants: List<Applicant> = emptyList()) = composeRule.setContent {
        LaburinTheme {
            PublishedOfferContent(
                state = PublishedOfferUiState.Content(
                    PublishedOffer("p-1", "Mozo", Category.GASTRONOMY, "Sábados", "[DESCRIPCION]", status, Instant.EPOCH),
                    qrLink = "laburin://offer/p-1",
                    applicants = applicants,
                ),
                onShare = { clicks += "share" }, onBack = {},
                onApprove = { clicks += "approve:$it" }, onReject = { clicks += "reject:$it" },
                onEdit = { clicks += "edit:$it" }, onCloseOffer = { clicks += "close" },
            )
        }
    }

    private fun applicant(status: ApplicantStatus, contact: String? = null) =
        Applicant("a-1", "Postulante Uno", "cv-uno.pdf", status, contact)

    // --- Applicants and match (RF-04, RNF-05) ---

    @Test
    fun applicantsShowTheCvWithTheContactHiddenAndCanBeReviewed() {
        showPublished(PublishedOfferStatus.ACTIVE, listOf(applicant(ApplicantStatus.PENDING_REVIEW)))

        composeRule.onNodeWithText("Postulantes (1)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Postulante Uno").assertExists()
        composeRule.onNodeWithText("CV: cv-uno.pdf").assertExists()
        composeRule.onNodeWithText("Contacto oculto hasta aprobar").assertExists()
        composeRule.onNodeWithText("Aprobar").performScrollTo().performClick()
        composeRule.onNodeWithText("Rechazar").performScrollTo().performClick()

        assertEquals(listOf("approve:a-1", "reject:a-1"), clicks)
    }

    @Test
    fun aDecisionWaitingToBeSentKeepsTheContactHidden() {
        showPublished(PublishedOfferStatus.ACTIVE, listOf(applicant(ApplicantStatus.APPROVAL_PENDING_SEND)))

        composeRule.onNodeWithText("Aprobación pendiente de envío").assertExists()
        composeRule.onNodeWithText("Contacto oculto hasta aprobar").assertExists()
        composeRule.onNodeWithText("Aprobar").assertDoesNotExist()
    }

    @Test
    fun aConfirmedMatchShowsTheContact() {
        showPublished(PublishedOfferStatus.ACTIVE, listOf(applicant(ApplicantStatus.APPROVED, contact = "persona@demo.laburin")))

        composeRule.onNodeWithText("Contacto: persona@demo.laburin").assertExists()
        composeRule.onNodeWithText("Contacto oculto hasta aprobar").assertDoesNotExist()
    }

    @Test
    fun aRejectedApplicantSaysSo() {
        showPublished(PublishedOfferStatus.ACTIVE, listOf(applicant(ApplicantStatus.REJECTED)))

        composeRule.onNodeWithText("Rechazado").assertExists()
        composeRule.onNodeWithText("Aprobar").assertDoesNotExist()
    }

    @Test
    fun noApplicantsYet() {
        showPublished(PublishedOfferStatus.ACTIVE)

        composeRule.onNodeWithText("Todavía no hay postulantes").assertExists()
    }

    @Test
    fun publishedOfferShowsItsQrAndCanBeShared() {
        showPublished(PublishedOfferStatus.ACTIVE)

        composeRule.onNodeWithText("Mozo").assertIsDisplayed()
        composeRule.onNodeWithText("Publicada").assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Código QR de la búsqueda")).assertIsDisplayed()
        composeRule.onNodeWithText("Compartir").performScrollTo().performClick()

        assertEquals(listOf("share"), clicks)
    }

    @Test
    fun pendingPublicationIsExplained() {
        showPublished(PublishedOfferStatus.PENDING_PUBLISH)

        composeRule.onNodeWithText("Pendiente de publicación").assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no se ve en el mapa: se publica cuando vuelva la conexión.").assertExists()
    }

    // --- Edit and close (RF-03) ---

    @Test
    fun anOpenOfferCanBeEdited() {
        showPublished(PublishedOfferStatus.ACTIVE)

        composeRule.onNodeWithText("Editar").performScrollTo().performClick()

        assertEquals(listOf("edit:p-1"), clicks)
    }

    @Test
    fun closingAsksForConfirmationFirst() {
        showPublished(PublishedOfferStatus.ACTIVE)

        composeRule.onNodeWithText("Cerrar búsqueda").performScrollTo().performClick()
        composeRule.onNodeWithText("¿Cerrar esta búsqueda?").assertIsDisplayed()
        assertEquals(emptyList<String>(), clicks)

        composeRule.onNodeWithText("Sí, cerrar").performClick()

        assertEquals(listOf("close"), clicks)
        composeRule.onNodeWithText("¿Cerrar esta búsqueda?").assertDoesNotExist()
    }

    @Test
    fun cancellingTheConfirmationKeepsTheOfferOpen() {
        showPublished(PublishedOfferStatus.ACTIVE)

        composeRule.onNodeWithText("Cerrar búsqueda").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancelar").performClick()

        assertEquals(emptyList<String>(), clicks)
        composeRule.onNodeWithText("¿Cerrar esta búsqueda?").assertDoesNotExist()
    }

    @Test
    fun aClosedOfferCanNoLongerBeEditedOrClosed() {
        showPublished(PublishedOfferStatus.CLOSED)

        composeRule.onNodeWithText("Cerrada").assertIsDisplayed()
        composeRule.onNodeWithText("Editar").assertDoesNotExist()
        composeRule.onNodeWithText("Cerrar búsqueda").assertDoesNotExist()
    }

    @Test
    fun aCloseWaitingToBeSentIsExplainedAndCannotBeEdited() {
        showPublished(PublishedOfferStatus.CLOSE_PENDING_SEND)

        composeRule.onNodeWithText("Cierre pendiente de envío").assertIsDisplayed()
        composeRule.onNodeWithText("Editar").assertDoesNotExist()
        composeRule.onNodeWithText("Cerrar búsqueda").assertDoesNotExist()
    }

    @Test
    fun unsentChangesAreExplained() {
        showPublished(PublishedOfferStatus.EDIT_PENDING_SEND)

        composeRule.onNodeWithText("Cambios pendientes de envío").assertIsDisplayed()
        composeRule.onNodeWithText("Editar").performScrollTo().assertIsDisplayed()
    }

    private fun showEdit(state: EditOfferUiState) = composeRule.setContent {
        LaburinTheme {
            EditOfferContent(
                state = state, onTitleChange = {}, onCategoryChange = {}, onScheduleChange = {}, onDescriptionChange = {},
                onSave = { clicks += "save" }, onBack = { clicks += "back" },
                onSalaryMinChange = {}, onSalaryMaxChange = {}, onSalaryPeriodChange = {},
            )
        }
    }

    @Test
    fun theEditFormShowsTheCurrentContentAndSaves() {
        showEdit(EditOfferUiState(isLoading = false, title = "Mozo", category = Category.GASTRONOMY, schedule = "Sábados", description = "Salón"))

        composeRule.onNodeWithText("Editar búsqueda").assertIsDisplayed()
        composeRule.onNodeWithText("Mozo").assertIsDisplayed()
        composeRule.onNodeWithText("Guardar cambios").performScrollTo().performClick()

        assertEquals(listOf("save"), clicks)
    }

    @Test
    fun aClosedOfferExplainsWhyItCannotBeSaved() {
        showEdit(EditOfferUiState(isLoading = false, title = "Mozo", error = EditOfferError.Closed))

        composeRule.onNodeWithText("Esta búsqueda ya está cerrada y no se puede editar.").performScrollTo().assertIsDisplayed()
    }

    // --- Salary (optional) ---

    @Test
    fun theFormOffersAnOptionalSalaryWithItsPeriod() {
        showForm(PublishUiState())

        composeRule.onNodeWithText("Salario (opcional)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Si no lo completás, se muestra “A convenir”.").assertExists()
        composeRule.onNodeWithText("Por hora").performScrollTo().performClick()

        assertEquals(listOf("period:HOUR"), clicks)
    }

    @Test
    fun anInvalidSalaryIsExplained() {
        showForm(PublishUiState(salaryMin = "mucho", fieldErrors = setOf(PublishField.SALARY)))

        composeRule.onNodeWithText("Revisá el salario: montos en pesos, sin centavos, y el máximo no menor que el mínimo.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun thePublishedOfferShowsItsSalary() {
        composeRule.setContent {
            LaburinTheme {
                PublishedOfferContent(
                    state = PublishedOfferUiState.Content(
                        PublishedOffer("p-1", "Mozo", Category.GASTRONOMY, "Sábados", "[DESCRIPCION]", PublishedOfferStatus.ACTIVE,
                            Instant.EPOCH, salary = Salary(600_000, null, SalaryPeriod.MONTH)),
                        qrLink = "laburin://offer/p-1",
                    ),
                    onShare = {}, onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("$ 600.000 por mes").assertIsDisplayed()
    }
}
