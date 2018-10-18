package fr.coppernic.sample.caenuhf.reader;

import com.caen.RFIDLibrary.CAENRFIDNotify;

import fr.coppernic.sdk.utils.core.CpcBytes;

public class Tag {

    private String epc;
    private int count;

    public Tag(CAENRFIDNotify tag) {
        epc = CpcBytes.byteArrayToString(tag.getTagID()).replaceAll(" ","");
        count = 1;
    }

    public String getEpc() {
        return epc;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Tag) {
            Tag toCompare = (Tag) obj;
            return this.epc.equals(toCompare.epc);
        }
        return false;
    }

    public int getCount() {
        return count;
    }

    public void incrementCount() {
        count++;
    }
}
