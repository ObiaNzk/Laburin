package com.example.laburin.ui.navigation

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.laburin.MainActivity
import com.example.laburin.domain.repository.AuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject

/** RF-03 end to end: a shop without an account publishes, registers on the way, and gets its QR. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PublishFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION))
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        hiltRule.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context, Configuration.Builder().setExecutor(SynchronousExecutor()).setWorkerFactory(workerFactory).build(),
        )
        runBlocking { auth.logout() }
    }

    @Test
    fun aShopWithoutAccountRegistersOnTheWayAndPublishesWithAQr() {
        composeRule.waitFor("¿Buscás empleados? Publicá gratis")
        composeRule.tap("¿Buscás empleados? Publicá gratis")
        composeRule.waitFor("Nueva búsqueda")

        composeRule.typeInField(0, "Bachero/a")
        composeRule.tap("Elegí un rubro")
        composeRule.tap("Gastronomía")
        composeRule.typeInField(1, "Viernes y sábados, 20 a 2 h")
        composeRule.typeInField(2, "Lavado de vajilla y limpieza de cocina")
        composeRule.tap("Publicar búsqueda")

        composeRule.waitFor("Registrá tu comercio")
        composeRule.typeInField(0, "Bodegón E2E")
        composeRule.tap("Elegí un rubro")
        composeRule.tap("Gastronomía")
        composeRule.typeInField(1, "Martes a domingo")
        composeRule.typeInField(2, "[DIRECCION]")
        composeRule.typeInField(3, "bodegon.e2e@demo.laburin")
        composeRule.typeInField(4, "12345678")
        composeRule.tap("Crear cuenta de comercio")

        composeRule.waitFor("Nueva búsqueda")
        composeRule.onNodeWithText("Bachero/a").assertExists() // The form was kept during sign-up.
        composeRule.tap("Publicar búsqueda")

        composeRule.waitFor("Pendiente de publicación")
        composeRule.onNode(hasContentDescription("Código QR de la búsqueda")).assertExists()

        connectionIsBack(context)
        composeRule.waitFor("Publicada")
    }
}
