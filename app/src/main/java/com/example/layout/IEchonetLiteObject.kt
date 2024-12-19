package com.example.layout

import java.net.DatagramPacket

interface IEchonetLiteObject {
    /**
     * Echonetのパケットを作成し、送信する
     * @param elPacket
     * @return 送信したパケット（送信失敗時null）
     */
    fun sendEchonetPacket(elPacket: EchonetLitePacketData): DatagramPacket?

    /**
     * 任意のEPCのEDTをセットする 応答を受け取らない
     * @param epc EPCの値
     * @param edt EDTの値
     */
    fun setI(epc: String, edt: String): EchonetLitePacketData?

    /**
     * 任意のEPCのEDTをセットする 応答を受け取る
     * @param epc EPCの値
     * @param edt EDTの値
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun setC(epc: String, edt: String): EchonetLitePacketData?

    /**
     * 任意のEPCのEDTを知る
     * @param epc EPCの値
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun get(epc: String): EchonetLitePacketData?

    suspend fun asyncSetI(epc: String, edt: String): EchonetLitePacketData?
    suspend fun asyncSetC(epc: String, edt: String): EchonetLitePacketData?
    suspend fun asyncGet(epc: String): EchonetLitePacketData?
}
