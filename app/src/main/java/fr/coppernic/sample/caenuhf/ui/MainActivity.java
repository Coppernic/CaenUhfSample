package fr.coppernic.sample.caenuhf.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.caen.RFIDLibrary.CAENRFIDNotify;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import fr.coppernic.sample.caenuhf.R;
import fr.coppernic.sample.caenuhf.reader.Adapter;
import fr.coppernic.sample.caenuhf.reader.CaenReader;
import fr.coppernic.sample.caenuhf.reader.Reader;
import fr.coppernic.sample.caenuhf.reader.Tag;
import fr.coppernic.sample.caenuhf.settings.SettingsActivity;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.access.AccessPeripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.core.CpcResult;
import fr.coppernic.sdk.utils.helpers.OsHelper;
import fr.coppernic.sdk.utils.sound.Sound;
import kotlin.Unit;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private TextView tvMessage;
    private FloatingActionButton fab;
    private RecyclerView.Adapter adapter;

    private Reader UHFreader;

    ArrayList<Tag> tags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!UHFreader.isConnected()) {
                    clearRecyclerView();
                    UHFreader.connectAndRead();
                    fab.setImageResource(R.drawable.ic_stop_white_24dp);
                } else {
                    UHFreader.disconnect();
                    fab.setImageResource(R.drawable.ic_nfc_white_24dp);
                   // stopProgress();
                    showMessage(getString(R.string.press_RFID_button));
                }
            }
        });

        tvMessage = findViewById(R.id.tvMessage);

        RecyclerView recyclerView = findViewById(R.id.my_recycler_view);
        // use a linear layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, layoutManager.getOrientation());

        UHFreader = new CaenReader(this, UHFreaderListener);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        recyclerView.setLayoutManager(layoutManager);

        tags = new ArrayList<>();
        // specify an adapter (see also next example)
        adapter = new Adapter(tags);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    protected void onStart() {
        Timber.d("onStart");
        super.onStart();
        showFAB(false);
        PowerManager.get().registerListener(powerListener);
        powerOn(true);
    }

    @Override
    protected void onStop() {
        Timber.d("onStop");
        if(UHFreader.isConnected()){
            UHFreader.disconnect();
        }
        powerOn(false);
        PowerManager.get().unregisterListener(powerListener);
        stopProgress();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_clear:
                clearRecyclerView();
                break;
            case R.id.action_settings:
                showSettings();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private Reader.Listener UHFreaderListener = new Reader.Listener() {
        @Override
        public void onReaderReady(CpcResult.RESULT res) {
            stopProgress();
            if (res != CpcResult.RESULT.OK) {//init reader failed
                showMessage(getString(R.string.UHF_opened_error));
            } else {
                showMessage("Reading...");
            }
        }

        @Override
        public void onTagsReceived(final ArrayList<CAENRFIDNotify> tags) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beepFunction();
                    updateViews(tags);
                }
            });

        }
    };

    void updateViews(ArrayList<CAENRFIDNotify> data) {
        for (CAENRFIDNotify tag : data) {
            Timber.d("tag received : %s", CpcBytes.byteArrayToString(tag.getTagID()));
            Tag currentTag = new Tag(tag);
            int position = tags.indexOf(currentTag);
                if (position < 0) { //new tag
                    Timber.d("Not in the list");
                    tags.add(0, currentTag);
                    // Notify the adapter that an item was inserted at position 0
                    adapter.notifyItemInserted(0);
                    Timber.d("size %s", adapter.getItemCount());
                } else{
                    tags.get(position).incrementCount();
                    // Notify the adapter that an item was changed
                    adapter.notifyItemChanged(position);
                }
        }
    }

    void clearRecyclerView() {
        final int size = tags.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                tags.remove(0);
            }
            adapter.notifyItemRangeRemoved(0, size);
        }
    }

    private final PowerListener powerListener = new PowerListener() {
        @Override
        public void onPowerUp(CpcResult.RESULT result, Peripheral peripheral) {
            if (result == CpcResult.RESULT.OK) {
                Timber.d("RFID reader powered on");
                showFAB(true);
            } else {
                showMessage(getString(R.string.error_power_on));
            }
        }

        @Override
        public void onPowerDown(CpcResult.RESULT result, Peripheral peripheral) {
            Timber.d("RFID reader powered off");
        }
    };

    private Peripheral getCaenPeripheral() {
        if (OsHelper.isCone())
            return ConePeripheral.RFID_CAEN_R1270C_GPIO;
        else if (OsHelper.isAccess())
            // TODO
            // return AccessPeripheral.RFID_GENERIC_GPIO;
            return AccessPeripheral.RFID_ASK_UCM108_GPIO;
        else
            return null;
    }

    private void powerOn(boolean on) {
        Peripheral peripheral = getCaenPeripheral();
        if (on) {
            peripheral.on(this);
        } else {
            peripheral.off(this);
            SystemClock.sleep(500);
        }
    }

    public void stopProgress() {
        fab.setEnabled(true);
    }

    public void showMessage(String value) {
        tvMessage.setText(value);
    }

    public void showFAB(boolean value) {
        if (value) {
            fab.show();
            showMessage(getString(R.string.press_RFID_button));
            fab.setImageResource(R.drawable.ic_nfc_white_24dp);
        } else {
            showMessage(getString(R.string.wait_RFID_powered));
            fab.hide();
        }
    }

    private void showSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void beepFunction() {
        Sound sound = new Sound(this);
        sound.playOk(250, this::callback);
    }

    private Unit callback() {
        //do something here
        return Unit.INSTANCE;
    }



}
