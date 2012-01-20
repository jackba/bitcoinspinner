package com.miracleas.bitcoin_spinner_lib;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Hashtable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.bccapi.api.Network;
import com.bccapi.core.HashUtils;
import com.bccapi.core.Asynchronous.AsynchronousAccount;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class Utils {

	private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

	public static void showConnectionAlert(Context context) {
		if (isConnected(context)) {
			showAlert(context, R.string.unexpected_error);
		} else {
			showAlert(context, R.string.no_network);
		}
	}

	public static void showAlert(Context context, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(message).setCancelable(false).setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
			}
		});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	public static void showAlert(Context context, int messageId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(messageId).setCancelable(false)
				.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				});
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	public static void showNoNetworkTip(Context context) {
		Toast.makeText(context, R.string.no_network, Toast.LENGTH_SHORT).show();
	}

	public static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] NI = cm.getAllNetworkInfo();
		for (int i = 0; i < NI.length; i++) {
			if (NI[i].isConnected()) {
				return true;
			}
		}
		return false;
	}

	private static String _lastQrAddressStringSmall;
	private static Bitmap _lastQrAddressBitmapSmall;

	public static synchronized Bitmap getPrimaryAddressAsSmallQrCode(AsynchronousAccount account) {
		String address = account.getPrimaryBitcoinAddress();
		if (address.equals(_lastQrAddressStringSmall)) {
			return _lastQrAddressBitmapSmall;
		}
		_lastQrAddressStringSmall = address;
		_lastQrAddressBitmapSmall = getSmallQRCodeBitmap("bitcoin:" + address);
		return _lastQrAddressBitmapSmall;
	}

	private static String _lastQrAddressStringLarge;
	private static Bitmap _lastQrAddressBitmapLarge;

	public static synchronized Bitmap getPrimaryAddressAsLargeQrCode(AsynchronousAccount account) {
		String address = account.getPrimaryBitcoinAddress();
		if (address.equals(_lastQrAddressStringLarge)) {
			return _lastQrAddressBitmapLarge;
		}
		_lastQrAddressStringLarge = address;
		_lastQrAddressBitmapLarge = getLargeQRCodeBitmap("bitcoin:" + address);
		return _lastQrAddressBitmapLarge;
	}

	public static Bitmap getLargeQRCodeBitmap(final String url) {
		// make size 85% of display size
		int size = Math.min(Consts.displayWidth, Consts.displayHeight) * 85 / 100;
		return getQRCodeBitmapX(url, size);
	}

	public static Bitmap getSmallQRCodeBitmap(final String url) {
		// make size 34% of display size
		int size = Math.min(Consts.displayWidth, Consts.displayHeight) * 34 / 100;
		return getQRCodeBitmapX(url, size);
	}

	private static Bitmap getQRCodeBitmapX(final String url, final int size) {
		try {
			final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			final BitMatrix result = QR_CODE_WRITER.encode(url, BarcodeFormat.QR_CODE, size, size, hints);

			final int width = result.getWidth();
			final int height = result.getHeight();
			final int[] pixels = new int[width * height];

			for (int y = 0; y < height; y++) {
				final int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
				}
			}

			final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			return bitmap;
		} catch (final WriterException x) {
			x.printStackTrace();
			return null;
		}
	}

	public static URL getBccapiUrl(Network network) {
		try {
			if (network.equals(Network.testNetwork)) {
				return Consts.USE_CLOSED_TESTNET ? new URL("https://testnet.bccapi.com:445") : new URL(
						"https://testnet.bccapi.com:444");
			}
			return new URL("https://prodnet.bccapi.com:443");
		} catch (Exception e) {
			// never happens
			return null;
		}
	}

	private static String getSeedFileName(Network network) {
		if (network.equals(Network.testNetwork)) {
			return Consts.USE_CLOSED_TESTNET ? Consts.CLOSED_TESTNET_FILE : Consts.TESTNET_FILE;
		}
		return Consts.PRODNET_FILE;
	}

	public static byte[] readSeed(Network network) {
		String seedFile = getSeedFileName(network);
		byte[] seed = new byte[Consts.SEED_SIZE];
		FileInputStream fis;
		try {
			fis = Consts.applicationContext.openFileInput(seedFile);
			fis.read(seed);
			fis.close();
			return seed;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean writeSeed(Network network, byte[] seed) {
		String seedFile = getSeedFileName(network);
		FileOutputStream fos = null;
		try {
			fos = Consts.applicationContext.openFileOutput(seedFile, Context.MODE_PRIVATE);
			fos.write(seed);
			fos.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public static byte[] createandWriteSeed(Network network) {
		try {
			SecureRandom random = new SecureRandom();
			byte genseed[] = random.generateSeed(Consts.SEED_GEN_SIZE);
			byte[] seed = HashUtils.sha256(genseed);
			if (writeSeed(network, seed)) {
				return seed;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}