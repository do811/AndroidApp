package com.example.layout

import java.net.DatagramPacket

interface IELObject {
    /**
     * Echonetのパケットを作成し、送信する
     * @param elPacket
     * @return 送信したパケット（送信失敗時null）
     */
    fun sendEchonetPacket(elPacket: ELPacketData): DatagramPacket?

    /**
     * 任意のEPCのEDTをセットする 応答を受け取らない
     * @param epc EPCの値
     * @param edt EDTの値
     */
    fun setI(epc: String, edt: String): ELPacketData?

    /**
     * 任意のEPCのEDTをセットする 応答を受け取る
     * @param epc EPCの値
     * @param edt EDTの値
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun setC(epc: String, edt: String): ELPacketData?

    /**
     * 任意のEPCのEDTを知る
     * @param epc EPCの値
     * @return 応答（今のところ受け取ったパケットそのまま）
     */
    fun get(epc: String): ELPacketData?

    suspend fun asyncSetI(epc: String, edt: String): ELPacketData?
    suspend fun asyncSetC(epc: String, edt: String): ELPacketData?
    suspend fun asyncGet(epc: String): ELPacketData?
}
