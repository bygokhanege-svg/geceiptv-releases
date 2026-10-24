/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import app.opentv.R

/**
 * Manages device hardware profiling and automatic TV stabilization.
 * Detects RAM, CPU cores, SoC architecture, and Hardware Codecs to configure
 * ideal buffer durations, preview behaviors, and multi-screen thresholds.
 */
object DeviceOptimizationManager {

    data class DeviceHardwareInfo(
        val brand: String,
        val model: String,
        val socOrHardware: String,
        val totalRamMb: Int,
        val availableRamMb: Int,
        val cpuCores: Int,
        val isLowRamDevice: Boolean,
        val androidVersion: String,
        val hasHardwareHevc: Boolean,
    ) {
        val formattedRam: String get() = "%.1f GB".format(totalRamMb / 1024.0)
    }

    enum class HardwareProfile(
        val titleRes: Int,
        val descRes: Int,
        val minBufferMs: Int,
        val maxBufferMs: Int,
        val playbackStartBufferMs: Int,
        val previewVideoEnabled: Boolean,
        val maxMultiViewSlots: Int,
        val imageCacheMb: Int,
        val hwTunneling: Boolean,
    ) {
        LOW_END(
            titleRes = R.string.settings_profile_low,
            descRes = R.string.settings_profile_low_desc,
            minBufferMs = 4_000,
            maxBufferMs = 8_000,
            playbackStartBufferMs = 800,
            previewVideoEnabled = false,
            maxMultiViewSlots = 2,
            imageCacheMb = 25,
            hwTunneling = false,
        ),
        BALANCED(
            titleRes = R.string.settings_profile_balanced,
            descRes = R.string.settings_profile_balanced_desc,
            minBufferMs = 8_000,
            maxBufferMs = 15_000,
            playbackStartBufferMs = 1_200,
            previewVideoEnabled = true,
            maxMultiViewSlots = 3,
            imageCacheMb = 60,
            hwTunneling = false,
        ),
        ULTRA(
            titleRes = R.string.settings_profile_ultra,
            descRes = R.string.settings_profile_ultra_desc,
            minBufferMs = 15_000,
            maxBufferMs = 30_000,
            playbackStartBufferMs = 2_000,
            previewVideoEnabled = true,
            maxMultiViewSlots = 4,
            imageCacheMb = 120,
            hwTunneling = true,
        ),
    }

    /**
     * Inspects the host Android TV / Box hardware specifications via system APIs.
     */
    fun analyzeHardware(context: Context): DeviceHardwareInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo().apply {
            activityManager?.getMemoryInfo(this)
        }

        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availRamMb = (memInfo.availMem / (1024 * 1024)).toInt()
        val isLowRam = activityManager?.isLowRamDevice ?: (totalRamMb < 1800)
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        val brand = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val model = Build.MODEL
        val soc = if (Build.VERSION.SDK_INT >= 31) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

        val hasHevc = runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.contains("hevc", ignoreCase = true) }
            }
        }.getOrDefault(true)

        return DeviceHardwareInfo(
            brand = brand,
            model = model,
            socOrHardware = soc,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            cpuCores = cpuCores,
            isLowRamDevice = isLowRam,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            hasHardwareHevc = hasHevc,
        )
    }

    /**
     * Recommends the optimal stability profile tailored to this specific TV.
     */
    fun recommendProfile(info: DeviceHardwareInfo): HardwareProfile {
        val isNvidiaOrShield = info.brand.contains("nvidia", ignoreCase = true) || info.model.contains("shield", ignoreCase = true)

        return when {
            // Devices with <= 1.5 GB RAM or low RAM flags (Mi Stick 1st Gen, entry TV sticks)
            info.isLowRamDevice || info.totalRamMb < 1600 || info.cpuCores <= 2 -> HardwareProfile.LOW_END

            // High-end hardware with > 2.8 GB RAM or Nvidia Shield or 8 powerful cores
            isNvidiaOrShield || (info.totalRamMb > 2800 && info.cpuCores >= 4) -> HardwareProfile.ULTRA

            // Standard mainstream Android TV / Box (2 GB RAM, e.g. Mi Box S, Chromecast 4K)
            else -> HardwareProfile.BALANCED
        }
    }

    /**
     * Applies the selected profile to [AppSettings] to immediately stabilize playback and UI.
     */
    fun applyProfile(context: Context, profile: HardwareProfile) {
        val settings = AppSettings.get(context)
        settings.setHardwareProfile(profile.name)
        settings.setStreamBufferDurations(
            minMs = profile.minBufferMs,
            maxMs = profile.maxBufferMs,
            startMs = profile.playbackStartBufferMs,
        )
        settings.setGuidePreviewVideo(profile.previewVideoEnabled)
        settings.setMaxMultiViewSlots(profile.maxMultiViewSlots)
        settings.setTunnelingEnabled(profile.hwTunneling)
    }
}
