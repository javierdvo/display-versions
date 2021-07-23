package com.javierdvo.link.fragment.organization.display

import android.app.Activity
import android.util.Log
import androidx.fragment.app.Fragment

import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.javierdvo.link.R


import java.util.*

class SetupDisplayFragment : Fragment(){

    private var setupDisplayButton: Button? = null
    private var myActivity: SetupDisplayFragmentInterface? = null
    private var ssid =""
    private var wifiPassword=""
    private var ssidText: EditText? = null
    private var passwordText: EditText? = null
    private var deviceNames: ArrayList<String>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_setup_display, container, false)
        myActivity = activity as SetupDisplayFragmentInterface?
        assert(myActivity != null)

        ssidText = view.findViewById(R.id.text_setup_display_ssid)
        passwordText = view.findViewById(R.id.text_setup_display_password)
        setupDisplayButton = view.findViewById(R.id.btn_setup_display_add)

        ssidText!!.setText(myActivity!!.userWifiSSID)
        passwordText!!.setText(myActivity!!.userWifiPWD)
        myActivity = activity as SetupDisplayFragmentInterface?
        assert(myActivity != null)



        setupDisplayButton!!.setOnClickListener {
            if (checkNetwork() != 0) {
                add()
            } else {
                Toast.makeText(this.context, "Check your Internet Connection", Toast.LENGTH_LONG).show() // Replace context with your context instance.
            }
        }
        setupDisplayButton!!.isEnabled=true

        return view

    }


    fun add() {
        Log.d(TAG, "Add")
        setupDisplayButton!!.isEnabled = false
        val imm = this.requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.requireView().windowToken, 0)
        Log.d(TAG,validateSSID().toString())
        Log.d(TAG,validatePassword().toString())

        if (validateSSID() && validatePassword()) {
            myActivity!!.setupDisplaySystem(ssid, wifiPassword)
        }
        else {
            setupDisplayButton!!.isEnabled = true
        }

    }



    private fun validateSSID(): Boolean {
        var valid = true
        ssid = ssidText!!.text.toString()
        if (ssid.isEmpty()) {
            ssidText!!.error = "Please introduce a valid SSID"
            valid = false
        } else {
            ssidText!!.error = null
        }
        return valid
    }

    private fun validatePassword(): Boolean {
        var valid = true
        wifiPassword = passwordText!!.text.toString()
        if (wifiPassword.isEmpty()) {
            passwordText!!.error = "Please introduce a valid password"
            valid = false
        } else {
            passwordText!!.error = null
        }
        return valid
    }



    private fun checkNetwork(): Int {
        var netType = 0
        val connMgr =this.requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var isWifiConn = false
        var isMobileConn = false
        for (network in connMgr.allNetworks) {
            val networkInfo = connMgr.getNetworkCapabilities(network)
            if (networkInfo!!.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                isWifiConn = isWifiConn or networkInfo!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (isWifiConn) netType += 1
            }
            if (networkInfo!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                isMobileConn = isMobileConn or networkInfo!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (isMobileConn) netType += 2
            }
        }
        Log.d(TAG, "Wifi connected: $isWifiConn")
        Log.d(TAG, "Mobile connected: $isMobileConn")
        return netType
    }


    companion object {
        private const val TAG = "Setup Display Fragment"
    }
}