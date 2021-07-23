package com.javierdvo.link.fragment.organization.display

import android.app.Activity
import android.util.Log
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.javierdvo.link.R
import com.javierdvo.link.classes.devices.Display


class SetupDisplayAddFragment : Fragment() {

    private var setupDisplayButton: Button? = null
    private var myActivity: SetupDisplayAddFragmentInterface? = null
    private var nameText: EditText? = null
    private var descText: EditText? = null
    private var addrText: EditText? = null
    private var plusText: EditText? = null
    private var latText: EditText? = null
    private var longText: EditText? = null
    private var displayDevice: Display?=null
    private var currentDisplayNum=1



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_setup_display_add_device, container, false)
        myActivity = activity as SetupDisplayAddFragmentInterface?
        assert(myActivity != null)

        displayDevice=myActivity!!.userDisplayDevice
        currentDisplayNum=myActivity!!.nextDisplayNum()

        setupDisplayButton = view.findViewById(R.id.btn_setup_display_add)
        nameText = view.findViewById(R.id.text_setup_display_add_name)

        setupDisplayButton!!.setOnClickListener {
            add()
        }
        setupDisplayButton!!.isEnabled=true


        return view

    }


    fun add() {
        Log.d(TAG, "Add")
        setupDisplayButton!!.isEnabled = false
        val imm = this.requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.requireView().windowToken, 0)
        val displayToAdd= Display(myActivity!!.devAddr,myActivity!!.devAddr,nameText!!.text.toString(),currentDisplayNum,ArrayList())
        myActivity!!.setupDisplayDevice(displayToAdd,true)

    }

    companion object {
        private const val TAG = "Setup Display Add Frag"
    }
}