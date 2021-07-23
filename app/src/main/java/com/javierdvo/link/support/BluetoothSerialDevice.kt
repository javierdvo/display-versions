package com.javierdvo.link.support

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.text.TextUtils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable

class BluetoothSerialDevice private constructor(private val device: BluetoothDevice, private val socket: BluetoothSocket, private val outputStream: OutputStream, private val inputStream: InputStream) {

    private var closed = false
    private val mac: String

    private var owner: BluetoothSerialInterface? = null

    init {
        this.mac = device.address
    }


    /**
     * @param message The message to send to the device
     * @return An RxJava Completable to asynchronously
     * send the message.
     */
    fun send(message: String): Completable {
        requireNotClosed()
        return Completable.fromAction { if (!closed) outputStream.write(message.toByteArray()) }
    }

    /**
     * @return An RxJava Flowable that, when observed, will
     * provide a stream of messages from the device.
     */
    fun openMessageStream(): Flowable<String> {
        requireNotClosed()
        return Flowable.create({ emitter ->
            val `in` = BufferedReader(InputStreamReader(inputStream))
            var breakBool=false
            while (!emitter.isCancelled && !closed) {
                synchronized(this) {
                    try {
                        val receivedString = `in`.readLine()
                        if (!TextUtils.isEmpty(receivedString)) {
                            emitter.onNext(receivedString)
                        }
                    } catch (e: Exception) {
                        if (!emitter.isCancelled && !closed) {
                            emitter.onError(e)
                        } else {
                            breakBool=true
                        }
                    }

                }
                if(breakBool){
                    break
                }
            }
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
    }

    /**
     * @throws IOException if one of the streams
     * throws an exception whilst closing
     */
    @Throws(IOException::class)
    internal fun close() {
        if (!closed) {
            closed = true
            inputStream.close()
            outputStream.close()
            socket.close()
        }
        if (owner != null) {
            owner!!.close()
            owner = null
        }
    }

    /**
     * Wrap using a SimpleBluetoothDeviceInterface.
     * This makes things a lot simpler within the class accessing this device
     *
     * @return a SimpleBluetoothDeviceInterface that will access this device object
     */
    fun toSimpleDeviceInterface(): BluetoothSerialInterface {
        requireNotClosed()
        return if (owner != null) {
            owner!!
        } else {
            BluetoothSerialInterface(this)
        }
    }

    /**
     * Internal function that checks that
     * this instance has not been closed
     */
    internal fun requireNotClosed() {
        if (closed) {
            throw IllegalArgumentException("Device connection closed")
        }
    }

    companion object {

        @Throws(IOException::class)
        internal fun getInstance(device: BluetoothDevice, socket: BluetoothSocket): BluetoothSerialDevice {
            return BluetoothSerialDevice(device, socket, socket.outputStream, socket.inputStream)
        }
    }

}