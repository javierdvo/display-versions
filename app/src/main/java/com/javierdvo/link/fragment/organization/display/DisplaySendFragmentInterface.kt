package com.javierdvo.link.fragment.organization.display

import com.javierdvo.link.classes.devices.Display

interface DisplaySendFragmentInterface {
    val sendingDisplay:Display
    fun sendMessage(sendingDevice:Display,message:String)
}