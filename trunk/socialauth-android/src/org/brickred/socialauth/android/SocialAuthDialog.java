/*
 ===========================================================================
 Copyright (c) 2012 Three Pillar Global Inc. http://threepillarglobal.com

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sub-license, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ===========================================================================
 */

package org.brickred.socialauth.android;

import java.util.Map;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.brickred.socialauth.SocialAuthManager;
import org.brickred.socialauth.android.SocialAuthAdapter.Provider;

/**
 * Dialog that wraps a Web view for authenticating with the given
 * social network. All the OAuth redirection happens over here and 
 * the success and failure are handed over to the listener
 * 
 * @author vineeta@brickred.com
 * @author abhinavm@brickred.com
 *
 */
public class SocialAuthDialog extends Dialog {

	// Variables
	static final int BLUE = 0xFF6D84B4;
	static final int MARGIN = 4;
	static final int PADDING = 2;
	
	public static float width = 40;
	public static float height = 60;

	public static final float[] DIMENSIONS_DIFF_LANDSCAPE = { width, height };
	public static final float[] DIMENSIONS_DIFF_PORTRAIT = { width, height };

	static final String DISPLAY_STRING = "touch";

	private String mUrl;
	
	// Android Components
	private TextView mTitle;
	private DialogListener mListener;
	private ProgressDialog mSpinner;
	private CustomWebView mWebView;
	private LinearLayout mContent;
	private Drawable icon;
	private Handler handler;
	static final FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.FILL_PARENT);

	// SocialAuth Components
	private SocialAuthManager mSocialAuthManager;
	private Provider mProviderName;
	
	/**
	 * Constructor for the dialog
	 * @param context Parent component that opened this dialog
	 * @param url URL that will be used for authenticating
	 * @param providerName Name of provider that is being authenticated
	 * @param listener Listener object to handle events
	 * @param socialAuthManager Underlying SocialAuth framework for OAuth
	 */
	public SocialAuthDialog(Context context, String url, Provider providerName, DialogListener listener,
			SocialAuthManager socialAuthManager) {
		super(context);
		mProviderName = providerName;
		mUrl = url;
		mListener = listener;
		mSocialAuthManager = socialAuthManager;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
		mSpinner = new ProgressDialog(getContext());
		mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSpinner.setMessage("Loading...");

		mContent = new LinearLayout(getContext());
		mContent.setOrientation(LinearLayout.VERTICAL);
		setUpTitle();
		setUpWebView();
		
		Display display = getWindow().getWindowManager().getDefaultDisplay();
		final float scale = getContext().getResources().getDisplayMetrics().density;
		int orientation = getContext().getResources().getConfiguration().orientation;
		float[] dimensions = (orientation == Configuration.ORIENTATION_LANDSCAPE) ? DIMENSIONS_DIFF_LANDSCAPE
				: DIMENSIONS_DIFF_PORTRAIT;

		addContentView(
				mContent,
				new LinearLayout.LayoutParams(display.getWidth()
						- ((int) (dimensions[0] * scale + 0.5f)), display
						.getHeight() - ((int) (dimensions[1] * scale + 0.5f))));
	}

	/**
	 * Sets title and icon of provider
	 * 
	 */
	
	private void setUpTitle() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mTitle = new TextView(getContext());
		int res = getContext().getResources().getIdentifier(mProviderName.toString(),
				"drawable", getContext().getPackageName());
		icon = getContext().getResources().getDrawable(res);
		StringBuilder sb = new StringBuilder();
		sb.append(mProviderName.toString().substring(0, 1).toUpperCase());
		sb.append(mProviderName.toString().substring(1, mProviderName.toString().length()));
		mTitle.setText(sb.toString());
		mTitle.setGravity(Gravity.CENTER_VERTICAL);
		mTitle.setTextColor(Color.WHITE);
		mTitle.setTypeface(Typeface.DEFAULT_BOLD);
		mTitle.setBackgroundColor(BLUE);
		mTitle.setPadding(MARGIN + PADDING, MARGIN, MARGIN, MARGIN);
		mTitle.setCompoundDrawablePadding(MARGIN + PADDING);
		mTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
		mContent.addView(mTitle);
	}

	/**
	 * Set up WebView to load the provider URL
	 * 
	 */
	private void setUpWebView() {
		mWebView = new CustomWebView(getContext());
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.setWebViewClient(new SocialAuthDialog.SocialAuthWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.loadUrl(mUrl);
		mWebView.setLayoutParams(FILL);
		mContent.addView(mWebView);
	}

	
	private class SocialAuthWebViewClient extends WebViewClient 
	{
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d("SocialAuth-WebView", "Override url: " + url);
			
			if (url.startsWith(mProviderName.getCallbackUri()) && (mProviderName.toString().equalsIgnoreCase("facebook") || mProviderName.toString().equalsIgnoreCase("twitter"))) {
					if (url.startsWith(mProviderName.getCancelUri())) {
						// Handles Twitter and Facebook Cancel
						mListener.onCancel();
					} 
					else 
					{ // for Facebook and Twitter
						final Map<String, String> params = Util.parseUrl(url);
						
						Runnable runnable = new Runnable()  
						{
							public void run() 
						    {
						        try 
						        {
									mSocialAuthManager.connect(params);
								} 
						        catch (Exception e) 
						        {
						        	e.printStackTrace();
									mListener.onError(new SocialAuthError("Unknown Error", e));
								}	
									
						        handler.post(new Runnable() 
								{
									@Override
									public void run() 
									{	
										Bundle bundle = new Bundle();
										bundle.putString(SocialAuthAdapter.PROVIDER, mProviderName.toString());
										mListener.onComplete(bundle);
									}
								});
						      }
						    };
						    new Thread(runnable).start();		
					}
				SocialAuthDialog.this.dismiss();
				return true;
			} 
			else if (url.startsWith(mProviderName.getCancelUri())) {
				// Handles MySpace and Linkedin Cancel
				mListener.onCancel();
				SocialAuthDialog.this.dismiss();
				return true;
			} else if (url.contains(DISPLAY_STRING)) {
				return false;
			}
			
			return false;
			
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {

			super.onReceivedError(view, errorCode, description, failingUrl);
			mListener.onError(new SocialAuthError(description, new Exception(failingUrl)));
			SocialAuthDialog.this.dismiss();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			
			// For Linkedin and MySpace -  Calls onPageStart to authorize.
			if (url.startsWith(mProviderName.getCallbackUri())) {
				Log.d("SocialAuth-WebView", "onPageStart:" + url);
				if (url.startsWith(mProviderName.getCancelUri())) {
					mListener.onCancel();
				} 
				else {
					final Map<String, String> params = Util.parseUrl(url);	
					Runnable runnable = new Runnable()  
					{
						public void run() {
							try {
								mSocialAuthManager.connect(params);
							} 
					        catch (Exception e) {
					        	e.printStackTrace();
								mListener.onError(new SocialAuthError("Could not connect using SocialAuth", e));
							}	
								
					        handler.post(new Runnable() 
							{
								@Override
								public void run() {	
									Bundle bundle = new Bundle();
									bundle.putString(SocialAuthAdapter.PROVIDER, mProviderName.toString());
									mListener.onComplete(bundle);
								}
							});
						}
					};
					new Thread(runnable).start();    
				}
				SocialAuthDialog.this.dismiss();
			}
			mSpinner.show();
		}

		@Override
		public void onPageFinished(WebView view, String url) {

			super.onPageFinished(view, url);
			
			String title = mWebView.getTitle();
			if (title != null && title.length() > 0) {
				mTitle.setText(title);
			}
			mSpinner.dismiss();
		}
	}

	
	/**
	 * Workaround for Null pointer exception in WebView.onWindowFocusChanged in
	 * droid phones and emulator with android 2.2 os. It prevents first time
	 * WebView crash.
	 */

	public class CustomWebView extends WebView {

		public CustomWebView(Context context) {
			super(context);
		}

		public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		public CustomWebView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		public void onWindowFocusChanged(boolean hasWindowFocus) {
			try {
				super.onWindowFocusChanged(hasWindowFocus);
			} catch (NullPointerException e) {
				// Catch null pointer exception
			}
		}
	}
}
