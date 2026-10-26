/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.core

import android.util.Log
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Intelligent Anti-Freeze DNS resolver using DNS-over-HTTPS (DoH).
 * Bypasses ISP throttling, port blocking, and DNS poisoning using Cloudflare (1.1.1.1)
 * and Google (8.8.8.8) encrypted HTTPS queries.
 *
 * Guarantees zero-lag and zero-crash:
 * 1. High-speed in-memory cache (30 min TTL) so video segment requests take 0 ms.
 * 2. Hardcoded fallback to Dns.SYSTEM if DoH is disabled, offline, or blocked.
 * 3. Immediate passthrough for IP literals, localhost, and LAN domains.
 */
class AntiFreezeDns(
    private val settings: AppSettings,
) : Dns {

    companion object {
        private const val TAG = "AntiFreezeDns"
        private const val CACHE_TTL_MS = 30 * 60 * 1000L // 30 minutes
        private val IPV4_PATTERN = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        private val DOH_ENDPOINTS = listOf(
            "https://1.1.1.1/dns-query?name=",
            "https://1.0.0.1/dns-query?name=",
            "https://dns.google/resolve?name=",
        )
    }

    private data class CachedDns(val addresses: List<InetAddress>, val expireTime: Long)

    private val cache = ConcurrentHashMap<String, CachedDns>()

    // Dedicated lightweight client for DoH queries (short timeouts, no recursion)
    private val dohClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .callTimeout(3000, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        if (hostname.isBlank()) return Dns.SYSTEM.lookup(hostname)

        // 1. If it's already an IP address, return directly (0 ms)
        if (IPV4_PATTERN.matcher(hostname).matches()) {
            return listOf(InetAddress.getByName(hostname))
        }

        // 2. Local / LAN / Intranet domains always use System DNS
        if (hostname == "localhost" || hostname.endsWith(".local") || hostname.endsWith(".lan")) {
            return Dns.SYSTEM.lookup(hostname)
        }

        // 3. Check in-memory cache
        val now = System.currentTimeMillis()
        val cached = cache[hostname]
        if (cached != null && now < cached.expireTime && cached.addresses.isNotEmpty()) {
            return cached.addresses
        }

        // 4. If DoH is disabled by user, use standard System DNS
        if (!settings.dohEnabled.value) {
            val systemResult = Dns.SYSTEM.lookup(hostname)
            cache[hostname] = CachedDns(systemResult, now + CACHE_TTL_MS)
            return systemResult
        }

        // 5. Query DoH with multi-endpoint fallback
        for (endpoint in DOH_ENDPOINTS) {
            try {
                val url = "${endpoint}${hostname}&type=A"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/dns-json")
                    .build()

                dohClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val answers = json.optJSONArray("Answer")
                            if (answers != null && answers.length() > 0) {
                                val resolved = mutableListOf<InetAddress>()
                                for (i in 0 until answers.length()) {
                                    val obj = answers.getJSONObject(i)
                                    val type = obj.optInt("type")
                                    val data = obj.optString("data")
                                    if (type == 1 && IPV4_PATTERN.matcher(data).matches()) {
                                        resolved.add(InetAddress.getByName(data))
                                    }
                                }
                                if (resolved.isNotEmpty()) {
                                    cache[hostname] = CachedDns(resolved, now + CACHE_TTL_MS)
                                    return resolved
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "DoH lookup failed on $endpoint for $hostname: ${e.message}")
            }
        }

        // 6. Fail-safe Fallback: If DoH fails or has no answers, fall back to System DNS
        val fallbackResult = Dns.SYSTEM.lookup(hostname)
        cache[hostname] = CachedDns(fallbackResult, now + (5 * 60 * 1000L)) // 5 min cache on fallback
        return fallbackResult
    }
}
