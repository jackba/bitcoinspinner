package com.miracleas.bitcoin_spinner;

import java.net.URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.Display;

import com.bccapi.api.Network;
import com.bccapi.core.BitcoinClientApiImpl;
import com.bccapi.core.ECKeyManager;
import com.bccapi.core.Asynchronous.AsynchronousAccount;

public class SpinnerContext {

  private static final String NETWORK_USED = "network used";
  private static final String PIN = "pin";
  private static final String PUBLIC_KEY_REGISTERED = "pub key registered";
  private static final String BACKUP_WARNING_DISABLED = "backup warning disabled";

  private static SpinnerContext _spinnerContext;

  private Network _network;
  private Context _applicationContext;
  private AsynchronousAccount _account;
  private int _displayWidth; // Total width of the device display in pixels
  private int _displayHeight; // Total height of the device display in pixels

  public static boolean isInitialized() {
    return _spinnerContext != null;
  }

  public static void initialize(Context context, Display display, Network network) {
    context = context.getApplicationContext();
    SharedPreferences preferences = context.getSharedPreferences(Consts.PREFS_NAME, Context.MODE_PRIVATE);
    preferences.edit().putInt(NETWORK_USED, network.getStandardAddressHeader()).commit();
    _spinnerContext = new SpinnerContext(context, display, network);
    Utils.getPrimaryAddressAsSmallQrCode(_spinnerContext.getAccount());
  }

  public static void initialize(Context context, Display display) {
    context = context.getApplicationContext();
    SharedPreferences preferences = context.getSharedPreferences(Consts.PREFS_NAME, Context.MODE_PRIVATE);
    int net = preferences.getInt(NETWORK_USED, -1);
    Network network;
    if (net == Network.productionNetwork.getStandardAddressHeader()) {
      network = Network.productionNetwork;
    } else if (net == Network.testNetwork.getStandardAddressHeader()) {
      network = Network.testNetwork;
    } else {
      throw new RuntimeException("Network never initialized");
    }
    _spinnerContext = new SpinnerContext(context, display, network);
    Utils.getPrimaryAddressAsSmallQrCode(_spinnerContext.getAccount());
  }

  public static SpinnerContext getInstance() {
    if (!isInitialized()) {
      throw new RuntimeException("BitcoinSpinner not initialized");
    }
    return _spinnerContext;
  }

  private SpinnerContext(Context context, Display display, Network network) {
    _applicationContext = context.getApplicationContext();
    _network = network;
    DisplayMetrics dm = new DisplayMetrics();
    display.getMetrics(dm);
    _displayWidth = dm.widthPixels;
    _displayHeight = dm.heightPixels;
    URL url = Utils.getBccapiUrl(_network);
    ECKeyManager keyManager = new AndroidKeyManager(_applicationContext, _network);
    BitcoinClientApiImpl api = new BitcoinClientApiImpl(url, _network);
    _account = new AndroidAccount(keyManager, api, _applicationContext);
  }

  public void recoverWallet(byte[] seed) {
    URL url = Utils.getBccapiUrl(_network);
    ECKeyManager keyManager = new AndroidKeyManager(_applicationContext, _network, seed);
    BitcoinClientApiImpl api = new BitcoinClientApiImpl(url, _network);
    _account = new AndroidAccount(keyManager, api, _applicationContext);
  }

  public Network getNetwork() {
    return _network;
  }

  public boolean isPinProtected() {
    return getPin().length() > 0;
  }

  public boolean isPublicKeyRegistered() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    return preferences.getBoolean(PUBLIC_KEY_REGISTERED, false);
  }

  public void markPublicKeyRegistered() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    if (!preferences.getBoolean(PUBLIC_KEY_REGISTERED, false)) {
      preferences.edit().putBoolean(PUBLIC_KEY_REGISTERED, true).commit();
    }
  }

  public boolean isBackupWarningDisabled() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    return preferences.getBoolean(BACKUP_WARNING_DISABLED, false);
  }

  public void markBackupWarningDisabled() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    if (!preferences.getBoolean(BACKUP_WARNING_DISABLED, false)) {
      preferences.edit().putBoolean(BACKUP_WARNING_DISABLED, true).commit();
    }
  }

  public String getPin() {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    return preferences.getString(PIN, "");
  }

  public void setPin(String pin) {
    SharedPreferences preferences = getApplicationContext().getSharedPreferences(Consts.PREFS_NAME,
        Context.MODE_PRIVATE);
    preferences.edit().putString(PIN, pin).commit();
  }

  public Context getApplicationContext() {
    return _applicationContext;
  }

  public AsynchronousAccount getAccount() {
    return _account;
  }

  public int getDisplayWidth() {
    return _displayWidth;
  }

  public int getDisplayHeight() {
    return _displayHeight;
  }

}
