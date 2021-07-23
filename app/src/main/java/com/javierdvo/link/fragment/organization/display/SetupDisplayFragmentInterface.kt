package com.javierdvo.link.fragment.organization.display



interface SetupDisplayFragmentInterface {

    val userWifiSSID:String
    val userWifiPWD:String
    fun setupDisplaySystem(ssid:String,wifiPassword:String)
}