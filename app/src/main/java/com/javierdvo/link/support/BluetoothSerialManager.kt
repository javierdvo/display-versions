package com.javierdvo.link.support

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.ArrayMap
import android.util.Log

import java.io.Closeable
import java.util.UUID

import io.reactivex.Single

class BluetoothSerialManager(private val adapter: BluetoothAdapter) : Closeable {
    private val uuidString = "7cd0262e-5f42-43db-b0f9-b73744ea306c"

    private val devices = ArrayMap<String, BluetoothSerialDevice>()

    /**
     * @param device The Device you are trying to connect to
     * @return An RxJava Single, that will either emit
     * a BluetoothSerialDevice or a BluetoothConnectException
     */
    fun openSerialDevice(device: BluetoothDevice): Single<BluetoothSerialDevice> {
        return if (devices.containsKey(device.address)) {
            Single.just(devices[device.address]!!)
        } else {
            Single.fromCallable {
                try {
                    Log.d("BT Connect", device.address)
                    Log.d("BT Connect", uuidString)
                    val socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    adapter.cancelDiscovery()
                    Log.d("BT Connect", "After Socket")
                    socket.connect()
                    Log.d("BT Connect", "After Socket")
                    val serialDevice = BluetoothSerialDevice.getInstance(device, socket)
                    devices[device.address] = serialDevice
                    return@fromCallable serialDevice
                } catch (e: Exception) {
                    throw BluetoothConnectException(e)
                }
            }
        }
    }

    fun openSerialDevice(mac: String): Single<BluetoothSerialDevice> {
        return if (devices.containsKey(mac)) {
            Single.just(devices[mac]!!)
        } else {
            Single.fromCallable {
                try {
                    val device = adapter.getRemoteDevice(mac)
                    val socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                    adapter.cancelDiscovery()
                    socket.connect()
                    val serialDevice = BluetoothSerialDevice.getInstance(device, socket)
                    devices[mac] = serialDevice
                    return@fromCallable serialDevice
                } catch (e: Exception) {
                    throw BluetoothConnectException(e)
                }
            }
        }
    }

    inner class BluetoothConnectException(cause: Throwable) : Exception(cause)

    /**
     * Closes the connection to a device. After calling,
     * you should probably set your instance to null
     * to avoid trying to read/write from it.
     *
     * @param mac The MAC Address of the device you are
     * trying to close the connection to
     */
    fun closeDevice(mac: String) {
        val removedDevice = devices.remove(mac)
        if (removedDevice != null) {
            try {
                removedDevice.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


    /**
     * Close all connected devices
     */
    override fun close() {
        val iterator = devices.entries.iterator()
        while (iterator.hasNext()) {
            val deviceEntry = iterator.next()
            try {
                deviceEntry.value.close()
            } catch (ignored: Throwable) {
            }

            iterator.remove()
        }
    }
}