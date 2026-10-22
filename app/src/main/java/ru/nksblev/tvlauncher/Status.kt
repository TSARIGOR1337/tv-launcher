package ru.nksblev.tvlauncher

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.math.roundToInt

object Status {

    @Suppress("DEPRECATION")
    private fun networks(ctx: Context): List<Pair<Network, NetworkCapabilities>> {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return emptyList()
        return cm.allNetworks.mapNotNull { n -> cm.getNetworkCapabilities(n)?.let { n to it } }
    }

    fun vpnOn(ctx: Context): Boolean =
        networks(ctx).any { it.second.hasTransport(NetworkCapabilities.TRANSPORT_VPN) }

    private fun wifiNetwork(ctx: Context): Network? =
        networks(ctx).firstOrNull {
            (it.second.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                it.second.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) &&
                !it.second.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        }?.first

    /** Tailscale exposes explicit broadcast actions to connect / disconnect. */
    fun setTailscale(ctx: Context, on: Boolean) {
        val action = if (on) "com.tailscale.ipn.CONNECT_VPN" else "com.tailscale.ipn.DISCONNECT_VPN"
        try {
            ctx.sendBroadcast(Intent(action).setClassName("com.tailscale.ipn", "com.tailscale.ipn.IPNReceiver"))
        } catch (e: Exception) {
            // ignore
        }
    }

    /** PlayStation Device Discovery Protocol (PS5 listens on UDP 9302). */
    suspend fun ps5(ctx: Context): Ps5State = withContext(Dispatchers.IO) {
        try {
            DatagramSocket().use { sock ->
                sock.broadcast = true
                sock.soTimeout = 1500
                wifiNetwork(ctx)?.let { net -> runCatching { net.bindSocket(sock) } }
                val msg = "SRCH * HTTP/1.1\ndevice-discovery-protocol-version:00030010\n".toByteArray()
                sock.send(DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), 9302))
                val buf = ByteArray(2048)
                val packet = DatagramPacket(buf, buf.size)
                sock.receive(packet)
                val text = String(buf, 0, packet.length)
                when {
                    text.contains(" 200 ") -> Ps5State.ON
                    text.contains(" 620 ") -> Ps5State.STANDBY
                    else -> Ps5State.UNKNOWN
                }
            }
        } catch (e: SocketTimeoutException) {
            Ps5State.OFFLINE
        } catch (e: Exception) {
            Ps5State.UNKNOWN
        }
    }

    /** Open-Meteo, no API key. Saint Petersburg. */
    fun weather(): Weather {
        val j = JSONObject(
            Net.request(
                "https://api.open-meteo.com/v1/forecast?latitude=59.94&longitude=30.31" +
                    "&current=temperature_2m,weather_code&timezone=Europe%2FMoscow"
            )
        )
        val c = j.getJSONObject("current")
        return Weather(c.getDouble("temperature_2m").roundToInt(), c.getInt("weather_code"))
    }
}
