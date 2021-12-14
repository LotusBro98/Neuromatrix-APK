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

val TIME_CAROUSEL_US = 10000

const val REQUEST_ENABLE_BT = 1
private val BLUETOOTH_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private val CYBER_SUIT_NAME = "CyberSuit-3000"
private val BEG_CHAR: Byte = 'H'.toByte()
private val END_CHAR: Byte = 'B'.toByte()

private val CMD_PULLDOWN: Byte = 0x01
private val CMD_RELEASE: Byte = 0x02
private val CMD_CALIBRATE: Byte = 0x03
private val CMD_SET_PATTERN: Byte = 0x04
private val CMD_GET_SAMPLES: Byte = 0x05
private val CMD_BYPASS: Byte = 0x06
private val CMD_CLK_SYNC: Byte = 0x07
private val CMD_ALLOCATE: Byte = 0x08
private val CMD_SET_PERIOD: Byte = 0x09
private val CMD_ASSIGN: Byte = 0x0a

class Impulse(var delay: Int, var duration: Int) {
}

class CommandMessage(var ctx: Context, var board: Int, var command: ByteArray, var log: Boolean) {
}

fun InputStream.read(
    buffer: ByteArray,
    offset: Int,
    length: Int,
    ans_timeout: Long,
    timeout: Long
): Int = runBlocking {
    var byteCount = 0
    try {
        withTimeout(ans_timeout) {
            while (available() <= 0) {
                delay(timeout)
            }
            var read_now = -1
            while (read_now != 0) {
                try {
                    withTimeout(timeout) {
                        read_now = async {
                            if (available() > 0) read(
                                buffer,
                                offset + byteCount,
                                length - byteCount
                            ) else 0
                        }.await()
                    }
                    byteCount += read_now
                } catch (e: TimeoutCancellationException) {
                    read_now = 0
                }
            }
        }
    } catch (e: TimeoutCancellationException) {

    }
    byteCount
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
        segments.add(Segment("Лев. большой", 0, 1, 200, arrayOf(0,1,2,3,4,5,6,7), arrayOf(0,1)))
        segments.add(Segment("Лев. указат.", 0, 1, 200, arrayOf(0,1,2,3,4,5,6,7), arrayOf(2)))
        segments.add(Segment("Лев. средний", 0, 1, 200, arrayOf(0,1,2,3,4,5,6,7), arrayOf(3)))
        segments.add(Segment("Лев. безымян", 0, 1, 200, arrayOf(0,1,2,3,4,5,6,7), arrayOf(4)))
        segments.add(Segment("Лев. мизинец", 0, 1, 200, arrayOf(0,1,2,3,4,5,6,7), arrayOf(5)))

        segments.add(Segment("Прав. большой", 0, 2, 200, arrayOf(8,9,10,11,12,13,14,15), arrayOf(0,1)))
        segments.add(Segment("Прав. указат.", 0, 2, 200, arrayOf(8,9,10,11,12,13,14,15), arrayOf(2)))
        segments.add(Segment("Прав. средний", 0, 2, 200, arrayOf(8,9,10,11,12,13,14,15), arrayOf(3)))
        segments.add(Segment("Прав. безымян", 0, 2, 200, arrayOf(8,9,10,11,12,13,14,15), arrayOf(4)))
        segments.add(Segment("Прав. мизинец", 0, 2, 200, arrayOf(8,9,10,11,12,13,14,15), arrayOf(5)))

        for (i in segments.indices) {
            segments[i].curCommand.channel = i
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

    fun postCommand(ctx: Context, board: Int, command: ByteArray, msg:String="", log: Boolean=true) {
        cmd_handler.post {
            sendCommand(ctx, board, command, log, msg)
        }
    }

    fun sendCommand(ctx: Context, board: Int, command: ByteArray, log: Boolean=true, msg: String="") {
        val address = boards[board]
        var addr = byteArrayOf()
        for (i in address) {
            addr += CMD_BYPASS
            if (i == 1) {
                addr += 1
            } else {
                addr += 2
            }
        }
        var cmd_addr = addr + command
        val cmd = wrapPacket(cmd_addr)

        var response: ByteArray? = null
        val retries = 5
        try {
            for (i in 1..retries) {
//                val skipped = socket!!.inputStream.skip(socket!!.inputStream.available().toLong())
                socket!!.outputStream.write(cmd)
                socket!!.outputStream.flush();
                var resp: ByteArray? = ByteArray(1024)
                val byteCount = socket!!.inputStream.read(resp!!, 0, 1024, 300, 20)
                resp = unwrapPacket(resp.copyOfRange(0, byteCount))
                if (resp != null) {
                    response = resp
                    break
                }
            }

            if (log) {
                cmd_log += msg + " --> : " + bytesToHex(cmd_addr) + "\n"
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
        var masks = Array(boards.size)
        {
            Array(segments.size) {
                IntArray(0)
            }
        }

        var slots = IntArray(segments.size, {segments[it].timeSlot})

        for (i in boards.indices) {
            for (i_seg in segments.indices) {
                if (segments[i_seg].board1 == i) {
                    masks[i][i_seg] = segments[i_seg].mask1.toIntArray()
                } else if (segments[i_seg].board2 == i) {
                    masks[i][i_seg] = segments[i_seg].mask2.toIntArray()
                }
            }
        }

        for (i in boards.indices) {
            postCommand(ctx, 0, byteArrayOf(0), log=false)
            postCommand(ctx, i, cmd_assign(masks[i]), msg="assign")
        }

        for (i_seg in segments.indices) {
            calibrate_channel(ctx, segments[i_seg].board1, segments[i_seg].board2, i_seg)
        }

        postCommand(ctx, 0, cmd_clk_sync(1), "clk sync")

        for (i in boards.indices) {
            postCommand(ctx, 0, byteArrayOf(0), log=false)
            postCommand(ctx, i, byteArrayOf(CMD_RELEASE))
            postCommand(ctx, i, cmd_allocate(slots), "allocate")
            postCommand(ctx, i, cmd_set_period(T))
        }
    }

    fun cmd_set_pattern(channel: Int, per: Int, pattern: Array<Impulse>): ByteArray {
        var cmd = byteArrayOf(CMD_SET_PATTERN)
        var period = per - 1
        if (period < 0) {
            period = 0
        }
        cmd += UInt16toByteArray(channel)
        cmd += UInt16toByteArray(period)
        for (impulse in pattern) {
            cmd += UInt16toByteArray(impulse.delay) + UInt16toByteArray(impulse.duration)
        }
        return cmd
    }

    fun cmd_allocate(slots: IntArray): ByteArray {
        var cmd = byteArrayOf(CMD_ALLOCATE)
        for (slot in slots) {
            cmd += UInt16toByteArray(slot)
        }
        return cmd
    }

    fun cmd_set_period(period: Int): ByteArray  {
        var cmd = byteArrayOf(CMD_SET_PERIOD)
        cmd += UInt32toByteArray(period.toLong())
        return cmd
    }

    fun cmd_clk_sync(depth: Int): ByteArray  {
        var cmd = byteArrayOf(CMD_CLK_SYNC)
        cmd += 0
        cmd += depth.toByte()
        return cmd
    }

    fun cmd_pulldown(channel: Int): ByteArray {
        var cmd = byteArrayOf(CMD_PULLDOWN)
        cmd += channel.toByte()
        return cmd
    }

    fun cmd_calibrate(channel: Int): ByteArray {
        var cmd = byteArrayOf(CMD_CALIBRATE)
        cmd += channel.toByte()
        return cmd
    }

    fun cmd_assign(channel_masks: Array<IntArray>): ByteArray {
        var cmd = byteArrayOf(CMD_ASSIGN)
        for (channel in channel_masks) {
            var mask = 0
            for (i in 0..15) {
                if (i in channel) {
                    mask = mask or (1 shl i)
                }
            }
            cmd += UInt16toByteArray(mask)
        }
        return cmd
    }

    fun set_impulse(ctx: Context, brd1: Int, brd2: Int, channel: Int, period: Int, tau: Int) {
        postCommand(ctx, 0, byteArrayOf(0), log=false)
        postCommand(ctx, brd1, cmd_set_pattern(channel, period, arrayOf(Impulse(0, tau))), msg="pattern ch " + channel + " brd " + brd1)
        postCommand(ctx, 0, byteArrayOf(0), log=false)
        postCommand(ctx, brd2, cmd_set_pattern(channel, period, arrayOf(Impulse(tau, tau))), msg="pattern ch " + channel + " brd " + brd2)
    }

    fun calibrate_channel(ctx: Context, brd1: Int, brd2: Int, channel: Int) {
        postCommand(ctx, brd2, cmd_pulldown(channel))
        postCommand(ctx, 0, byteArrayOf(0), log=false)
        postCommand(ctx, brd1, cmd_calibrate(channel), msg="calibrate ch " + channel + " brd " + brd1)
        postCommand(ctx, brd1, cmd_pulldown(channel))
        postCommand(ctx, 0, byteArrayOf(0), log=false)
        postCommand(ctx, brd2, cmd_calibrate(channel), msg="calibrate ch " + channel + " brd " + brd2)
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
        val cmd = byteArrayOf(BEG_CHAR) + command + UInt32toByteArray(crc32.value) + byteArrayOf(END_CHAR)
        return cmd
    }

    fun unwrapPacket(packet: ByteArray): ByteArray? {
        if (packet.size < 6)
            return null
        if (packet[0] != BEG_CHAR || packet[packet.size - 1] != END_CHAR)
            return null

        val crc_recv = ByteArrayToUInt32(packet.copyOfRange(packet.size-5, packet.size-1))
        val crc32 = CRC32()
        crc32.reset()
        val data = packet.copyOfRange(1, packet.size - 5)
        crc32.update(data)
        val crc_calc = crc32.value

        if (crc_recv != crc_calc)
            return null

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