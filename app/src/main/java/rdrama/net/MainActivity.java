package rdrama.net;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.artjimlop.altex.AltexImageDownloader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.pusher.pushnotifications.PushNotifications;

import static android.view.View.INVISIBLE;


public class MainActivity extends Activity {

	public class WebAppInterface {
		Context mContext;

		/** Instantiate the interface and set the context */
		WebAppInterface(Context c) {
			mContext = c;
		}

		/** Subscribe to notifications */
		@JavascriptInterface
		public void Subscribe(String uid) {
			PushNotifications.start(getApplicationContext(), "00bb6d59-7b11-4339-b1ae-b1f1259d1316");
			PushNotifications.addDeviceInterest(uid);
		}
	}

	private static final int REQUEST_CODE_LOLIPOP = 1;
	private final static int RESULT_CODE_ICE_CREAM = 2;
	private static ValueCallback<Uri[]> mFilePathCallback;
	private static String mCameraPhotoPath;
	private static ValueCallback<Uri> mUploadMessage;




	RotateAnimation rotate = new RotateAnimation(
			0, 360,
			Animation.RELATIVE_TO_SELF, 0.5f,
			Animation.RELATIVE_TO_SELF, 0.5f
	);

	String DownloadImageURL;
	String myurl = "https://rdrama.net";
	String[] supported_urls = {
		"rdrama.net",
		"www.rdrama.net",
		"old.rdrama.net",
		"rdrama.com",
		"www.rdrama.com",
		"rdrama.ga",
		"www.rdrama.ga",
		"pcmemes.net",
		"www.pcmemes.net",
		"chapotraphouse.club",
		"www.chapotraphouse.club",
		"cringetopia.org",
		"www.cringetopia.org",
		"watchpeopledie.co",
		"www.watchpeopledie.co"
	};

	private WebView mWebview;
	private ProgressBar progressBar;
	private ImageView logo;

	public static File createImageFile() throws IOException {
		// Create an image file name
		@SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		String imageFileName = "JPEG_" + timeStamp + "_";
		File storageDir = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_PICTURES);
		return File.createTempFile(
				imageFileName,  /* prefix */
				".png",		 /* suffix */
				storageDir	  /* directory */
		);
	}

	@SuppressLint({"SetJavaScriptEnabled", "SdCardPath"})
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String url_from_browser_activity = getIntent().getStringExtra("URL");

		if (url_from_browser_activity != null) {
			myurl = url_from_browser_activity;
		}

		handleIntent(getIntent());

		setContentView(R.layout.activity_main);

		int currentOrientation = getResources().getConfiguration().orientation;
		if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}

		logo = findViewById(R.id.imageView);
		progressBar = findViewById(R.id.progressBar);
		mWebview = findViewById(R.id.webView);
		mWebview.addJavascriptInterface(new WebAppInterface(this), "Android");

		mWebview.getSettings().setDomStorageEnabled(true);
		mWebview.getSettings().setAppCachePath(getCacheDir().getPath());
		mWebview.getSettings().setAppCacheEnabled(true);
		mWebview.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
		mWebview.getSettings().setAllowFileAccess(true);
		mWebview.getSettings().setAllowContentAccess(true);
		mWebview.getSettings().setAllowFileAccessFromFileURLs(true);
		mWebview.getSettings().setAllowUniversalAccessFromFileURLs(true);
		mWebview.getSettings().setDatabaseEnabled(true);
		mWebview.getSettings().setGeolocationEnabled(true);
		mWebview.getSettings().setJavaScriptEnabled(true);
		mWebview.getSettings().setBuiltInZoomControls(true);
		mWebview.getSettings().setDisplayZoomControls(false);

		if (CheckNetwork.isInternetAvailable(this))
		{
			mWebview.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
		}
		else
		{
			mWebview.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		}

		registerForContextMenu(mWebview);

		StartLoadingScreen();
		mWebview.setWebViewClient(new WebViewClient() {


			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {

				if (url == null) {
					return false;
				}
				if (!url.startsWith("http") || !url.startsWith("https")) {
					view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				}


				for (String inArray : supported_urls) {
					// This is my website, so do not override; let my WebView load the page
					if (inArray.equals(Uri.parse(url).getHost())) {
						return false;
					}

				}


				// Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
				return true;

			}

			@SuppressLint("SetTextI18n")
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

				String[] error_data = {
						String.valueOf(errorCode),
						description,
						failingUrl
				};

				mWebview.stopLoading();
				mWebview.setVisibility(INVISIBLE);

				Intent intent = new Intent(getBaseContext(), errorHandlerActivity.class);
				intent.putExtra("ERROR_DATA", error_data);
				startActivity(intent);
				finish();


			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				EndLoadingScreen();

			}
		});


		mWebview.setWebChromeClient(new WebChromeClient() {

			String TAG;

			public boolean onShowFileChooser(
					WebView webView, ValueCallback<Uri[]> filePathCallback,
					FileChooserParams fileChooserParams) {
				if (mFilePathCallback != null) {
					mFilePathCallback.onReceiveValue(null);
				}
				mFilePathCallback = filePathCallback;
				Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
					// Create the File where the photo should go
					File photoFile = null;
					try {
						photoFile = createImageFile();
						takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
					} catch (IOException ex) {
						// Error occurred while creating the File
						Log.e(TAG, "Unable to create Image File", ex);
					}
					// Continue only if the File was successfully created
					if (photoFile != null) {
						mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
						takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
								Uri.fromFile(photoFile));
					} else {
						takePictureIntent = null;
					}
				}
				Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
				contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
				contentSelectionIntent.setType("*/*");
				contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "video/*"});
				Intent[] intentArray;
				if (takePictureIntent != null) {
					intentArray = new Intent[]{takePictureIntent};
				} else {
					intentArray = new Intent[0];
				}
				Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
				chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
				chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image/Video Chooser");
				chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

				startActivityForResult(chooserIntent, REQUEST_CODE_LOLIPOP);


				return true;
			}


			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				progressBar.setVisibility(View.VISIBLE);
				if (newProgress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
					progressBar.setVisibility(ProgressBar.VISIBLE);

				}

				progressBar.setProgress(newProgress);
				if (newProgress == 100) {
					progressBar.setVisibility(ProgressBar.GONE);


				}
				if (newProgress >= 80) {
					EndLoadingScreen();
				}
				super.onProgressChanged(view, newProgress);
			}
		});

		if (getIntent().getStringExtra("url") != null && getIntent().getStringExtra("url").startsWith("/")) {
			myurl += getIntent().getStringExtra("url");
		}
		
		mWebview.loadUrl(myurl.replace("http://","https://"));
	}

	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
		String appLinkAction = intent.getAction();
		Uri appLinkData = intent.getData();
		if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null) {
			myurl = appLinkData.toString();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (mWebview.canGoBack()) {
					mWebview.goBack();
				} else {

					new AlertDialog.Builder(this)
							.setTitle("You are about to close the application")
							.setMessage("You cannot go back any further, would you like to close?")
							.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									finish();
								}
							})
							.setNegativeButton("No", null)
							.show();
				}
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	void StartLoadingScreen() {
		mWebview.setVisibility(INVISIBLE);
		double logo_rotation_speed = 1.0;
		rotate.setDuration(((long) (1000 / logo_rotation_speed)));
		rotate.setRepeatCount(Animation.INFINITE);
		logo.startAnimation(rotate);

	}

	void EndLoadingScreen() {
		mWebview.setVisibility(View.VISIBLE);
		rotate.cancel();
		logo.setVisibility(View.INVISIBLE);
		logo.setVisibility(View.GONE);
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case RESULT_CODE_ICE_CREAM:
				Uri uri = null;
				if (data != null) {
					uri = data.getData();
				}
				mUploadMessage.onReceiveValue(uri);
				mUploadMessage = null;
				break;
			case REQUEST_CODE_LOLIPOP:
				Uri[] results = null;
				// Check that the response is a good one
				if (resultCode == Activity.RESULT_OK) {
					if (data == null) {
						// If there is not data, then we may have taken a photo
						if (mCameraPhotoPath != null) {
							results = new Uri[]{Uri.parse(mCameraPhotoPath)};
						}
					} else {
						String dataString = data.getDataString();
						if (dataString != null) {
							results = new Uri[]{Uri.parse(dataString)};
						}
					}
				}
				mFilePathCallback.onReceiveValue(results);
				mFilePathCallback = null;
				break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu contextMenu, View view,
									ContextMenu.ContextMenuInfo contextMenuInfo) {
		super.onCreateContextMenu(contextMenu, view, contextMenuInfo);

		final WebView.HitTestResult webViewHitTestResult = mWebview.getHitTestResult();

		if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
				webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

			contextMenu.setHeaderTitle("Download Image From Below");

			contextMenu.add(0, 1, 0, "Save - Download Image")
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem menuItem) {
							DownloadImageURL = webViewHitTestResult.getExtra();
							int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

							if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

								ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
							} else {
								downloadImage();
							}


							return false;


						}
					});
			contextMenu.add(0, 2, 1, "Share post")
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							String DownloadImageURL = webViewHitTestResult.getExtra();

							Intent myIntent = new Intent(Intent.ACTION_SEND);
							myIntent.setType("text/plain");
							myIntent.putExtra(Intent.EXTRA_TEXT, DownloadImageURL);
							startActivity(Intent.createChooser(myIntent, "Share Using"));
							return false;
						}
					});
		}

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case 1:
				if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
					downloadImage();
				}
				break;

			default:
				break;
		}
	}

	public void downloadImage() {
		if (URLUtil.isValidUrl(DownloadImageURL)) {
			AltexImageDownloader.writeToDisk(getApplicationContext(), DownloadImageURL, "rdrama");


							   /* DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
								request.setDestinationInExternalFilesDir(getApplicationContext(), "dir", "rqs-" + Calendar.getInstance().getTime() + ".png");
								request.allowScanningByMediaScanner();


								request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
								DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

								downloadManager.enqueue(request);*/

			Toast.makeText(MainActivity.this, "Image Downloaded Successfully.", Toast.LENGTH_LONG).show();
		} else {
			Toast.makeText(MainActivity.this, "Sorry.. Something Went Wrong.", Toast.LENGTH_LONG).show();
		}
	}

	static class CheckNetwork {
		private static final String TAG = CheckNetwork.class.getSimpleName();

		public static boolean isInternetAvailable(Context context) {
			NetworkInfo info = ((ConnectivityManager)
					context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
			if (info == null) {
				Log.d(TAG, "no internet connection");
				return false;
			} else {
				if (info.isConnected()) {
					Log.d(TAG, " internet connection available...");
				} else {
					Log.d(TAG, " internet connection");
				}
				return true;
			}

		}
	}
}
