package com.example.layout

import java.net.DatagramPacket
import com.example.layout.ELManager

/**
 * パケットのパースなど、Echonet電文に関するユーティリティ
 */
class ELFormat {
    companion object {
        /**
         * esv, epc, edtからEchonetLiteのパケットを作成する
         * @param data EchonetLiteのデータ
         * @return EchonetLiteのパケット 例：[0x10, 0x81, 0x00, 0x0A, 0x05, 0xFF, 0x01, 0x02, 0x90, 0x01, 0x60, 0x01, 0x80, 0x01, 0x30]
         */
        fun makePacket(data: ELPacketData): ByteArray {
            val payload = mutableListOf(0x10, 0x81.toByte())
            payload.addAll(data.tid)
            payload.addAll(data.seoj)
            payload.addAll(data.deoj)
            payload.add(data.esv)
            payload.add(data.epc.size.toByte())
            payload.addAll(data.epc)
            // ノードプロファイルの場合などedtを送らないことがある
            data.edt?.let {
                payload.add(it.size.toByte())
                payload.addAll(it)
            } ?: run { payload.add(0) }
            return payload.map { it }.toByteArray()
        }

        /**
         * EchonetLiteのパケットを解析し、ELPacketDataに整形して返す
         * Tidの照合も可能
         * @param packet EchonetLiteのDatagramPacket
         * @param collectTid Tidの照合を行う場合は指定する
         * @return EchonetLiteのデータ
         * @throws IllegalArgumentException パケットが短すぎる、EchonetLiteでない、Tidが一致しない場合
         */
        fun parsePacket(
            packet: DatagramPacket,
            collectTid: List<Byte>? = null
        ): ELPacketData {
            val data = packet.data
            if (data.size < 12) {
                throw IllegalArgumentException("Echonet: packet is too short")
            }
            if (data[0] != 0x10.toByte() || data[1] != 0x81.toByte()) {
                throw IllegalArgumentException("Echonet: packet is not EchonetLite")
            }
            if (collectTid != null) {
                if (collectTid.size != 2) {
                    throw IllegalArgumentException("Echonet: tid must be 2 bytes")
                }
                if (data[2] != collectTid[0] || data[3] != collectTid[1]) {
                    throw IllegalArgumentException("Echonet: tid is not match")
                }
            }

            val tid = data.slice(2 until 4).map { it }

            val seoj = data.slice(4 until 4 + 3).map { it }
            val deoj = data.slice(7 until 7 + 3).map { it }

            val esv = data[10]

            val epcSize = data[11]
            val epc = data.slice(12 until 12 + epcSize).map { it }

            val edtSizeIndex = 12 + epcSize
            val edtSize = data[edtSizeIndex]

            val edt = if (0 < edtSize) {
                data.slice(edtSizeIndex + 1 until edtSizeIndex + 1 + edtSize).map { it }
            } else {
                null
            }
            return ELPacketData(packet.address, tid, seoj, deoj, esv, epc, edt)
        }

        /**
         * SelfNodeInstanceListを解析し、ELObjectのListにして返す
         * @param data EchonetLiteのデータ
         * @param assetManager アセットマネージャ
         * @return EchonetLiteのオブジェクトのリスト
         * @throws IllegalArgumentException ESV==0x72(getへの応答)でない場合
         */
        fun parseSelfNodeInstanceList(
            data: ELPacketData,
            assetManager: android.content.res.AssetManager
        ): List<ELObject<Number>> {
            if (data.esv != 0x72.toByte()) {
                throw IllegalArgumentException("Echonet: esv is not 0x72")
            }
            val edt = data.edt ?: return listOf()
            val num = edt[0].toInt()
            val list = mutableListOf<ELObject<Number>>()
            for (i in 1..num) {
                list.add(
                    ELObject<Number>(
                        ELManager.deviceList.size,
                        data.ipAddress,
                        edt.slice(1 + (i - 1) * 3 until 1 + i * 3), assetManager
                    )
                )
            }
            return list
        }

        /**
         * SelfNodeInstanceListを解析し、ELObjectのListにして返す
         * TIDの照合も可能
         * @param packet EchonetLiteのDatagramPacket
         * @param collectTid Tidの照合を行う場合は指定する
         * @param assetManager アセットマネージャ
         * @return EchonetLiteのオブジェクトのリスト
         * @throws IllegalArgumentException ESV==0x72(getへの応答)でない場合
         */
        fun parseSelfNodeInstanceList(
            packet: DatagramPacket,
            collectTid: List<Byte>?,
            assetManager: android.content.res.AssetManager
        ): List<ELObject<Number>> {
            return parseSelfNodeInstanceList(parsePacket(packet, collectTid), assetManager)
        }
    }
}
