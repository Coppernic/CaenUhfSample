package fr.coppernic.sample.caenuhf.reader;

import android.content.Context;

import com.caen.RFIDLibrary.CAENRFIDEvent;
import com.caen.RFIDLibrary.CAENRFIDEventListener;
import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDLogicalSource;
import com.caen.RFIDLibrary.CAENRFIDLogicalSourceConstants;
import com.caen.RFIDLibrary.CAENRFIDNotify;
import com.caen.RFIDLibrary.CAENRFIDPort;
import com.caen.RFIDLibrary.CAENRFIDReader;
import com.caen.RFIDLibrary.OnGetReaderInstanceListener;

import java.util.ArrayList;

import fr.coppernic.sample.caenuhf.settings.Settings;
import fr.coppernic.sdk.core.Defines;
import fr.coppernic.sdk.utils.core.CpcResult;
import timber.log.Timber;

public class CaenReader implements Reader {

    private static final String CAEN_READER_PORT = Defines.SerialDefines.ASK_READER_PORT;
    private Reader.Listener listener;

    private CAENRFIDReader reader;
    private Settings settings;
    private boolean isConnected = false;

    public CaenReader(Context context, final Listener listener) {
        this.listener = listener;
        if (reader == null) {
            CAENRFIDReader.getInstance(context, new OnGetReaderInstanceListener() {
                @Override
                public void OnGetReaderInstance(CAENRFIDReader caenrfidReader) {
                    reader = caenrfidReader;
                    listener.onReaderReady(CpcResult.RESULT.OK);
                }
            });
        }
        settings = new Settings(context);
    }

    private CpcResult.RESULT init() {
        CpcResult.RESULT res = CpcResult.RESULT.NOT_INITIALIZED;
        try {
            Timber.d("SetSession");
            String session = settings.getSession();
            setSession(session);
        } catch (CAENRFIDException e) {
            e.printStackTrace();
            return res.setCause(e);
        }
        try {
            int power = settings.getPower();
            Timber.d("SetPower " + power + "mW");
            reader.SetPower(power);
        } catch (CAENRFIDException e) {
            e.printStackTrace();
            return res.setCause(e);
        }
        return CpcResult.RESULT.OK;
    }

    private CpcResult.RESULT connect() {
        try {
            reader.Connect(CAENRFIDPort.CAENRFID_RS232, CAEN_READER_PORT);
            CpcResult.RESULT res = init();
            if (res != CpcResult.RESULT.OK) {
                Timber.d("Connection Error !!!");
                return res;
            } else {
                isConnected = true;
                Timber.d("Connection OK");
                return res;
            }
        } catch (CAENRFIDException e) {
            e.printStackTrace();
            return CpcResult.RESULT.CONNECTION_ERROR;
        }
    }

    private void inventoryRead() {
        try {
            try {
                reader.removeCAENRFIDEventListener(caenrfidEventListener);
                reader.InventoryAbort();
            } catch (Exception e) {
                Timber.d("Exception stop inventory : " + e.getMessage());
            }
            Timber.d("Start inventory");
            CAENRFIDLogicalSource source = reader.GetSource("Source_0");
            source.SetReadCycle(0);
            reader.addCAENRFIDEventListener(caenrfidEventListener);
            source.SetSelected_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_All_SELECTED);
            source.EventInventoryTag(new byte[0], (short) 0x0, (short) 0x0, (short) 0x06);
        } catch (CAENRFIDException e) {
            e.printStackTrace();
        }

    }

    private void setSession(String sessionNumber) throws CAENRFIDException {
        Timber.d("SetSession " + sessionNumber);
        CAENRFIDLogicalSource source = reader.GetSource("Source_0");
        switch (sessionNumber) {
            case "S0":
                source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S0);
                break;
            case "S1":
                source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S1);
                break;
            case "S2":
                source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S2);
                break;
            case "S3":
                source.SetSession_EPC_C1G2(CAENRFIDLogicalSourceConstants.EPC_C1G2_SESSION_S3);
                break;
        }
    }

    @Override
    public void connectAndRead() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final CpcResult.RESULT res = connect();
                listener.onReaderReady(res);
                inventoryRead();
            }
        }).run();

    }

    @Override
    public void disconnect() {
        try {
            Timber.d("Stop inventory and disconnect reader");
            reader.removeCAENRFIDEventListener(caenrfidEventListener);
            reader.InventoryAbort();
            reader.Disconnect();
        } catch (CAENRFIDException e) {
            e.printStackTrace();
        }
        isConnected = false;

    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    private CAENRFIDEventListener caenrfidEventListener = new CAENRFIDEventListener() {
        @Override
        public void CAENRFIDTagNotify(CAENRFIDEvent caenrfidEvent) {
            Timber.d("Tag received!!");
            ArrayList<CAENRFIDNotify> tags = caenrfidEvent.getData();
            listener.onTagsReceived(tags);
        }
    };

}
