package fr.coppernic.sample.caenuhf.ui

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caen.RFIDLibrary.CAENRFIDNotify
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.coppernic.sample.caenuhf.R
import fr.coppernic.sample.caenuhf.reader.Adapter
import fr.coppernic.sample.caenuhf.reader.CaenReader
import fr.coppernic.sample.caenuhf.reader.Reader
import fr.coppernic.sample.caenuhf.reader.Tag
import fr.coppernic.sample.caenuhf.settings.SettingsActivity
import fr.coppernic.sdk.power.PowerManager
import fr.coppernic.sdk.power.api.PowerListener
import fr.coppernic.sdk.power.api.peripheral.Peripheral
import fr.coppernic.sdk.power.impl.access.AccessPeripheral
import fr.coppernic.sdk.power.impl.cone.ConePeripheral
import fr.coppernic.sdk.utils.core.CpcBytes
import fr.coppernic.sdk.utils.core.CpcResult.RESULT
import fr.coppernic.sdk.utils.helpers.OsHelper
import fr.coppernic.sdk.utils.helpers.UsbHelper.ACTION_USB_PERMISSION
import fr.coppernic.sdk.utils.sound.Sound
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_CODE_PERMISSIONS = 100
    }

    private var tvMessage: TextView? = null
    private var fab: FloatingActionButton? = null
    private var adapter: RecyclerView.Adapter<*>? = null
    private var reader: Reader? = null
    private var tags: MutableList<Tag>? = mutableListOf()

    private val caenPeripheral: Peripheral?
        get() = if (OsHelper.isCone()) ConePeripheral.RFID_CAEN_R1270C_GPIO
        else if (OsHelper.isAccess()) AccessPeripheral.RFID_CAEN_R1270C_GPIO
        else null

    //region Listeners
    private val readerListener: Reader.Listener = object : Reader.Listener {
        override fun onReaderReady(res: RESULT) {
            stopProgress()
            if (res != RESULT.OK) { //init reader failed
                showMessage(getString(R.string.UHF_opened_error))
                fab?.setImageResource(R.drawable.ic_nfc_white_24dp)
            } else {
                showMessage("Reader is ready")
            }
        }

        override fun onTagsReceived(tags: ArrayList<CAENRFIDNotify>) {
            runOnUiThread {
                beepFunction()
                updateViews(tags)
            }
        }
    }
    private val powerListener: PowerListener = object : PowerListener {
        override fun onPowerUp(result: RESULT, peripheral: Peripheral) {
            if (result == RESULT.OK) {
                Timber.d("RFID reader powered on")
            } else {
                showMessage(getString(R.string.error_power_on))
            }
        }
        override fun onPowerDown(result: RESULT, peripheral: Peripheral) {
            Timber.d("RFID reader powered off")
        }
    }
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }
    }
    //endregion

    //region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        reader = CaenReader(this, readerListener)

        // FAB
        fab = findViewById(R.id.fab)
        fab?.setOnClickListener {
            reader?.apply {
                if (!isConnected) {
                    fab?.setImageResource(R.drawable.ic_stop_white_24dp)
                    clearRecyclerView()
                    connectAndRead()
                } else {
                    disconnect()
                    fab?.setImageResource(R.drawable.ic_nfc_white_24dp)
                    // stopProgress();
                    showMessage(getString(R.string.press_RFID_button))
                }
            }
        }

        // Message
        tvMessage = findViewById(R.id.tvMessage)

        // RecycleView
        val recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view)
        val layoutManager = LinearLayoutManager(this)
        val itemDecoration = DividerItemDecoration(this, layoutManager.orientation)
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        tags = ArrayList()
        // specify an adapter (see also next example)
        adapter = Adapter(tags)
        recyclerView.addItemDecoration(itemDecoration)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        getUsbPermissions(this.applicationContext)
    }

    override fun onStart() {
        super.onStart()
        Timber.d("onStart")

        showFAB(false)
        PowerManager.get().registerListener(powerListener)
        powerOn(true)

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)
    }

    override fun onStop() {
        Timber.d("onStop")
        if (reader?.isConnected == true) {
            reader?.disconnect()
        }
        powerOn(false)
        PowerManager.get().unregisterListener(powerListener)
        stopProgress()

        unregisterReceiver(usbReceiver)

        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear -> clearRecyclerView()
            R.id.action_settings -> showSettings()
        }
        return super.onOptionsItemSelected(item)
    }
    // endregion


    // region Methods
    fun updateViews(data: ArrayList<CAENRFIDNotify>) {
        for (tag in data) {
            Timber.d("tag received : %s", CpcBytes.byteArrayToString(tag.tagID))
            val currentTag = Tag(tag)
            val position = tags!!.indexOf(currentTag)
            if (position < 0) { //new tag
                Timber.d("Not in the list")
                tags?.add(0, currentTag)
                // Notify the adapter that an item was inserted at position 0
                adapter?.notifyItemInserted(0)
                Timber.d("size %s", adapter!!.itemCount)
            } else {
                tags?.get(position)?.incrementCount()
                // Notify the adapter that an item was changed
                adapter?.notifyItemChanged(position)
            }
        }
    }

    private fun clearRecyclerView() {
        val size = tags?.size ?: 0
        if (size > 0) {
            for (i in 0 until size) {
                tags?.removeAt(0)
            }
            adapter?.notifyItemRangeRemoved(0, size)
        }
    }

    private fun powerOn(on: Boolean) {
        caenPeripheral?.let {
            if (on) {
                it.on(this)
                showFAB(true)
            } else {
                it.off(this)
                showFAB(false)
                SystemClock.sleep(500)
            }
        } ?: run {
            // Not an Coppernic device (example Crosscall or HMD : maybe auto-powered by default)
            showFAB(true)
        }
    }

    fun stopProgress() {
        fab?.isEnabled = true
    }

    fun showMessage(value: String?) {
        tvMessage?.text = value
    }

    private fun showFAB(value: Boolean) {
        with(fab) {
            if (value) {
                this?.show()
                showMessage(getString(R.string.press_RFID_button))
                this?.setImageResource(R.drawable.ic_nfc_white_24dp)
            } else {
                showMessage(getString(R.string.wait_RFID_powered))
                this?.hide()
            }
        }

    }

    private fun showSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }

    private fun beepFunction() {
        val sound = Sound(this)
        sound.playOk(250) { this.callback() }
    }

    private fun callback() {
        //do something here
        return Unit
    }

    private fun getUsbPermissions(context: Context) {
        val manager: UsbManager = context.getSystemService(USB_SERVICE) as UsbManager
        val pIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
        val deviceList: HashMap<String, UsbDevice> = manager.deviceList
        deviceList.values.takeIf { it.isNotEmpty() }?.forEach { device ->
            manager.requestPermission(device, pIntent)
        } ?: run {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
    // endregion
}
