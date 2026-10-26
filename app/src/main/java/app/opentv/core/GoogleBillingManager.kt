/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Prepares the Google Play In-App Billing architecture for seamless transition
 * when publishing to Google Play Store (TiviMate model).
 *
 * Sku IDs configured for Google Play Console:
 * - gece_premium_1year: 1-Year Recurring Subscription / In-App Purchase
 * - gece_premium_lifetime: Lifetime One-Time Purchase
 */
class GoogleBillingManager private constructor(
    private val context: Context,
    private val licenseManager: LicenseManager,
) {

    /** Set to true once Google Play Console developer account & in-app products are approved */
    var isGooglePlayStoreActive: Boolean = false

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseStatus>(PurchaseStatus.Idle)
    val purchaseState: StateFlow<PurchaseStatus> = _purchaseState.asStateFlow()

    sealed interface PurchaseStatus {
        data object Idle : PurchaseStatus
        data object Processing : PurchaseStatus
        data class Success(val productId: String) : PurchaseStatus
        data class Error(val message: String) : PurchaseStatus
    }

    init {
        // Architecture initialization hook
        _isAvailable.value = isGooglePlayStoreActive
    }

    /**
     * Initiates the Google Play In-App Purchase flow.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        if (!isGooglePlayStoreActive) {
            _purchaseState.value = PurchaseStatus.Error(
                "Google Play ile ödeme altyapısı şu anda beklemede. Lütfen QR kod, Telegram (@geceiptv) veya e-posta (geceiptv@gmail.com) üzerinden aktive ediniz.",
            )
            return
        }

        _purchaseState.value = PurchaseStatus.Processing
        // Google Play BillingClient flow hook
        Log.i(TAG, "Requesting purchase for: $productId")
    }

    /**
     * Handles successful Google Play purchase completion and grants license.
     */
    fun onPurchaseSuccessful(productId: String, purchaseToken: String) {
        val isLifetime = productId == SKU_PREMIUM_LIFETIME
        val expiryMillis = if (isLifetime) 0L else (System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000L)

        licenseManager.setPremium(isLifetime = isLifetime, expiryMillis = expiryMillis)
        _purchaseState.value = PurchaseStatus.Success(productId)
        Log.i(TAG, "Granted premium license for product $productId with token $purchaseToken")
    }

    companion object {
        private const val TAG = "GoogleBillingManager"

        const val SKU_PREMIUM_1YEAR = "gece_premium_1year"
        const val SKU_PREMIUM_LIFETIME = "gece_premium_lifetime"

        @Volatile
        private var instance: GoogleBillingManager? = null

        fun get(context: Context, licenseManager: LicenseManager): GoogleBillingManager =
            instance ?: synchronized(this) {
                instance ?: GoogleBillingManager(context.applicationContext, licenseManager).also { instance = it }
            }
    }
}
