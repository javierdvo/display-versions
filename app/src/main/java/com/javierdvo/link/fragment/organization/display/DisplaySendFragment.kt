package com.javierdvo.link.fragment.organization.display

import android.app.Activity
import android.util.Log
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.javierdvo.link.R
import com.javierdvo.link.classes.devices.Display


class DisplaySendFragment : Fragment() {

    private var setupSendButton: Button? = null
    private var myActivity: DisplaySendFragmentInterface? = null
    private var messageText: EditText? = null
    private var smileyButton: ImageButton? = null
    private var heartButton: ImageButton? = null
    private var pauseButton: ImageButton? = null
    private var restartButton: ImageButton? = null
    private var restoreButton: ImageButton? = null

    private var displayDevice: Display?=null
    private var editingDisplay:Display?=null
    //private var selPlace:Place?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_display_send, container, false)
        myActivity = activity as DisplaySendFragmentInterface?
        assert(myActivity != null)
        displayDevice=myActivity!!.sendingDisplay
        setupSendButton = view.findViewById(R.id.btn_setup_display_add)
        smileyButton = view.findViewById(R.id.imageButton1)
        heartButton = view.findViewById(R.id.imageButton2)
        pauseButton = view.findViewById(R.id.imageButton6)
        restartButton = view.findViewById(R.id.imageButton3)
        restoreButton = view.findViewById(R.id.imageButton4)
        messageText = view.findViewById(R.id.text_setup_display_send)
        setupSendButton!!.setOnClickListener {
            send(messageText!!.text.toString())
        }
        smileyButton!!.setOnClickListener {
            send("SMILEY")
        }
        heartButton!!.setOnClickListener {
            send("HEART")
        }
        pauseButton!!.setOnClickListener {
            send("STOP")
        }
        restartButton!!.setOnClickListener {
            send("RESTART")
        }
        restoreButton!!.setOnClickListener {
            send("RESTORE")
        }
        return view

    }


    fun send(str:String) {
        Log.d(TAG, "Send")
        val imm = this.requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(this.requireView().windowToken, 0)
        myActivity!!.sendMessage(displayDevice!!,str)

    }
    
    companion object {
        private const val TAG = "Setup Display Add Frag"
    }
}