package com.javierdvo.link

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.ACTION_FOUND
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.javierdvo.link.classes.devices.Display
import com.javierdvo.link.fragment.organization.display.*
import com.javierdvo.link.support.BluetoothSerialDevice
import com.javierdvo.link.support.BluetoothSerialInterface
import com.javierdvo.link.support.BluetoothSerialManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

private const val SCAN_PERIOD: Long = 5000
const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"

/**
 * Created by javier de velasco on 12/10/20
 */
class MainActivity : AppCompatActivity(),NavigationView.OnNavigationItemSelectedListener,
    DisplayListFragment.OnListFragmentInteractionListener, DisplayListFragmentInterface,
    BluetoothSerialInterface.OnMessageSentListener, SetupDisplayFragmentInterface,
    SetupDisplayAddFragmentInterface, DisplayListFragment.OnCheckedChangeListener, DisplaySendFragmentInterface
{
    private var setupDisplayId=0
    private var setupSSID: String=""
    private var setupWiFiPassword: String=""
    private lateinit var database: DatabaseReference
    private val TAG = "MainActivity"
    var DisplayList= ArrayList<Display>()
    private var displayNameList= ArrayList<String>()
    private var organizationNameList= ArrayList<String>()
    private var scheduleNameList= ArrayList<String>()
    private var userNameList= ArrayList<String>()
    private var openingDisplay:Display= Display()
    private var displayList=ArrayList<Display>()
    private var itemDisplay:Display= Display()
    private var mScanning: Boolean = false
    private var geoAddr=""
    private var bluetoothEnabled = false
    private var sharedPref: SharedPreferences? = null
    private var navigationView: NavigationView? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSerialManager: BluetoothSerialManager? = null
    private var deviceInterface: BluetoothSerialInterface? = null
    private var bluetoothConnectType = 0
    private var attemptBtConnect = false
    private var attemptStartup = false
    private var currentFragment: String? = null
    private val handler= Handler()
    private var bluetoothDevices:ArrayList<BluetoothDevice> =ArrayList()
    private var bluetoothDevice: BluetoothDevice? =null
    private var currentSetupIndex=0
    private var currentDisplayDevice= Display("w")
    private var setupDisplayDevice= Display("w")
    private var displayToSend= Display("w")
    private var action=""
    private var displayToEdit:Display?=null
    private var usrDevAddr=""
    var connectionState = BluetoothAdapter.STATE_DISCONNECTED

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                Log.d("BT Discovery", "Device name: " + device!!.name)
                Log.d("BT Discovery", "Device MAC Address: " + device.address)
                if (device.name != null) {
                    if ("LKDP" in device.name) {
                        //bluetoothAdapter!!.cancelDiscovery()
                        Log.d("name",device.name)
                        Log.d("SUBSTR",device.name.substring(4))
                        setupDisplayDevice= Display(device.name.substring(4))
                        usrDevAddr=device.name.substring(4)
                        bluetoothDevices.add(device)
                        Log.d(
                            "BT Discovery",
                            "Discovery finished: Link Display Found"
                        )
                        bluetoothAdapter!!.cancelDiscovery()

                        currentFragment = "SETUP_DISPLAY_ADD_FRAGMENT"
                        val transaction = supportFragmentManager.beginTransaction()
                        val fragment1 = SetupDisplayAddFragment()
                        bluetoothDevice=device
                        transaction.replace(R.id.content_main, fragment1)
                        transaction.commitAllowingStateLoss()

                        //bluetoothConnectType = 2
                        //connectDevice(device)
                    }
                }
            }
            if (ACTION_DISCOVERY_STARTED == action) {
                setupDisplayDevice= Display()
                bluetoothDevices=ArrayList()
                Log.d("BT Discovery", "Discovery Started")

            }
            if (ACTION_DISCOVERY_FINISHED == action) {
                if (bluetoothDevices.size<1) {
                    Log.d(
                        "BT Discovery",
                        "Discovery finished: No Link Display Found"
                    )
                    Toast.makeText(
                        baseContext,
                        "No se encontraron pantallas  ",
                        Toast.LENGTH_LONG
                    ).show() // Replace context with your context instance.
                    currentFragment = "SETUP_DISPLAY_FRAGMENT"
                    val transaction = supportFragmentManager.beginTransaction()
                    val fragment1 = SetupDisplayFragment()
                    transaction.replace(R.id.content_main, fragment1)
                    transaction.commitAllowingStateLoss()
                }
            }
        }
    }
    
    override public fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val permissionsRequestFineLocation = 2
        while(ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    permissionsRequestFineLocation
                )
            }
            else{
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    permissionsRequestFineLocation
                )
            }
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission is not granted")
            }}

        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        setContentView(R.layout.activity_main)
        //verifySignInLink()
        checkCurrentUser()

        val initListener = object :ValueEventListener {
            @SuppressLint("MissingPermission")
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d("TEST0", "Init data read")
                displayList=ArrayList()
                val displays = dataSnapshot.child("displaysdvo")
                for (child in displays.children) {
                    Log.d(TAG, child.toString())
                    child.getValue(Display::class.java)?.let { displayList.add(it) }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        val displayListener = object :ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                displayList=ArrayList()
                displayNameList= ArrayList()

                // Get Post object and use the values to update the UI
                val displays = dataSnapshot
                for (child in displays.children) {
                    Log.d(TAG, child.toString())
                    child.getValue(Display::class.java)?.let { displayList.add(it) }
                    child.getValue(Display::class.java)?.let { displayNameList.add(it.name) }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }

        if(Firebase.auth.currentUser!=null) {
            database = FirebaseDatabase.getInstance().reference
            database.addListenerForSingleValueEvent(initListener)
            database.child("test").setValue("test")
            database.child("test").removeValue()

            database.child("displaysdvo").addValueEventListener(displayListener)
            //database.child("users").removeValue()
            currentFragment = "DISPLAY_LIST_FRAGMENT"
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter!!.cancelDiscovery()

            val bluetoothFilter = IntentFilter()
            bluetoothFilter.addAction(ACTION_DISCOVERY_STARTED)
            bluetoothFilter.addAction(ACTION_FOUND)
            bluetoothFilter.addAction(ACTION_DISCOVERY_FINISHED)

            this.registerReceiver(receiver, bluetoothFilter)



            checkBluetooth()
            action = intent.getStringExtra("ACTION") ?: "Null"
            geoAddr = intent.getStringExtra("ADDRESS") ?: "Null"


            if (checkNetwork() != 0) {
                startup()
            } else {
                attemptStartup = true
                Toast.makeText(this, "Check your Internet Connection", Toast.LENGTH_LONG)
                    .show() // Replace context with your context instance.
                wifiDialog()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                Log.d(TAG, "Signup Success")

                if (user != null) {
                    Log.d(TAG, user.uid)
                    val bundle = Bundle()
                    bundle.putString("ACTION", "loggedIn")
                    val intent2 = Intent()
                    intent2.setClassName(
                        this.applicationContext,
                        "com.javierdvo.vio_access_control.MainActivity"
                    )
                    intent2.putExtras(bundle)
                    intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    this.applicationContext.startActivity(intent2)
                }
            } else {
                if (response != null) {
                    Log.d(TAG, "Signup Error")
                    Log.d(TAG, response.error?.errorCode.toString())
                } else {
                    Log.d(TAG, "Flow Cancelled")
                }
            }
        }
    }

    override val devAddr: String
        get() = usrDevAddr
    override val userWifiPWD: String
        get() = setupWiFiPassword
    override val userWifiSSID: String
        get() = setupSSID

    override val userDisplayDevice: Display
        get() = currentDisplayDevice
    override val sendingDisplay: Display
        get() = displayToSend!!

    override val scheduleNames: ArrayList<String>
        get() = scheduleNameList

    override val organizationNames: ArrayList<String>
        get() = organizationNameList
    override val btSetupDevice: BluetoothDevice
        get() = bluetoothDevice!!
    
    override fun nextDisplayNum(): Int { //Change Sec. Risk
        if (displayList.isEmpty()){
            return 1
        }
        Log.d("DEBUG", displayList.last().displayNum.toString())
        return displayList.last().displayNum+1
    }

    private fun checkCurrentUser() {
        val user = Firebase.auth.currentUser

        if (user != null) {
            getUserProfile()
            getProviderData()
            setupSSID = sharedPref!!.getString("wifiSSID", "")!!
            setupWiFiPassword = sharedPref!!.getString("wifiPWD", "")!!
        } else {
            loginRequest()
            //val intent = Intent(this, FirebaseUIActivity::class.java)
        }
    }


    private fun loginRequest() {

        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(true)
                .setLogo(R.drawable.logo_us) // Set logo drawable
                .setTheme(R.style.AppTheme) // Set theme
                .setTosAndPrivacyPolicyUrls(
                    "https://example.com/terms.html",
                    "https://example.com/privacy.html"
                )
                .build(),
            RC_SIGN_IN
        )
    }


    private fun getUserProfile() {
        val user = Firebase.auth.currentUser
        user?.let {
            // Name, email address, and profile photo Url
            val name = user.displayName
            val email = user.email
            val photoUrl = user.photoUrl

            // Check if user's email is verified
            val emailVerified = user.isEmailVerified

            // The user's ID, unique to the Firebase project. Do NOT use this value to
            // authenticate with your backend server, if you have one. Use
            // FirebaseUser.getToken() instead.
            val uid = user.uid
        }

    }

    private fun getProviderData() {
        val user = Firebase.auth.currentUser
        user?.let {
            for (profile in it.providerData) {
                // Id of the provider (ex: google.com)
                val providerId = profile.providerId
                // UID specific to the provider
                val uid = profile.uid

                // Name, email address, and profile photo Url
                val name = profile.displayName
                val email = profile.email
                val photoUrl = profile.photoUrl
            }
        }
        /*if (user != null) {
            when (user.providerId) {
                "google.com"-> Log.d(TAG,getGoogleCredentials().toString())
                "password" -> Log.d(TAG,getEmailCredentials(user.email!!,user.password).toString())
            }
        }*/
        Log.d(TAG, user.toString())

    }
    
    private fun writeDisplay(displayToAdd: Display){
        val displayStr="display_${displayToAdd.displayNum}"
        if (displayToAdd.id!=""){
            database.child("displaysdvo").child(displayToAdd.id).setValue(displayToAdd)
        }
        else{
            database.child("displaysdvo").push().setValue(displayToAdd)
        }
        val list = mutableListOf<Map<String, Boolean>>()
        val userStr="user_1"
        list.add(mapOf(userStr to true))
        val dict= mutableMapOf<String, Boolean>(Pair(userStr, true))
        Log.d("TESTDISPLAY", dict.toString())
    }


    private fun removeDisplay(displayToRemove: Display){
        val displayStr="display_${displayToRemove.displayNum}"
        database.child("displaysdvo").child(displayToRemove.id).removeValue()
    }

    private fun adjustPermissionsDisplay(displayNum: Int, allowed: Boolean){

    }

    private fun canWriteDisplay(displayNum: Int): Boolean {
        return true
        /*if(displayNum<1) return false
        if(selfDbUser!!.userNum<1) return false
        try {
            return displayAuthList.get(displayNum - 1).get(selfDbUser!!.userNum - 1) //DO NOT HANDLE ARRAYS FFS, DO CHECK VS THE DATABASE
        }
        catch (e:Exception){
            return false
        }*/
    }

    override fun onBackPressed() {
        val drawer: DrawerLayout =findViewById(R.id.drawer_layout_tenant)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            val transaction = supportFragmentManager.beginTransaction()
            when (currentFragment) {
                "SETUP_DISPLAY_FRAGMENT" -> {
                    currentFragment = "DISPLAY_LIST_FRAGMENT"
                    val fragment2 = DisplayListFragment()
                    transaction.replace(R.id.content_main, fragment2)
                    transaction.commitAllowingStateLoss()
                }
                "SETUP_DISPLAY_ADD_FRAGMENT" -> {
                    currentFragment = "SETUP_DISPLAY_FRAGMENT"
                    val fragment2 = SetupDisplayAddFragment()
                    transaction.replace(R.id.content_main, fragment2)
                    transaction.commitAllowingStateLoss()
                }
                "DISPLAY_ADD_FRAGMENT" -> {
                    currentFragment = "DISPLAY_LIST_FRAGMENT"
                    val fragment2 = DisplayListFragment()
                    transaction.replace(R.id.content_main, fragment2)
                    transaction.commitAllowingStateLoss()
                }
                "DISPLAY_SEND_FRAGMENT" -> {
                    currentFragment = "DISPLAY_LIST_FRAGMENT"
                    val fragment2 = DisplayListFragment()
                    transaction.replace(R.id.content_main, fragment2)
                    transaction.commitAllowingStateLoss()
                }
                "DISPLAY_EDIT_FRAGMENT" -> {
                    currentFragment = "DISPLAY_LIST_FRAGMENT"
                    val fragment2 = DisplayListFragment()
                    transaction.replace(R.id.content_main, fragment2)
                    transaction.commitAllowingStateLoss()
                }
                else -> super.onBackPressed()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        //checkBluetooth();
        if (checkNetwork() == 0) {
            Toast.makeText(this, "Check your Internet Connection", Toast.LENGTH_LONG).show() // Replace context with your context instance.
            wifiDialog()
        }
    }


    @SuppressLint("ApplySharedPref")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        when (id) {
            R.id.nav_displays -> {
                currentFragment = "DISPLAY_LIST_FRAGMENT"
                val transaction = supportFragmentManager.beginTransaction()
                val fragment = DisplayListFragment()
                transaction.replace(R.id.content_main, fragment)
                transaction.commitAllowingStateLoss()

            }
            R.id.nav_logout -> {
                Firebase.auth.signOut()
                sharedPref?.edit()?.putString("wifiSSID", "")?.commit()
                sharedPref?.edit()?.putString("wifiPWD", "")?.commit()
                val intent = Intent(this, FirebaseUIActivity::class.java)
                startActivityForResult(intent, REQUEST_LOGIN)
            }
        }

        val drawer: DrawerLayout = findViewById(R.id.drawer_layout_tenant)

        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun startup() {

        val drawer: DrawerLayout
        val toggle: ActionBarDrawerToggle
        val toolbar: Toolbar
        val nameTextView: TextView

        val temp: String
        val mainLayout: Int

        mainLayout = R.layout.activity_main_tenant
        setContentView(mainLayout)
        drawer = findViewById(R.id.drawer_layout_tenant)
        navigationView = findViewById(R.id.nav_view_tenant)
        nameTextView = navigationView!!.getHeaderView(0).findViewById(R.id.name_text_view_tenant)
        val user=Firebase.auth.currentUser
        temp = if (user != null ) {
            "${Firebase.auth.currentUser!!.email}"

        } else {
            "Admin"
            //"$firstName $lastName "
        }

        toolbar= findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        Objects.requireNonNull<ActionBar>(supportActionBar).setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toggle = object : ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                val imm = Objects.requireNonNull(drawerView.context).getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(Objects.requireNonNull(drawerView).windowToken, 0)
            }
        }
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        navigationView!!.setNavigationItemSelectedListener(this)
        nameTextView.text = temp
        navigationView!!.menu.getItem(0).isCheckable = true
        navigationView!!.menu.getItem(0).isChecked = true

        val transaction = supportFragmentManager.beginTransaction()
        currentFragment = "DISPLAY_LIST_FRAGMENT"
        val fragment = DisplayListFragment()
        transaction.replace(R.id.content_main, fragment)
        transaction.commitAllowingStateLoss()

    }


    private fun checkNetwork(): Int {
        var netType = 0
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var isWifiConn = false
        var isMobileConn = false
        for (network in connMgr.allNetworks) {
            val networkInfo = connMgr.getNetworkCapabilities(network)!!
            if (networkInfo.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                isWifiConn = isWifiConn or networkInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (isWifiConn) netType += 1
            }
            if (networkInfo.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                isMobileConn = isMobileConn or networkInfo.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (isMobileConn) netType += 2
            }
        }
        Log.d(TAG, "Wifi connected: $isWifiConn")
        Log.d(TAG, "Mobile connected: $isMobileConn")
        return netType
    }

    private fun checkBluetooth() {
        Log.d("BT Check", "Starting Bluetooth Check")

        bluetoothEnabled = false
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not available.", Toast.LENGTH_LONG).show() // Replace context with your context instance.
                Log.d("BT Check", "Bluetooth not Available")
            } else {
                Log.d("BT Check", "Bluetooth  Available")
                if (!bluetoothAdapter!!.isEnabled) {
                    Log.d("BT Check", "Bluetooth  Not Enabled, enabling")

                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    Log.d("BT Check", "Bluetooth  Enabled ")
                    bluetoothEnabled = true
                }
            }
        } else {
            Toast.makeText(this, "This device has no Bluetooth.", Toast.LENGTH_LONG).show() // Replace context with your context instance.
            Log.d("BT Check", "Bluetooth does not exist")
        }

    }


    private fun wifiDialog() {
        val alertDialogBuilder = AlertDialog.Builder(
            this@MainActivity
        )

        // set title
        alertDialogBuilder.setTitle("No Network Access Found")

        // set dialog message
        alertDialogBuilder
            .setMessage("Adjust your settings and retry")
            .setCancelable(false)
            .setPositiveButton("Retry") { dialog, _ ->
                if (checkNetwork() != 0) {
                    dialog.cancel()
                    if (attemptStartup) {
                        startup()
                        attemptStartup = false
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Check your Internet Connection",
                        Toast.LENGTH_LONG
                    ).show() // Replace context with your context instance.
                    wifiDialog()
                }
            }
            .setNegativeButton("Exit") { _, _ ->
                moveTaskToBack(true)
                android.os.Process.killProcess(android.os.Process.myPid())
                System.exit(1)
            }
            .setNeutralButton("WiFi Manager") { _, _ ->
                val enableWifiIntent = Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)
                startActivity(enableWifiIntent)
            }

        alertDialogBuilder.setCancelable(false)

        // create alert dialog
        val alertDialog = alertDialogBuilder.create()

        // show it
        alertDialog.show()

    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    @SuppressLint("CheckResult")
    private fun connectDevice(device: BluetoothDevice) {
        Log.d("BT Connect", "Connecting to Device")
        Log.d("BT Connect", device.address)
        Log.d("BT Connect", device.name)

        bluetoothSerialManager!!.openSerialDevice(device)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ this.onConnected(it) }, { this.onError(it) })
    }


    private fun onConnected(connectedDevice: BluetoothSerialDevice) {
        deviceInterface = connectedDevice.toSimpleDeviceInterface()
        //deviceInterface!!.setMessageReceivedListener(this)
        deviceInterface!!.setMessageSentListener(this)
        Log.d("BT Connect", "Connected to Device")
        val endpoint=database.toString()
        Toast.makeText(applicationContext, "Connected to Device", Toast.LENGTH_LONG).show() // Replace context with your context instance.
        try {
            Handler().postDelayed({ deviceInterface!!.sendMessage("3$setupSSID\n") }, 100)
            Handler().postDelayed({ deviceInterface!!.sendMessage("4$setupWiFiPassword\n") }, 200)
            Handler().postDelayed({ deviceInterface!!.sendMessage("8\n") }, 700)
            if(setupDisplayDevice.macAddress=="F0:08:D1:D8:19:90") {
                Log.d("Fliperino","flippity")
                var disp=Display(setupDisplayDevice.macAddress)
                Handler().postDelayed({ sendMessage(disp, "BOTLEFT") }, 30000)
            }
            Log.d("Display", "Settings Updated")
            Toast.makeText(
                this,
                "Link Display Settings Updated succesfully",
                Toast.LENGTH_LONG
            ).show() // Replace context with your context instance.
            setupDisplayDevice= Display()
            bluetoothDevices=ArrayList()

        } catch (e: Exception) {
            Log.d("Seat", "Not available")
            Toast.makeText(this, "Link Display Not in Setup", Toast.LENGTH_LONG).show() // Replace context with your context instance.
        }

        currentFragment = "DISPLAY_LIST_FRAGMENT"
        val transaction = supportFragmentManager.beginTransaction()
        val fragment1 = DisplayListFragment()
        transaction.replace(R.id.content_main, fragment1)
        transaction.commitAllowingStateLoss()
    }

    override fun onMessageSent(message: String) {
        // We sent a message! Handle it here.
        Log.d("BT Connect", "Message Sent $message")

    }

    private fun onMessageReceived(message: String) {
        // We received a message! Handle it here.
        Log.d("BT Connect", "Message received $message")
    }

    private fun onError(error: Throwable) {
        Log.d("BT Connect", "Error $error")
        if (bluetoothConnectType == 1) {
            Toast.makeText(
                applicationContext,
                "Could not connect to the Paired device, looking for other Link Displays",
                Toast.LENGTH_LONG
            ).show() // Replace context with your context instance.
            bluetoothAdapter!!.startDiscovery()
        } else {
            //Toast.makeText(getApplicationContext(), "Could not connect with the Master Anchor", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_LOGIN = 1
        private const val REQUEST_ENABLE_BT = 2
        private const val RC_SIGN_IN = 123

    }
    override fun onListFragmentInteraction(item: Display) {
        displayToSend=item
        currentFragment = "DISPLAY_SEND_FRAGMENT"
        val transaction = supportFragmentManager.beginTransaction()
        val fragment1 = DisplaySendFragment()
        transaction.replace(R.id.content_main, fragment1)
        transaction.commitAllowingStateLoss()
    }
    override fun sendMessage(item: Display, messageStr: String) {
        val topic="link-display/messages/"+item.macAddress
        val server="tcp://broker.mqtt-dashboard.com:1883"
        try {
            val mqttAndroidClient = MqttAndroidClient(
                application, server, MqttClient.generateClientId()
            )
            val mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isAutomaticReconnect = true
            mqttConnectOptions.isCleanSession = false
            mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d("beep", "Success")
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.setBufferEnabled(true)
                    disconnectedBufferOptions.setBufferSize(100)
                    disconnectedBufferOptions.setPersistBuffer(false)
                    disconnectedBufferOptions.setDeleteOldestMessages(false)
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                    val message = MqttMessage()
                    message.setPayload(messageStr.toByteArray())
                    Log.d("beep", messageStr)
                    Log.d("beep", topic)
                    Log.d("beep", message.toString())
                    mqttAndroidClient.publish(topic, message)
                    mqttAndroidClient.disconnect()
                    if(messageStr=="RESTORE"){
                        Log.d("beep","RESTORING")
                        removeDisplay(item)
                        currentFragment = "DISPLAY_LIST_FRAGMENT"
                        val transaction = supportFragmentManager.beginTransaction()
                        val fragment1 = DisplayListFragment()
                        transaction.replace(R.id.content_main, fragment1)
                        transaction.commitAllowingStateLoss()
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d("beep", "Failed")
                    Log.d("beep", exception.toString())

                }
            })

        } catch (e: MqttException) {
            System.err.println("Error Publishing: " + e.toString())
            e.printStackTrace()
        }

    }


    override fun setupDisplaySystem(ssid: String, wifiPassword: String) {
        setupSSID=ssid
        setupWiFiPassword=wifiPassword
        sharedPref?.edit()?.putString("wifiSSID", setupSSID)?.commit()
        sharedPref?.edit()?.putString("wifiPWD", setupWiFiPassword)?.commit()
        bluetoothSerialManager = BluetoothSerialManager(bluetoothAdapter!!)
        checkBluetooth()
        Log.d(TAG, bluetoothEnabled.toString())
        if (bluetoothEnabled) {
            bluetoothAdapter!!.startDiscovery()
        }
    }
    override fun onCheckedChangeListener(item: Display, allowed: Boolean) {
        adjustPermissionsDisplay(item.displayNum, allowed)
    }

    override fun setupDisplayDevice(displayToAdd: Display, connect: Boolean){
        if(connect) {
            bluetoothDevice?.let { connectDevice(it) }
            writeDisplay(displayToAdd)
        }
        else{
            writeDisplay(displayToAdd)
            currentFragment = "DISPLAY_LIST_FRAGMENT"
            val transaction = supportFragmentManager.beginTransaction()
            val fragment1 = DisplayListFragment()
            transaction.replace(R.id.content_main, fragment1)
            transaction.commitAllowingStateLoss()
        }
    }


    override fun addDisplay() {
        currentFragment = "SETUP_DISPLAY_FRAGMENT"
        val transaction = supportFragmentManager.beginTransaction()
        val fragment = SetupDisplayFragment()
        transaction.replace(R.id.content_main, fragment)
        transaction.commitAllowingStateLoss()
    }

    override fun requestLogin(){
        checkCurrentUser()
    }

}
