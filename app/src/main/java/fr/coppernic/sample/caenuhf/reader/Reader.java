package fr.coppernic.sample.caenuhf.reader;

import com.caen.RFIDLibrary.CAENRFIDNotify;

import java.util.ArrayList;

import fr.coppernic.sdk.utils.core.CpcResult;

public interface Reader {

    interface Listener {
        void onReaderReady(CpcResult.RESULT res);

        void onTagsReceived(ArrayList<CAENRFIDNotify> tags);
    }

    void connectAndRead();

    void disconnect();

    boolean isConnected();
}
