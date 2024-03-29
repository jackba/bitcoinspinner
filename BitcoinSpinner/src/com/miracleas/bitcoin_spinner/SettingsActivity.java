package com.miracleas.bitcoin_spinner;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.Toast;

import com.bccapi.bitlib.crypto.KeyExporter;
import com.bccapi.bitlib.crypto.PrivateKeyRing;
import com.bccapi.bitlib.crypto.PublicKey;
import com.bccapi.bitlib.model.Address;
import com.bccapi.bitlib.model.NetworkParameters;
import com.bccapi.bitlib.util.Base58;
import com.bccapi.ng.api.Balance;

public class SettingsActivity extends PreferenceActivity {

  private SharedPreferences preferences;
  private SharedPreferences.Editor editor;

  private static final int REQUEST_CODE_SCAN = 0;

  private Context mContext;

  private Preference backupWalletPref;
  private Preference restoreWalletPref;
  private Preference exportPrivateKeyPref;
  private Preference clearPinPref;
  private Preference setPinPref;
  private EditTextPreference transactionHistorySizePref;
  private ListPreference useLocalePref;
  private ListPreference usedCurrencyPref;

  private ProgressDialog restoreDialog;

  /**
   * @see android.app.Activity#onCreate(Bundle)
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!SpinnerContext.isInitialized()) {
      SpinnerContext.initialize(this, getWindowManager().getDefaultDisplay());
    }

    preferences = getSharedPreferences(Consts.PREFS_NAME, Activity.MODE_PRIVATE);
    editor = preferences.edit();

    mContext = this;

    addPreferencesFromResource(R.xml.preferences);

    useLocalePref = (ListPreference) findPreference("useLocale");
    useLocalePref.setTitle(R.string.prefs_choose_default_locale);
    useLocalePref.setOnPreferenceChangeListener(useLocalChangeListener);

    usedCurrencyPref = (ListPreference) findPreference("usedCurrency");
    String localCurrency = preferences.getString(Consts.LOCAL_CURRENCY, Consts.DEFAULT_CURRENCY);
    usedCurrencyPref.setValue(localCurrency);
    usedCurrencyPref.setTitle(R.string.prefs_currency);
    usedCurrencyPref.setOnPreferenceChangeListener(usedCurrency);

    transactionHistorySizePref = (EditTextPreference) findPreference("transactionHistorySize");
    int transactionSize = preferences.getInt(Consts.TRANSACTION_HISTORY_SIZE, Consts.DEFAULT_TRANSACTION_HISTORY_SIZE);
    transactionHistorySizePref.setText(String.valueOf(transactionSize));
    transactionHistorySizePref.setOnPreferenceChangeListener(TransactionHistorySizeChangeListener);

    backupWalletPref = (Preference) findPreference("backupSeed");
    backupWalletPref.setOnPreferenceClickListener(backupWalletClickListener);

    restoreWalletPref = (Preference) findPreference("restoreSeed");
    restoreWalletPref.setOnPreferenceClickListener(restoreWalletClickListener);

    exportPrivateKeyPref = (Preference) findPreference("exportPrivateKey");
    exportPrivateKeyPref.setOnPreferenceClickListener(exportPrivateKeyClickListener);
    setPinPref = (Preference) findPreference("setPin");
    setPinPref.setOnPreferenceClickListener(setPinClickListener);
    clearPinPref = (Preference) findPreference("clearPin");
    clearPinPref.setOnPreferenceClickListener(clearPinClickListener);

  }

  @Override
  public void onResume() {
    super.onResume();

    if (!preferences.getString(Consts.LOCALE, "").matches("")) {
      Locale locale = new Locale(preferences.getString(Consts.LOCALE, "en"));
      Locale.setDefault(locale);
      Configuration config = new Configuration();
      config.locale = locale;
      getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }
  }

  private final OnPreferenceChangeListener useLocalChangeListener = new OnPreferenceChangeListener() {

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      editor.putString(Consts.LOCALE, (String) newValue);
      editor.commit();
      return true;
    }
  };

  private final OnPreferenceChangeListener usedCurrency = new OnPreferenceChangeListener() {

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      editor.putString(Consts.LOCAL_CURRENCY, (String) newValue);
      editor.commit();
      return true;
    }
  };

  private final OnPreferenceChangeListener TransactionHistorySizeChangeListener = new OnPreferenceChangeListener() {

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      try {
        int newSize = Integer.parseInt((String) newValue);
        if (newSize < 1) {
          newSize = Consts.DEFAULT_TRANSACTION_HISTORY_SIZE;
        }
        editor.putInt(Consts.TRANSACTION_HISTORY_SIZE, newSize);
        editor.commit();
      } catch (NumberFormatException e) {
        Toast.makeText(mContext, R.string.invalid_value, Toast.LENGTH_SHORT).show();
      }
      return true;
    }
  };

  private final OnPreferenceClickListener backupWalletClickListener = new OnPreferenceClickListener() {

    public boolean onPreferenceClick(Preference preference) {
      Utils.runPinProtectedFunction(mContext, new Runnable() {
        @Override
        public void run() {
          backupWallet();
        }
      });
      return true;
    }
  };

  private void backupWallet() {
    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    builder.setMessage(R.string.backup_dialog_text).setCancelable(false)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();

            NetworkParameters net = SpinnerContext.getInstance().getNetwork();
            final BackupInfo info = new BackupInfo(Utils.readSeed(SpinnerContext.getInstance().getApplicationContext(),
                net), net);
            info.getBackupUrl();
            Bitmap qrCode = Utils.getLargeQRCodeBitmap(info.getBackupUrl());
            Utils.showQrCode(mContext, R.string.bitcoinspinner_backup, qrCode, info.getBackupUrl());
          }
        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            // do nothing
          }
        });
    AlertDialog alertDialog = builder.create();
    alertDialog.show();
  }

  private final OnPreferenceClickListener restoreWalletClickListener = new OnPreferenceClickListener() {

    public boolean onPreferenceClick(Preference preference) {

      Balance balance = SpinnerContext.getInstance().getAsyncApi().getCachedBalance();
      if (balance != null && balance.unspent + balance.pendingChange > 0) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage(R.string.restore_dialog_coins).setCancelable(false)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                ShowRestoreWalletAlert();
              }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
              }
            });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
      } else {
        ShowRestoreWalletAlert();
      }
      return true;
    }
  };

  private void ShowRestoreWalletAlert() {
    final Activity myself = this;
    final Dialog dialog = new Dialog(mContext);
    dialog.setTitle(R.string.restore_method);
    dialog.setContentView(R.layout.dialog_restore_method);
    dialog.findViewById(R.id.btn_ok).setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        RadioButton rbQrCode = (RadioButton) dialog.findViewById(R.id.rb_qr_code);
        if (rbQrCode.isChecked()) {
          Utils.startScannerActivity(myself, REQUEST_CODE_SCAN);
        } else {
          ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
          String backupCode = "";
          if (clipboard.hasText()) {
            backupCode = clipboard.getText().toString().trim();
          }
          doRestore(backupCode);
        }
        dialog.dismiss();
      }
    });
    dialog.findViewById(R.id.btn_cancel).setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        dialog.dismiss();
      }
    });
    dialog.show();
  }

  private final OnPreferenceClickListener exportPrivateKeyClickListener = new OnPreferenceClickListener() {

    public boolean onPreferenceClick(Preference preference) {
      Utils.runPinProtectedFunction(mContext, new Runnable() {
        @Override
        public void run() {
          exportPrivateKey();
        }
      });
      return true;
    }
  };

  private void exportPrivateKey() {
    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
    builder.setMessage(R.string.export_private_key_dialog_text).setCancelable(false)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
            NetworkParameters network = SpinnerContext.getInstance().getNetwork();
            PrivateKeyRing keyRing = SpinnerContext.getInstance().getPrivateKeyRing();
            // Get the single bitcoin address that BitcoinSpinner uses
            Address address = SpinnerContext.getInstance().getAsyncApi().getPrimaryBitcoinAddress();
            // Locate the corresponding public key
            PublicKey publicKey = keyRing.findPublicKeyByAddress(address);
            // Get the key exporter from the public key reference 
            KeyExporter exporter = keyRing.findKeyExporterByPublicKey(publicKey);
            // Export the private key
            String exportedPrivateKey = exporter.getBase58EncodedPrivateKey(network);
            // Turn it into a QR code
            Bitmap qrCode = Utils.getLargeQRCodeBitmap(exportedPrivateKey);
            // Show
            Utils.showQrCode(mContext, R.string.private_key, qrCode, exportedPrivateKey);
          }
        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
          }
        });
    AlertDialog alertDialog = builder.create();
    alertDialog.show();
  }

  private final OnPreferenceClickListener setPinClickListener = new OnPreferenceClickListener() {
    public boolean onPreferenceClick(Preference preference) {
      Utils.runPinProtectedFunction(mContext, new Runnable() {
        @Override
        public void run() {
          setPin();
        }
      });
      return true;
    }
  };

  private void setPin() {
    Dialog d = new PinDialog(mContext, false, new PinDialog.OnPinEntered() {
      private String newPin = null;

      @Override
      public void pinEntered(PinDialog dialog, String pin) {
        if (newPin == null) {
          newPin = pin;
          dialog.setTitle(R.string.pin_confirm_pin);
        } else if (newPin.equals(pin)) {
          SpinnerContext.getInstance().setPin(pin);
          Toast.makeText(mContext, R.string.pin_set, Toast.LENGTH_LONG).show();
          dialog.dismiss();
        } else {
          Toast.makeText(mContext, R.string.pin_codes_dont_match, Toast.LENGTH_LONG).show();
          dialog.dismiss();
        }
      }
    });
    d.setTitle(R.string.pin_enter_new_pin);
    d.show();
  }

  private final OnPreferenceClickListener clearPinClickListener = new OnPreferenceClickListener() {
    public boolean onPreferenceClick(Preference preference) {
      Utils.runPinProtectedFunction(mContext, new Runnable() {
        @Override
        public void run() {
          SpinnerContext.getInstance().setPin("");
          Toast.makeText(mContext, R.string.pin_cleared, Toast.LENGTH_LONG).show();
        }
      });
      return true;
    }
  };

  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
    if (requestCode != REQUEST_CODE_SCAN || resultCode != RESULT_OK
        || !("QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT")))) {
      return;
    }
    String contents = intent.getStringExtra("SCAN_RESULT");
    doRestore(contents);
  }

  private void doRestore(String backupCode) {
    BackupInfo info = BackupInfo.fromString(backupCode);
    if (info == null || !info.getNetwork().equals(SpinnerContext.getInstance().getNetwork())) {
      Utils.showAlert(this, R.string.restore_invalid_qr_code);
      return;
    }
    restoreDialog = ProgressDialog.show(this, getString(R.string.restore_dialog_title),
        getString(R.string.please_wait), true);
    new AsyncInit().execute(info);
  }

  private class AsyncInit extends AsyncTask<BackupInfo, Integer, Long> {

    @Override
    protected Long doInBackground(BackupInfo... params) {
      BackupInfo info = params[0];
      SpinnerContext.getInstance().recoverWallet(info.getSeed());
      return null;
    }

    protected void onPostExecute(Long result) {
      restoreDialog.dismiss();
      Utils.showAlert(mContext, R.string.restore_complete_dialog_text);
    }
  }

  public static class BackupInfo {
    private byte[] _seed;
    private NetworkParameters _network;

    public static BackupInfo fromString(String string) {
      try {
        Uri temp = Uri.parse(string);
        final Uri uri = Uri.parse("bsb://" + temp.getSchemeSpecificPart());
        int netId = Integer.parseInt(uri.getQueryParameter("net"));
        NetworkParameters network;
        if (netId == 0) {
          network = NetworkParameters.productionNetwork;
        } else if (netId == 1) {
          network = NetworkParameters.testNetwork;
        } else {
          return null;
        }
        String host = uri.getHost();
        byte[] seed = Base58.decode(host);
        if (seed != null) {
          return new BackupInfo(seed, network);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    public BackupInfo(byte[] seed, NetworkParameters network) {
      _seed = seed;
      _network = network;
    }

    public byte[] getSeed() {
      return _seed;
    }

    public NetworkParameters getNetwork() {
      return _network;
    }

    String getBackupUrl() {
      StringBuilder sb = new StringBuilder();
      sb.append("bsb:");
      sb.append(Base58.encode(_seed));
      sb.append("?net=");
      sb.append(_network.isProdnet() ? 0 : 1);
      return sb.toString();
    }
  }

}
