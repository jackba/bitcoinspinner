package com.miracleas.bitcoin_spinner_testnet;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bccapi.api.Network;
import com.miracleas.bitcoin_spinner_lib.Consts;
import com.miracleas.bitcoin_spinner_lib.StartUpActivity;

public class Main extends Activity {
	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Intent i = new Intent(this, StartUpActivity.class);
        i.putExtra(Consts.EXTRA_NETWORK, Network.testNetwork);
        startActivity(i);
        finish();
	}
}
