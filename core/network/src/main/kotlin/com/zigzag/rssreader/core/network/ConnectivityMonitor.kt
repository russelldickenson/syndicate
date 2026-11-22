package com.zigzag.rssreader.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import com.zigzag.rssreader.core.domain.repository.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class ConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {
    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        fun currentStatus(): Boolean {
            if (manager == null) return true
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val network = manager.activeNetwork
                    val capabilities = network?.let(manager::getNetworkCapabilities)
                    capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                        )
                } else {
                    @Suppress("DEPRECATION")
                    manager.activeNetworkInfo?.isConnected == true
                }
            } catch (_: SecurityException) {
                true
            }
        }

        trySend(currentStatus())

        val callback = if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(true)
                }

                override fun onLost(network: Network) {
                    trySend(currentStatus())
                }

                override fun onUnavailable() {
                    trySend(false)
                }
            }.also { networkCallback ->
                try {
                    manager.registerDefaultNetworkCallback(networkCallback)
                } catch (_: SecurityException) {
                    // Permission missing; keep the initial best-effort value.
                }
            }
        } else {
            null
        }

        awaitClose {
            if (manager != null && callback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    manager.unregisterNetworkCallback(callback)
                } catch (_: Exception) {
                    // Already unregistered.
                }
            }
        }
    }.distinctUntilChanged()
}
