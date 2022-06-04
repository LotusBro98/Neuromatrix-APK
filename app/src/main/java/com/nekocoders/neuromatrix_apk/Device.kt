package com.nekocoders.neuromatrix_apk

import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context

import android.widget.Toast
import android.bluetooth.BluetoothAdapter

import android.content.Intent
import java.util.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.*
import android.widget.TextView
import kotlinx.coroutines.*
import java.io.InputStream
import java.lang.Exception
import java.util.zip.CRC32
import kotlin.math.min

val TIME_CAROUSEL_US = 10000

const val REQUEST_ENABLE_BT = 1
private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private val CYBER_SUIT_NAME = "JDY-31-SPP"
private val BEG_CHAR: Byte = 0xB4.toByte()

private val CMD_SET_IMPULSE: Byte = 0x01
private val CMD_CLEAR: Byte = 0x02
private val CMD_INIT: Byte = 0x03
private val CMD_CFG_SEG: Byte = 0x04

class Impulse(var delay: Int, var duration: Int) {
}

class CommandMessage(var ctx: Context, var board: Int, var command: ByteArray, var log: Boolean) {
}

fun InputStream.read(
    buffer: ByteArray,
    offset: Int,
    length: Int,
    timeout: Long
): Int {
    val delta: Long = 1
    var byteCount = 0
    for (i in 1..(timeout/delta)) {
        if (available() > 0) {
            var len = length - byteCount
            len = min(available(), len)
            byteCount += read(buffer, offset + byteCount, len)
        }
        if (byteCount >= length) {
            return byteCount
        }
        runBlocking { delay(delta) }
    }
    return byteCount
}

class Device(var ctx: Context) {
    var cmd_log: String = ""
    var T: Int
    var segments: MutableList<Segment>
    var socket: BluetoothSocket? = null
    var cmd_log_view: TextView? = null

    val boards: Array<IntArray> = arrayOf(
        intArrayOf(),
        intArrayOf(1),
        intArrayOf(2)
    )

    var cmd_handler: Handler
    var cmd_handler_thread: HandlerThread

    var ui_handler: Handler

    init {
        cmd_handler_thread = HandlerThread("cmd handler")
        cmd_handler_thread.start()
        cmd_handler = Handler(cmd_handler_thread.looper)

        ui_handler = Handler(Looper.getMainLooper())

        T = TIME_CAROUSEL_US
        segments = mutableListOf()
        segments.add(Segment("L1P",      0, intArrayOf(0), 0, intArrayOf(9),500))
        segments.add(Segment("L1P",      0, intArrayOf(0), 0, intArrayOf(10),500))
        segments.add(Segment("L1P",      0, intArrayOf(0), 0, intArrayOf(11),500))
        segments.add(Segment("L1P",      0, intArrayOf(0), 0, intArrayOf(12),500))
        segments.add(Segment("L1P",      0, intArrayOf(0), 0, intArrayOf(13),500))
        segments.add(Segment("L1P",      0, intArrayOf(0), 0, intArrayOf(8),500))
//        segments.add(Segment("L1",       500))
//        segments.add(Segment("L2",       500))
//        segments.add(Segment("L3",       500))
//        segments.add(Segment("L4",       500))
//        segments.add(Segment("L5",       500))
//        segments.add(Segment("Lудерж",   500))
//        segments.add(Segment("Lвправо",  500))
//        segments.add(Segment("Lвлево",   500))
//        segments.add(Segment("Lзапястье",500))
//
//        segments.add(Segment("R1P",      500))
//        segments.add(Segment("R1",       500))
//        segments.add(Segment("R2",       500))
//        segments.add(Segment("R3",       500))
//        segments.add(Segment("R4",       500))
//        segments.add(Segment("R5",       500))
//        segments.add(Segment("Rудерж",   500))
//        segments.add(Segment("Rвправо",  500))
//        segments.add(Segment("Rвлево",   500))
//        segments.add(Segment("Rзапястье",500))

        for (i in segments.indices) {
            segments[i].curCommand.segment = i
            segments[i].curCommand.load(ctx)
        }
    }

    fun connect(ctx: Activity) {
        val manager: BluetoothManager? = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        val adapter = manager?.adapter
        if (manager == null || adapter == null) {
            Toast.makeText(ctx, "Bluetooth is not supported", Toast.LENGTH_SHORT).show()
            return
        }

        if (!adapter.isEnabled()) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            ctx.startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }

        var dev: BluetoothDevice? = null
        var addr: String? = null
        for (device in adapter.bondedDevices) {
            if (device.name == CYBER_SUIT_NAME) {
                dev = device
                addr = device.address
                break
            }
        }
        if (dev == null) {
            Toast.makeText(ctx, "Failed to find device 'CyberSuit-3000'", Toast.LENGTH_SHORT).show()
            return
        }

        BTConnectAsyncTask(ctx, adapter).execute(addr)
    }

    fun postCommand(ctx: Context, command: ByteArray, msg:String="", log: Boolean=true) {
        cmd_handler.post {
            sendCommand(ctx, command, log, msg)
        }
    }

    fun sendCommand(ctx: Context, command: ByteArray, log: Boolean=true, msg: String="") {
        val cmd = wrapPacket(command)

        var response: ByteArray? = null
        val retries = 1
        try {
            for (i in 1..retries) {
                socket!!.outputStream.write(cmd)
                socket!!.outputStream.flush();
                var resp: ByteArray = ByteArray(1024)
                var byteCount1 = 0
                byteCount1 = socket!!.inputStream.read(resp, 0, 1, 200)
                while (byteCount1 != 0 && resp[0] != BEG_CHAR) {
                    byteCount1 = socket!!.inputStream.read(resp, 0, 1, 200)
                }
                if (byteCount1 == 0 || resp[0] != BEG_CHAR) {
                    continue
                }
                socket!!.inputStream.read(resp, 0, 1, 1000)
                var length = resp[0].toUByte()
                if (length.toUInt().toInt() == 0)
                    continue
                val byteCount = socket!!.inputStream.read(resp, 0, length.toInt() - 1, 1000)
                val resp2 = unwrapPacket(resp.copyOfRange(0, byteCount))
                response = resp2
                if (resp2 != null && resp2.decodeToString().subSequence(0, 2) == "OK") {
                    break
                }
            }

            if (log) {
                cmd_log += msg + " --> : " + bytesToHex(cmd) + "\n"
                cmd_log += "<-- : " + response?.decodeToString() + "\n"

                ui_handler.post {
                    cmd_log_view?.text = cmd_log
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ui_handler.post {
                Toast.makeText(ctx, "Failed to send command to device", Toast.LENGTH_SHORT).show()
            }
            cmd_handler.removeCallbacksAndMessages(null)
            socket = null
        }
    }

    fun initialize(ctx: Context) {
        for (seg in segments) {
            set_seg(ctx, seg)
        }

        postCommand(ctx, cmd_init(), "Init")
    }

    fun cmd_set_impulse(segment: Int, per: Int, duration: Int): ByteArray {
        var cmd = byteArrayOf(CMD_SET_IMPULSE)
        var period = per
        if (period < 1) {
            period = 1
        }
        cmd += UInt16toByteArray(segment)
        cmd += UInt16toByteArray(duration)
        cmd += UInt16toByteArray(period)
        return cmd
    }

    fun cmd_clear(segment: Int): ByteArray {
        var cmd = byteArrayOf(CMD_CLEAR)
        cmd += byteArrayOf(segment.toByte())
        return cmd
    }

    fun cmd_init(): ByteArray {
        var cmd = byteArrayOf(CMD_INIT)
        return cmd
    }

    fun cmd_set_seg(segment: Int, adr1: Int, mask1:Int, adr2: Int, mask2:Int, time_window_us:Int): ByteArray {
        var cmd = byteArrayOf(CMD_CFG_SEG)
        cmd += UInt16toByteArray(mask1)
        cmd += UInt16toByteArray(mask2)
        cmd += UInt16toByteArray(time_window_us)
        cmd += byteArrayOf(segment.toByte())
        cmd += byteArrayOf(adr1.toByte())
        cmd += byteArrayOf(adr2.toByte())
        return cmd
    }

    fun get_mask(arr: IntArray): Int {
        var mask = 0
        for (i in 0..15) {
            if (i in arr) {
                mask = mask or (1 shl i)
            }
        }
        return mask
    }

    fun set_seg(ctx: Context, segment: Segment) {
        val id = segments.indexOf(segment)
        val mask1 = get_mask(segment.mask1)
        val mask2 = get_mask(segment.mask2)
        postCommand(ctx, cmd_set_seg(id, segment.adr1, mask1, segment.adr2, mask2, segment.timeSlot), msg= "set segment ${segment.name}")
//        postCommand(ctx, cmd_init(), "Init")
    }

    fun set_impulse(ctx: Context, segment: Int, period: Int, tau: Int) {
        postCommand(ctx, cmd_set_impulse(segment, period, tau), msg="impulse on " + segments[segment].name + ", tau=" + tau)
    }

    fun calibrate_channel(ctx: Context, brd1: Int, brd2: Int, channel: Int) {
    }

    fun UInt16toByteArray(value: Int): ByteArray {
        val bytes = ByteArray(2)
        bytes[0] = (value and 0xFFFF).toByte()
        bytes[1] = ((value ushr 8) and 0xFFFF).toByte()
        return bytes
    }

    fun UInt32toByteArray(value: Long): ByteArray {
        val bytes = ByteArray(4)
        bytes[0] = (value and 0xFFFF).toByte()
        bytes[1] = ((value ushr 8) and 0xFFFF).toByte()
        bytes[2] = ((value ushr 16) and 0xFFFF).toByte()
        bytes[3] = ((value ushr 24) and 0xFFFF).toByte()
        return bytes
    }

    fun ByteArrayToUInt32(bytes: ByteArray): Long {
        var value: ULong = 0U
        value = value or (bytes[0].toUByte().toULong() shl 0)
        value = value or (bytes[1].toUByte().toULong() shl 8)
        value = value or (bytes[2].toUByte().toULong() shl 16)
        value = value or (bytes[3].toUByte().toULong() shl 24)
        return value.toLong()
    }

    fun wrapPacket(command: ByteArray): ByteArray {
        val crc32 = CRC32()
        crc32.reset()
        crc32.update(command)
        var cmd = byteArrayOf((command.size + 5).toByte()) + command
        cmd = byteArrayOf(BEG_CHAR) + cmd + UInt32toByteArray(crc32.value)
        return cmd
    }

    fun unwrapPacket(packet: ByteArray): ByteArray? {
        if (packet.size < 4)
            return null

        val crc_recv = ByteArrayToUInt32(packet.copyOfRange(packet.size-4, packet.size))
        val crc32 = CRC32()
        crc32.reset()
        val data = packet.copyOfRange(0, packet.size - 4)
        crc32.update(data)
        val crc_calc = crc32.value

        if (crc_recv != crc_calc)
            return packet

        return data
    }

    fun getMaxTau(channel: Int): Int {
        return segments[channel].timeSlot / 2
    }

    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 3)
        for (j in bytes.indices) {
            val v: Int = bytes[j].toUByte().toInt() and 0xFF
            hexChars[j * 3] = HEX_ARRAY[v ushr 4]
            hexChars[j * 3 + 1] = HEX_ARRAY[v and 0x0F]
            hexChars[j * 3 + 2] = ' '
        }
        return String(hexChars)
    }

    inner private class BTConnectAsyncTask(var ctx: Context, var adapter: BluetoothAdapter) : AsyncTask<String?, Int?, Boolean>() {
        var deviceName: String? = null
        override fun doInBackground(vararg address: String?): Boolean {
            val device: BluetoothDevice = adapter.getRemoteDevice(address[0])
            deviceName = device.name
            return try {
                val sock = device.createRfcommSocketToServiceRecord(BLUETOOTH_SPP)
                sock!!.connect()
                socket = sock
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                cmd_log += "Connected to $deviceName\n"

                ui_handler.post {
                    cmd_log_view?.text = cmd_log
                }
                Toast.makeText(ctx, "Connected to $deviceName", Toast.LENGTH_SHORT).show()
                initialize(ctx)
            } else {
                Toast.makeText(ctx, "Failed to connect", Toast.LENGTH_SHORT).show()
            }
        }
    }
}