package com.nekocoders.neuromatrix_apk

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService

import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat

import cn.wch.ch34xuartdriver.CH34xUARTDriver
import android.content.DialogInterface
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.widget.Toast


class Device {
//    private val ACTION_USB_PERMISSION = "com.nekocoders.neuromatrix_apk.USB_PERMISSION"
    private val ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION"

    var T: Int
    var segments: MutableList<Segment>

    lateinit var CH34x_Driver: CH34xUARTDriver
    private val baudRate = 115200 // baud rate
    private val dataBit: Byte = 8 // data bit
    private val stopBit: Byte = 1 // Stopping
    private val parity: Byte = 0 //check
    private val flowControl: Byte = 0 //Flow Control


    init {
        T = 10000
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
    }

    fun connect(ctx: Context) {
        CH34x_Driver = CH34xUARTDriver(
            ctx.getSystemService(Context.USB_SERVICE) as UsbManager?, ctx,
            ACTION_USB_PERMISSION
        )

        if(!CH34x_Driver.UsbFeatureSupported()) { // Do not support, pop up the prompt window
            Toast.makeText(ctx, "Your phone does not support USB HOST", Toast.LENGTH_SHORT).show()
            return
        }

        val retval2 = CH34x_Driver.ResumeUsbPermission()
        if(retval2 != 0) { // No permission
            Toast.makeText(ctx, "Permission not granted " + retval2, Toast.LENGTH_SHORT).show()
            return
        }

        val retval = CH34x_Driver.ResumeUsbList()
        if (retval == -1) { // Open failed, turn off the device
            Toast.makeText(ctx, "Open USB device failed!", Toast.LENGTH_SHORT).show()
            CH34x_Driver.CloseDevice()
            return
        } else if (retval != 0) {
            Toast.makeText(ctx, "Permission not granted maybe!", Toast.LENGTH_SHORT).show()
            CH34x_Driver.CloseDevice()
            return
        }

        // Open success
        // Initialize CH34X serial port
        if (!CH34x_Driver.UartInit()) { //initialization failed
            Toast.makeText(ctx, "initialization failed!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!CH34x_Driver.SetConfig(baudRate, dataBit, stopBit, parity, flowControl)) {
            Toast.makeText(ctx, "Serial Port Configuration Failed!", Toast.LENGTH_SHORT).show()
        }

        Toast.makeText(ctx, "Serial port connected", Toast.LENGTH_SHORT).show()

//        try {
//            val manager: UsbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
//            val devices: Map<String, UsbDevice> = manager.deviceList
//            Toast.makeText(ctx, devices.size.toString(), Toast.LENGTH_SHORT).show()
//            var device: UsbDevice? = null
//            for (dev in devices) {
//                device = dev.value
//                Toast.makeText(
//                    ctx,
//                    dev.key + "|" + dev.value.deviceName + "|" + dev.value.manufacturerName,
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//
//            var DATA: ByteArray = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
//            var TIMEOUT: Int
//
//            val connection: UsbDeviceConnection = manager.openDevice(device)
//            val endpoint: UsbEndpoint? = device?.getInterface(0)?.getEndpoint(0)
//            connection.claimInterface(device?.getInterface(0), true);
//            connection.bulkTransfer(endpoint, DATA, DATA.size, 1000);
//        } catch (e: Exception) {
//            Toast.makeText(ctx, e.stackTraceToString(), Toast.LENGTH_LONG).show()
//        }
    }

    fun initialize() {

    }
}