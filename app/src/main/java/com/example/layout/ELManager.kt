package com.example.layout

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.runBlocking

/** objectはシングルトンみたいなもん
 * objectだと初期化時の引数がないのでゴリ押し実装
 * classでシングルトンを実装した方がいいかもしれん
 */
object ELManager {
    lateinit var assetManager: android.content.res.AssetManager
    fun isAssetManagerInitialized(): Boolean {
        return ::assetManager.isInitialized
    }

    private val echonetLitePort = 3610
    val TID = listOf(0x09, 0x29.toByte())
    var deviceList: List<ELObject<Number>> = mutableListOf()
        private set // 外部からの変更を許可しない

    /** getDeviseListのタイムアウト時間。外部から変更可能*/
    var timeout = 2000
    val packetList: ArrayDeque<ELPacketData> = ArrayDeque()

    val waitingPacketMap = mutableMapOf<ELPacketData, ELPacketData?>()

    var isReading = false
        private set // 外部からの変更を許可しない

    fun insertDevice(device: ELObject<Number>) {
        val idx =
            deviceList.indexOfFirst { it.ipAddress == device.ipAddress && it.eoj == device.eoj }
        if (idx == -1) {
            deviceList = deviceList.plus(device)
        } else {
            (deviceList as MutableList)[idx] = device
        }
    }

    /**
     * ネットワーク内のデバイスを検索し、deviceListに格納する
     * すでにdeviceListに格納されているデバイスの場合は情報を更新する
     * また、deviceListには自ノードインスタンスリストSも格納される
     * @throws IllegalArgumentException ESV==0x72(getへの応答)でない場合
     * @throws IllegalArgumentException TIDが違う場合
     * @throws IllegalArgumentException パケットが不正な場合
     */
    private fun getDeviceList() {
        if (isReading) {
            // isReadingPacket==trueの時落ちる
            println("isReading==trueのためgetDeviceListを起動できません")
            return
        }

        if (deviceList.isEmpty()) {
            val selfNodeInstanceList =
                ELObject<Number>(
                    0,
                    InetAddress.getByName("224.0.23.0"),
                    listOf(0x0E, 0xF0, 0x01),
                    assetManager
                )
            deviceList = deviceList.plus(selfNodeInstanceList)
            println(deviceList)
        }
        deviceList[0].get("自ノードインスタンスリストS")

        val socket = DatagramSocket(3610)
        socket.soTimeout = 100
        val buf = ByteArray(1024)
        val packet = DatagramPacket(buf, buf.size)

        for (i in 1..timeout / socket.soTimeout) {
            try {
                socket.receive(packet)
                val list =
                    ELFormat.parseSelfNodeInstanceList(
                        ELFormat.parsePacket(
                            packet,
                            TID
                        ), assetManager
                    )
                println("応答を受け取りました:${list}\n")

//                deviceList = deviceList.plus(list)
                list.forEach { insertDevice(it) }
            } catch (_: Exception) { // SocketTimeoutExceptionまたはIllegalArgumentException（jsonがない場合）
//                println("timeout")
            }
        }
        socket.close()

        println("検索終了")
        println("検索結果:${deviceList}")
    }



    /**
     * ネットワーク内のデバイスを検索し、deviceListに格納する
     * すでにdeviceListに格納されているデバイスの場合は情報を更新する
     * また、deviceListには自ノードインスタンスリストSも格納される
     * delayを使いたいためgetDeviceListとは別関数
     * @throws IllegalArgumentException ESV==0x72(getへの応答)でない場合
     * @throws IllegalArgumentException TIDが違う場合
     * @throws IllegalArgumentException パケットが不正な場合
     */
    suspend fun asyncGetDeviceList() {
        withContext(Dispatchers.IO) {
            if (isReading) {
                // isReadingPacket==trueの時落ちる
                println("isReading==trueのためgetDeviceListを起動できません")
                return@withContext
            }

            // isReadingPacket=falseしてから最大100ミリ秒ほどはcloseされていない可能性がある
            // 処理時間に余裕を持たせて150ミリ秒待つ
            delay(150)

            if (deviceList.isEmpty()) {
                val selfNodeInstanceList =
                    ELObject<Number>(
                        0,
                        InetAddress.getByName("224.0.23.0"),
                        listOf(0x0E, 0xF0, 0x01),
                        assetManager
                    )
                deviceList = deviceList.plus(selfNodeInstanceList)
                println(deviceList)
            }
            deviceList[0].get("自ノードインスタンスリストS")

            val socket = DatagramSocket(3610)
            socket.soTimeout = 100
            val buf = ByteArray(1024)
            val packet = DatagramPacket(buf, buf.size)

            for (i in 1..timeout / socket.soTimeout) {
                try {
                    socket.receive(packet)
                    val list =
                        ELFormat.parseSelfNodeInstanceList(
                            ELFormat.parsePacket(
                                packet,
                                TID
                            ), assetManager
                        )
                    println("応答を受け取りました:${list}\n")

//                deviceList = deviceList.plus(list)
                    list.forEach { insertDevice(it) }
                } catch (_: Exception) { // SocketTimeoutExceptionまたはIllegalArgumentException（jsonがない場合）
//                println("timeout")
                }
            }
            socket.close()

            println("検索終了")
            println("検索結果:${deviceList}")
        }
    }

    /**
     * waitingPacketMapに対応するパケットがあったら、それを格納する
     * すでに受信済みのパケットは考慮しない
     */
    private fun readPacket() {
        this.isReading = true
        val socket = DatagramSocket(echonetLitePort)
        socket.soTimeout = 100
        val buf = ByteArray(1024)
        val packet = DatagramPacket(buf, buf.size)

        while (isReading) {
            try {
                socket.receive(packet)

                val response = ELFormat.parsePacket(packet, TID)
//                println(response)
                packetList.add(response)

                for ((key, value) in waitingPacketMap) {
                    if (value != null) continue
                    if (checkPacket(response, key)) {
                        println("got expected packet!")
                        waitingPacketMap[key] = response
                    }
                }
            } catch (_: SocketTimeoutException) {
            } catch (_: IllegalArgumentException) {
                println("TIDが違います")
            }
        }
        socket.close()
    }

    suspend fun asyncReadPacket() {
        withContext(Dispatchers.IO) {
            if (isReading) return@withContext
            readPacket()
        }
    }

    /**
     * readPacketを停止する
     * 開始はasyncReadPacketで行う
     */
    fun stopReadPacket() {
        this.isReading = false
    }

    /**
     * パケットの内容を確認する
     * tidの一致, seojとdeojの一致, esvの一致を確認する。
     * @param receive 受信したパケット
     * @param expect 期待するパケット
     * @return 一致すればtrue
     * @throws IllegalArgumentException esvが0x61または0x71でない場合
     */
    fun checkPacket(receive: ELPacketData, expect: ELPacketData): Boolean {
        var returnValue: ELPacketData? = null
        val tid = expect.tid
        if (expect.esv != 0x61.toByte() && expect.esv != 0x62.toByte()) {
            throw IllegalArgumentException(
                "Echonet: esv is not 0x61 or 0x71 but ${
                    "%02X".format(
                        expect.esv
                    )
                }"
            )
        }
        val esv = if (expect.esv == 0x61.toByte()) 0x71.toByte() else 0x72.toByte()


        if (receive.tid != tid) {
            println("TIDが違います")
            return false
        }
        if (receive.seoj != expect.deoj) {
            println("SEOJは${receive.seoj}ですが、${expect.deoj}が期待されています")
            return false
        }
        if (receive.deoj != expect.seoj) {
            println("DEOJは${receive.deoj}ですが、${expect.seoj}が期待されています")
            return false
        }
        if (receive.esv != esv) {
            println("ESVが違います")
            return false
        }
        return true
    }

    /**
     * 引数のパケットに対する返答を待つ。こちらから送信したパケットを引数にし、それへの返答を受け取ることを想定。
     * tidの一致, seojとdeojの一致, esvの一致を確認する。
     * @param data: ELPacketData
     * @param timeout: Int
     * @return ELPacketData?
     * @throws IllegalArgumentException esvが0x61または0x71でない場合
     */
    fun waitPacket(data: ELPacketData, timeout: Int = 2000): ELPacketData? {
        waitingPacketMap[data] = null
        for (i in 1..timeout / 100) {
            Thread.sleep(100)
            if (waitingPacketMap[data] != null) break
        }
        return waitingPacketMap[data]
    }

    /**
     * Coroutine版のwaitPacket
     * suspend関数のため、呼び出すとlaunch内でのみ処理が止まる。
     * @param data: ELPacketData
     * @param timeout: Int
     * @return ELPacketData?
     */
    suspend fun asyncWaitPacket(
        data: ELPacketData,
        timeout: Int = 2000,
    ): ELPacketData? {
        return withContext(Dispatchers.IO) {
            waitPacket(data, timeout)
        }
    }
}

fun main() {
    // runBlocking はCoroutineBuilderの一つで、None-Coroutineの世界からCoroutineの世界へのエントリポイント
    // None-Coroutineの世界でlaunchなどのCoroutineBuilderは使えない
//    runBlocking {
//        val task = launch {
//            println("end")
//        }
//        println("Hello, World!")
//        task.join() // taskが終わるまで待つ
//        println("Goodbye, World!")
//    } // Hello, World! -> end -> Goodbye, World!


//    val echonet = ELManager()
//    val monoLite = ELObject(InetAddress.getByName("192.168.2.50"), listOf(0x02, 0x91, 0x01))
//    println("here")
//    println(echonet.waitPacket(monoLite.setC("power", "off")))

//    val echonet = ELManager()
//    runBlocking {
//        val job = launch {
//            echonet.asyncGetDeviceList()
//            echonet.deviceList.forEach {
//                println(it.asyncGet("power"))
//            }
//            println("len:"+echonet.deviceList.size)
//        }
//    }


//    echonet.getDeviceList()
//    echonet.deviceList.forEach {
//        println(it.get("power"))
//    }
//    println("len:" + echonet.deviceList.size)

//    runBlocking {
//        echonet.asyncGetDeviceList()
//        echonet.deviceList.forEach { it ->
//            val ret = it.asyncGet("power")
//            println("ret:"+ret)
//        }
//        println("len:"+echonet.deviceList.size)
//    }

//    val node = ELObject(InetAddress.getByName("224.0.23.0"), listOf(0x0E, 0xF0, 0x01))
//    println(echonet.waitPacket(node.get("selfNodeInstanceList")))

}

// コルーチンに関する資料
// https://zenn.dev/bookstore/articles/kotlin-coroutine-getting-started
// https://qiita.com/kawmra/items/d024f9ab32ffe0604d39
// https://qiita.com/naoi/items/9892db4cec2e9c0f6114
