package com.javierdvo.link.support

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class BluetoothSerialInterface internal constructor(
        /**
         * @return The device instance that the interface is wrapping around.
         */
        internal val device: BluetoothSerialDevice
) {

    private val compositeDisposable = CompositeDisposable()

    private var messageReceivedListener: OnMessageReceivedListener? = null
    private var messageSentListener: OnMessageSentListener? = null
    private var errorListener: OnErrorListener? = null

    init {

        compositeDisposable.add(device.openMessageStream()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ this.onReceivedMessage(it) }, { this.onError(it) }))
    }

    fun sendMessage(message: String) {
        device.requireNotClosed()
        compositeDisposable.add(device.send(message)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ this.onSentMessage(message) }, { this.onError(it) }))
    }

    /**
     * Internal callback called when the BluetoothSerialDevice receives a message
     *
     * @param message The message received from the bluetooth device
     */
    private fun onReceivedMessage(message: String) {
        if (messageReceivedListener != null) {
            messageReceivedListener!!.onMessageReceived(message)
        }
    }

    /**
     * Internal callback called when the BluetoothSerialDevice sends a message
     *
     * @param message The message sent to the bluetooth device
     */
    private fun onSentMessage(message: String) {
        if (messageSentListener != null) {
            messageSentListener!!.onMessageSent(message)
        }
    }

    /**
     * Internal callback called when a Bluetooth send/receive error occurs
     *
     * @param error The error that occurred
     */
    private fun onError(error: Throwable) {
        if (errorListener != null) {
            errorListener!!.onError(error)
        }
    }

    /**
     * Set all of the listeners for the interfact
     *
     * @param messageReceivedListener Receive message callback
     * @param messageSentListener Send message callback (indicates that a message was successfully sent)
     * @param errorListener Error callback
     */
    fun setListeners(messageReceivedListener: OnMessageReceivedListener?,
                     messageSentListener: OnMessageSentListener?,
                     errorListener: OnErrorListener?) {
        this.messageReceivedListener = messageReceivedListener
        this.messageSentListener = messageSentListener
        this.errorListener = errorListener
    }

    /**
     * Set the message received listener
     *
     * @param listener Receive message callback
     */
    fun setMessageReceivedListener(listener: OnMessageReceivedListener?) {
        messageReceivedListener = listener
    }

    /**
     * Set the message sent listener
     *
     * @param listener Send message callback (indicates that a message was successfully sent)
     */
    fun setMessageSentListener(listener: OnMessageSentListener?) {
        messageSentListener = listener
    }

    /**
     * Set the error listener
     *
     * @param listener Error callback
     */
    fun setErrorListener(listener: OnErrorListener?) {
        errorListener = listener
    }

    internal fun close() {
        compositeDisposable.dispose()
    }

    interface OnMessageReceivedListener {
        fun onMessageReceived(message: String)
    }

    interface OnMessageSentListener {
        fun onMessageSent(message: String)
    }

    interface OnErrorListener {
        fun onError(error: Throwable)
    }
}