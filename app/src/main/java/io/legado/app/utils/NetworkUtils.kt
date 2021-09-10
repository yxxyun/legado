package io.legado.app.utils

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import splitties.systemservices.connectivityManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URL
import java.util.*
import java.util.regex.Pattern


@Suppress("unused", "MemberVisibilityCanBePrivate")
object NetworkUtils {

    /**
     * 判断是否联网
     */
    @Suppress("DEPRECATION")
    fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            val mWiFiNetworkInfo = connectivityManager.activeNetworkInfo
            if (mWiFiNetworkInfo != null) {
                //移动数据
                return if (mWiFiNetworkInfo.type == ConnectivityManager.TYPE_WIFI) {
                    //WIFI
                    true
                } else mWiFiNetworkInfo.type == ConnectivityManager.TYPE_MOBILE
            }
        } else {
            val network = connectivityManager.activeNetwork
            if (network != null) {
                val nc = connectivityManager.getNetworkCapabilities(network)
                if (nc != null) {
                    //移动数据
                    return if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        //WIFI
                        true
                    } else nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                }
            }
        }
        return false
    }

    private val notNeedEncoding: BitSet by lazy {
        val bitSet = BitSet(256)
        for (i in 'a'.code..'z'.code) {
            bitSet.set(i)
        }
        for (i in 'A'.code..'Z'.code) {
            bitSet.set(i)
        }
        for (i in '0'.code..'9'.code) {
            bitSet.set(i)
        }
        for (char in "+-_.$:()!*@&#,[]") {
            bitSet.set(char.code)
        }
        return@lazy bitSet
    }

    /**
     * 支持JAVA的URLEncoder.encode出来的string做判断。 即: 将' '转成'+'
     * 0-9a-zA-Z保留 <br></br>
     * ! * ' ( ) ; : @ & = + $ , / ? # [ ] 保留
     * 其他字符转成%XX的格式，X是16进制的大写字符，范围是[0-9A-F]
     */
    fun hasUrlEncoded(str: String): Boolean {
        var needEncode = false
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (notNeedEncoding.get(c.code)) {
                i++
                continue
            }
            if (c == '%' && i + 2 < str.length) {
                // 判断是否符合urlEncode规范
                val c1 = str[++i]
                val c2 = str[++i]
                if (isDigit16Char(c1) && isDigit16Char(c2)) {
                    i++
                    continue
                }
            }
            // 其他字符，肯定需要urlEncode
            needEncode = true
            break
        }

        return !needEncode
    }

    /**
     * 判断c是否是16进制的字符
     */
    private fun isDigit16Char(c: Char): Boolean {
        return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
    }

    /**
     * 获取绝对地址
     */
    fun getAbsoluteURL(baseURL: String?, relativePath: String): String {
        if (baseURL.isNullOrEmpty()) return relativePath
        if (relativePath.isAbsUrl()) return relativePath
        var relativeUrl = relativePath
        try {
            val absoluteUrl = URL(baseURL.substringBefore(","))
            val parseUrl = URL(absoluteUrl, relativePath)
            relativeUrl = parseUrl.toString()
            return relativeUrl
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return relativeUrl
    }

    /**
     * 获取绝对地址
     */
    fun getAbsoluteURL(baseURL: URL?, relativePath: String): String {
        if (baseURL == null) return relativePath
        var relativeUrl = relativePath
        try {
            val parseUrl = URL(baseURL, relativePath)
            relativeUrl = parseUrl.toString()
            return relativeUrl
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return relativeUrl
    }

    fun getBaseUrl(url: String?): String? {
        if (url == null || !url.startsWith("http")) return null
        val index = url.indexOf("/", 9)
        return if (index == -1) {
            url
        } else url.substring(0, index)
    }

    fun getSubDomain(url: String?): String {
        val baseUrl = getBaseUrl(url) ?: return ""
        return if (baseUrl.indexOf(".") == baseUrl.lastIndexOf(".")) {
            baseUrl.substring(baseUrl.lastIndexOf("/") + 1)
        } else baseUrl.substring(baseUrl.indexOf(".") + 1)
    }

    /**
     * Get local Ip address.
     */
    fun getLocalIPAddress(): InetAddress? {
        var enumeration: Enumeration<NetworkInterface>? = null
        try {
            enumeration = NetworkInterface.getNetworkInterfaces()
        } catch (e: SocketException) {
            e.printOnDebug()
        }

        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                val nif = enumeration.nextElement()
                val addresses = nif.inetAddresses
                if (addresses != null) {
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && isIPv4Address(address.hostAddress)) {
                            return address
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     * @return True if the input parameter is a valid IPv4 address.
     */
    fun isIPv4Address(input: String): Boolean {
        return IPV4_PATTERN.matcher(input).matches()
    }

    /**
     * Ipv4 address check.
     */
    private val IPV4_PATTERN = Pattern.compile(
        "^(" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
    )

}