package com.javierdvo.link.fragment.organization.display


import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.javierdvo.link.R
import com.javierdvo.link.classes.devices.Display

import kotlin.collections.ArrayList

/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class DisplayListFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {
    private var userListener: ValueEventListener?=null
    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null
    private var recyclerView: RecyclerView? = null
    private var myActivity: DisplayListFragmentInterface? = null
    private var items: ArrayList<Display> = ArrayList()
    private var enabled: ArrayList<Boolean> = ArrayList()
    private var emptyView: ScrollView? = null
    private var emptyViewText: TextView? = null
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        val view = inflater.inflate(R.layout.fragment_display_list, container, false)
        val context = view.context
        recyclerView = view.findViewById(R.id.card_recycler_view_device)
        emptyView = view.findViewById(R.id.card_scroll_view_empty)
        emptyViewText = emptyView!!.findViewById(R.id.emptyView)
        emptyViewText!!.text = ""

        Log.d(TAG, recyclerView!!.toString())

        if (mColumnCount <= 1) {
            recyclerView!!.layoutManager = LinearLayoutManager(context)
        } else {
            recyclerView!!.layoutManager = GridLayoutManager(context, mColumnCount)
        }
        myActivity = activity as DisplayListFragmentInterface?
        assert(myActivity != null)

        // SwipeRefreshLayout
        mSwipeRefreshLayout = view.findViewById(R.id.card_swipe_refresh_device)
        mSwipeRefreshLayout!!.setOnRefreshListener(this)
        mSwipeRefreshLayout!!.setColorSchemeResources(R.color.mtrl_primary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_red_light)

        mSwipeRefreshLayout!!.post { this.fetchDisplays() }
        val fab = view.findViewById<FloatingActionButton>(R.id.display_new_fab)
        fab.setOnClickListener { myActivity!!.addDisplay() }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        }
        else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {

        super.onDetach()
        database = FirebaseDatabase.getInstance().reference
        userListener?.let { database.removeEventListener(it) }
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Display)
    }
    interface OnCheckedChangeListener {
        fun onCheckedChangeListener(item: Display, allowed:Boolean)
    }


    override fun onRefresh() {
        Log.d(TAG, "Refresh")
        // Fetching data from server

        if (checkNetwork() != 0) {
            //fetchDisplays()
            mSwipeRefreshLayout!!.isRefreshing = false

        } else {
            Toast.makeText(this.context, "Check your Internet Connection", Toast.LENGTH_LONG).show() // Replace context with your context instance.
        }
    }



    private fun fetchDisplays() {
        userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                Log.d(TAG,dataSnapshot.toString())
                mSwipeRefreshLayout!!.isRefreshing = false
                items= ArrayList()
                val displays = dataSnapshot.child("displaysdvo")
                for (child in displays.children) {
                    val childVal=child.getValue(Display::class.java)
                    Log.d("DEBG",childVal.toString())
                    if (childVal != null) {
                        items.add(childVal)
                    }
                    Log.d("DEBG",items.toString())
                }

                if (items.size <= 0) {
                        emptyViewText!!.text = getString(R.string.missing_devices)
                        recyclerView!!.adapter = DisplayRecyclerViewAdapter(ArrayList(), mListener)
                    } else {
                        emptyViewText!!.text = ""
                        Log.d("HTTP $TAG", items.toString())
                        Log.d("HTTP $TAG", enabled.toString())
                        recyclerView!!.adapter = DisplayRecyclerViewAdapter(items, mListener)

                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        database = FirebaseDatabase.getInstance().reference
        database.addValueEventListener(userListener as ValueEventListener)

    }
    private fun checkNetwork(): Int {
        var netType = 0
        val connMgr =  this.requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
        private const val TAG = "DEVICE FRAGMENT"

    }


}
