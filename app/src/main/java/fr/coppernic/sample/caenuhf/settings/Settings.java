package fr.coppernic.sample.caenuhf.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by michael on 30/01/18.
 */

public class Settings {
    private Context context;
    private SharedPreferences sharedPreferences;

    static final String KEY_UHF_POWER = "key_uhf_power";
    static final String KEY_UHF_Q = "key_uhf_q";
    static final String KEY_UHF_SESSION = "key_uhf_session";
    static private final String DEF_POWER = "100";
    static private final String DEF_Q = "6";
    static private final String DEF_SESSION = "S0";

    public Settings(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getPower() {
        return Integer.parseInt(sharedPreferences.getString(KEY_UHF_POWER, DEF_POWER));
    }

    public int getQ() {
        return Integer.parseInt(sharedPreferences.getString(KEY_UHF_Q, DEF_Q));
    }

    public String getSession() {
        return sharedPreferences.getString(KEY_UHF_SESSION, DEF_SESSION);
    }
}
