package com.javierdvo.link.fragment.organization.display

import android.bluetooth.BluetoothDevice
import com.javierdvo.link.classes.devices.Display

interface SetupDisplayAddFragmentInterface {
    val userDisplayDevice: Display
    val devAddr: String
    val btSetupDevice: BluetoothDevice
    val organizationNames: ArrayList<String>
    val scheduleNames: ArrayList<String>
    fun nextDisplayNum(): Int
    fun requestLogin()
    fun setupDisplayDevice(displayToAdd:Display,connect:Boolean)
}