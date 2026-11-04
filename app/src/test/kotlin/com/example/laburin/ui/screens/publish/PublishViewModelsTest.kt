package com.example.laburin.ui.screens.publish

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.model.ApplicantStatus
import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.publish.ReviewApplicantUseCase
import com.example.laburin.domain.publish.CloseOfferUseCase
import com.example.laburin.domain.publish.EditOfferUseCase
import com.example.laburin.ui.navigation.EditOfferDestination
import com.example.laburin.fakes.FakeApplicantsRepository
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.domain.publish.OfferLink
import com.example.laburin.domain.publish.PublishOfferUseCase
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.FakeLocationProvider
import com.example.laburin.fakes.FakeMyOffersRepository
import com.example.laburin.fakes.FakeSyncScheduler
import com.example.laburin.fakes.MainDispatcherRule
import com.example.laburin.fakes.testPublishedOffer
import com.example.laburin.ui.navigation.PublishedOfferDestination
import com.example.laburin.ui.screens.map.MapUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PublishViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // --- Publish form ---

    private val auth = FakeAuthRepository(BUSINESS_SESSION)
    private val offers = FakeMyOffersRepository()
    private val scheduler = FakeSyncScheduler()
    private fun publishVm() = PublishViewModel(PublishOfferUseCase(auth, offers, scheduler))

    private fun PublishViewModel.fill() {
        onTitleChange("Mozo")
        onCategoryChange(Category.GASTRONOMY)
        onScheduleChange("Sábados")
        onDescriptionChange("Atención en salón")
    }

    @Test
    fun `a complete form is published and opens the published offer`() = runTest {
        val vm = publishVm()
        vm.fill()

        vm.effects.test {
            vm.onPublish()
            assertEquals(PublishEffect.Published(offers.published.single().id), awaitItem())
        }
    }

    @Test
    fun `missing fields are marked`() = runTest {
        val vm = publishVm()

        vm.onPublish()

        assertEquals(PublishField.entries.toSet(), vm.uiState.value.fieldErrors)
        assertTrue(offers.published.isEmpty())
    }

    @Test
    fun `without a business account it asks to register the business and keeps the form`() = runTest {
        auth.session.value = null
        val vm = publishVm()
        vm.fill()

        vm.effects.test {
            vm.onPublish()
            assertEquals(PublishEffect.NeedsBusinessAccount, awaitItem())
        }
        assertEquals("Mozo", vm.uiState.value.title)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val vm = publishVm()
        vm.onPublish()

        vm.onTitleChange("Mozo")

        assertEquals(PublishField.entries.toSet() - PublishField.TITLE, vm.uiState.value.fieldErrors)
    }

    // --- Business sign-up ---

    private val location = FakeLocationProvider(LocationResult.Located(GeoLocation(-34.61, -58.39)))
    private fun businessVm() = BusinessSignUpViewModel(auth, location)

    private fun BusinessSignUpViewModel.fill() {
        onNameChange("Taller Nuevo")
        onCategoryChange(Category.MECHANICS)
        onScheduleChange("9 a 18")
        onAddressChange("[DIRECCION]")
        onEmailChange("taller@demo.laburin")
        onPasswordChange("12345678")
    }

    @Test
    fun `registering the business uses the current location and continues`() = runTest {
        auth.session.value = null
        val vm = businessVm()
        vm.fill()

        vm.effects.test {
            vm.onSubmit()
            assertEquals(BusinessSignUpEffect.SignedUp, awaitItem())
        }
        assertEquals(GeoLocation(-34.61, -58.39), auth.lastBusinessLocation)
    }

    @Test
    fun `without a location the business is placed at the default center`() = runTest {
        location.result = LocationResult.Unavailable
        val vm = businessVm()
        vm.fill()

        vm.onSubmit()

        assertEquals(MapUiState.DEFAULT_CENTER, auth.lastBusinessLocation)
    }

    @Test
    fun `invalid business data is marked and nothing is sent`() = runTest {
        val vm = businessVm()

        vm.onSubmit()

        assertEquals(BusinessSignUpField.entries.toSet(), vm.uiState.value.fieldErrors)
        assertTrue(auth.calls.isEmpty())
    }

    // --- Published offer ---

    private val applicants = FakeApplicantsRepository()
    private fun publishedVm() = PublishedOfferViewModel(
        SavedStateHandle(mapOf(PublishedOfferDestination.ARG_OFFER_ID to "p-1")), offers, applicants,
        ReviewApplicantUseCase(applicants, scheduler), CloseOfferUseCase(offers, scheduler),
    )

    @Test
    fun `closing the offer queues the close`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))
        val vm = publishedVm()

        vm.onCloseOffer()

        assertEquals(listOf("p-1"), offers.closes)
        assertEquals(1, scheduler.scheduled)
    }

    // --- Edit offer ---

    private fun editVm() = EditOfferViewModel(
        SavedStateHandle(mapOf(EditOfferDestination.ARG_OFFER_ID to "p-1")), offers, EditOfferUseCase(auth, offers, scheduler),
    )

    @Test
    fun `the edit form opens filled with the offer's current content`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", title = "Mozo", status = PublishedOfferStatus.ACTIVE))

        val state = editVm().uiState.value

        assertEquals(false, state.isLoading)
        assertEquals(listOf("Mozo", "Sábados", "[DESCRIPCION]"), listOf(state.title, state.schedule, state.description))
        assertEquals(Category.GASTRONOMY, state.category)
    }

    @Test
    fun `saving valid changes queues them and goes back`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))
        val vm = editVm()
        vm.onTitleChange("Mozo/a")

        vm.effects.test {
            vm.onSave()
            assertEquals(EditOfferEffect.Saved, awaitItem())
        }
        assertEquals("Mozo/a", offers.edits.single().second.title)
    }

    @Test
    fun `saving incomplete changes marks the missing fields`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))
        val vm = editVm()
        vm.onTitleChange(" ")

        vm.onSave()

        assertEquals(setOf(PublishField.TITLE), vm.uiState.value.fieldErrors)
        assertTrue(offers.edits.isEmpty())
    }

    @Test
    fun `a closed offer can no longer be edited`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.CLOSED))
        val vm = editVm()

        vm.onSave()

        assertEquals(EditOfferError.Closed, vm.uiState.value.error)
    }

    @Test
    fun `an unknown offer shows that it was not found`() = runTest {
        assertEquals(EditOfferError.NotFound, editVm().uiState.value.error)
    }

    @Test
    fun `the published offer lists its applicants and refreshes them on open`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))
        applicants.store.value = listOf(Applicant("a-1", "[NOMBRE]", "cv.pdf", ApplicantStatus.PENDING_REVIEW, null))
        val vm = publishedVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(applicants.store.value, (vm.uiState.value as PublishedOfferUiState.Content).applicants)
        assertEquals(listOf("p-1"), applicants.refreshCalls)
    }

    @Test
    fun `approving and rejecting queue the decision`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1"))
        val vm = publishedVm()

        vm.onApprove("a-1")
        vm.onReject("a-2")

        assertEquals(listOf("a-1:true", "a-2:false"), applicants.reviews)
        assertEquals(2, scheduler.scheduled)
    }

    @Test
    fun `the published offer shows its status and QR link`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))
        val vm = publishedVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val state = vm.uiState.value as PublishedOfferUiState.Content
        assertEquals(PublishedOfferStatus.ACTIVE, state.offer.status)
        assertEquals(OfferLink.build("p-1"), state.qrLink)
    }

    @Test
    fun `sharing offers the link with the job title`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", title = "Mozo"))
        val vm = publishedVm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.effects.test {
            vm.onShare()
            assertEquals(PublishedOfferEffect.Share(title = "Mozo", link = OfferLink.build("p-1")), awaitItem())
        }
    }
}
