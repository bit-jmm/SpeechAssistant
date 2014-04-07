package com.bit.speechassistant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;

import android.R.bool;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.opengl.ETC1;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bit.speech.util.ApkInstaller;
import com.bit.speech.util.JsonParser;
import com.iflytek.speech.ErrorCode;
import com.iflytek.speech.ISpeechModule;
import com.iflytek.speech.InitListener;
import com.iflytek.speech.RecognizerListener;
import com.iflytek.speech.RecognizerResult;
import com.iflytek.speech.SpeechConstant;
import com.iflytek.speech.SpeechRecognizer;
import com.iflytek.speech.SpeechSynthesizer;
import com.iflytek.speech.SpeechUtility;
import com.iflytek.speech.SynthesizerListener;
import com.iflytek.speech.UtilityConfig;

@SuppressLint("JavascriptInterface")
public class MainActivity extends Activity implements OnClickListener {
	private static String TAG = "MainActivity";
	private Handler mHandler;
	private Dialog mLoadDialog;
	private EditText dialogue;
	private Toast mToast;
	private int mNativePerson = 0;
	private Button ttsBtn;
	private Button sttBtn;
	// 语音识别对象。
	private SpeechRecognizer mStt;
	// 语音合成对象
	private SpeechSynthesizer mTts;

	// 每一次语音识别的结果
	private String SttResult = null;
	private String temp = null;

	private String messageText = null;

	private WebView webView = null;

	private Handler dialHandler;

	private boolean mRunning = false;
	
	private int x = 0;
	private int y = 0;

	/**
	 * 发音人选择。
	 */
	final String[] nativePersons = new String[] { "xiaoyan", "nannan",
			"xiaojing", "xiaofeng" };
	final String[] onlinePersons = new String[] { "xiaoyan", "vixq", "vixyun",
			"vixx", "vixl", "vixr" };

	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		dialogue = (EditText) findViewById(R.id.dialogue);
		dialogue.setMovementMethod(ScrollingMovementMethod.getInstance());
		Button mBtnSpeechAssistant = (Button) findViewById(R.id.speech_assistant);
		mBtnSpeechAssistant.setOnClickListener(this);

		Button mBtnStartDial = (Button) findViewById(R.id.start_dial);
		mBtnStartDial.setOnClickListener(this);

		webView = (WebView) findViewById(R.id.web);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// TODO Auto-generated method stub
				view.loadUrl(url);
				return super.shouldOverrideUrlLoading(view, url);
			}
			
			
			
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				// TODO Auto-generated method stub
				Log.d("WebView","onPageStarted");
				super.onPageStarted(view, url, favicon);
			}



			public void onPageFinished(WebView view, String url) {
				// TODO Auto-generated method stub
				Log.d("WebView","onPageFinished ");
//				view.loadUrl("javascript:window.local_obj.showSource('<head>'+" +
//		                "document.getElementsByTagName('html')[0].innerHTML+'</head>');");		
//				webView.loadUrl("javascript:document.getElementsByName('word')[0].value='北京理工大学'");
				
				if (url.equals("http://wapbaike.baidu.com/?adapt=1&")) {
					view.loadUrl("javascript:document.getElementsByName('word')[0].value='北京理工大学'");
//					try {
//						Thread.currentThread().sleep(1000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					Log.w("webView", "Load");
////					touchScreen(view,111.83757f, 175.40332f);
//					touchScreen(view,447.35028f, 192.3757f);
					
					Message message = new Message();
					message.what = 3;
					mHandler.sendMessage(message);
				}
				super.onPageFinished(view, url);
//				view.performLongClick();
//				Log.w("webView", "Finish");
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				touchScreen(view.findFocus(), view.findFocus().getLeft()+5, view.findFocus().getTop()+5);
			}

		});
		webView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				// switch (event.getAction()) {
				//
				// case MotionEvent.ACTION_DOWN:
				// editText1.setText(String.valueOf(event.getRawX()));
				// editText2.setText(String.valueOf(event.getRawY()));
				// break;
				// case MotionEvent.ACTION_UP:
				// if (!v.hasFocus()) {
				// v.requestFocus();
				//
				// }
				// break;
				// }
				int action = event.getAction();
				float x = event.getX();
				float y = event.getY();
				event.getMetaState();
				Log.v("ON_TOUCH",
						"Action = " + action + " View:" + v.toString());
				Log.v("ON_TOUCH", "X = " + x + "Y = " + y);
				return false;
			}
		});
		ttsBtn = (Button) findViewById(R.id.tts);
		ttsBtn.setOnClickListener(this);

		sttBtn = (Button) findViewById(R.id.stt);
		sttBtn.setOnClickListener(this);

		mToast = Toast.makeText(this, "", Toast.LENGTH_LONG);

		SpeechUtility.getUtility(this).setAppid(getString(R.string.app_id));
		// 初始化识别对象
		mStt = new SpeechRecognizer(this, mInitListener);
		// 初始化合成对象
		mTts = new SpeechSynthesizer(this, mTtsInitListener);

		mStt.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		mStt.setParameter(SpeechConstant.ACCENT, "mandarin");
		mStt.setParameter(SpeechConstant.DOMAIN, "iat");
		mStt.setParameter(SpeechConstant.PARAMS,
				"asr_ptt=1,asr_audio_path=/sdcard/asr.pcm");

		mTts.setParameter(SpeechConstant.ENGINE_TYPE, "local");
		mTts.setParameter(SpeechSynthesizer.VOICE_NAME,
				nativePersons[mNativePerson]);
		mTts.setParameter(SpeechSynthesizer.SPEED, "50");
		mTts.setParameter(SpeechSynthesizer.PITCH, "50");
		mTts.setParameter(SpeechConstant.PARAMS,
				"tts_audio_path=/sdcard/tts.pcm");

		mHandler = new Myhandler(Looper.getMainLooper());

	}
	
	final class InJavaScriptLocalObj {
	    public void showSource(String html) {
	        Log.d("HTML", html);
	    }
	}
	
	private void simulateDoubleTapEvent(View view, float x, float y,
			int action) {
		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis() + 100;
		// List of meta states found here:
		// developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
		int metaState = 0;
		MotionEvent me = MotionEvent.obtain(downTime, eventTime, action, x, y,
				metaState);
		view.dispatchTouchEvent(me);
	}
	
	public void touchScreen(View view,float x,float y) {
		simulateDoubleTapEvent(view, x, y, 0);
		simulateDoubleTapEvent(view, x, y, 2);
		simulateDoubleTapEvent(view, x, y, 2);
		simulateDoubleTapEvent(view, x, y, 2);
		simulateDoubleTapEvent(view, x, y, 1);
	}

	class Myhandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				String url = SpeechUtility.getUtility(MainActivity.this)
						.getComponentUrl();
				String assetsApk = "SpeechService.apk";
				if (processInstall(MainActivity.this, url, assetsApk)) {
					Message message = new Message();
					message.what = 1;
					mHandler.sendMessage(message);
				}
				break;
			case 1:
				if (mLoadDialog != null) {
					mLoadDialog.dismiss();
				}
				break;
			case 2:
				dialogue.setText(dialogue.getText().toString() + "\n"
						+ messageText);
				dialogue.setSelection(dialogue.getText().length(), dialogue
						.getText().length());
				break;
			case 3:
				try {
					Thread.currentThread().sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				touchScreen(webView,447.35028f, 192.3757f);
			default:
				break;
			}
			super.handleMessage(msg);
		}

		public Myhandler(Looper looper) {
			super(looper);
			// TODO Auto-generated constructor stub
		}

	}

	Runnable mBackgroundRunnable = new Runnable() {

		private void sendUpdateMessage() {
			Message message = new Message();
			message.what = 2;
			mHandler.sendMessage(message);
		}

		@Override
		public void run() {
			// ----------模拟耗时的操作，开始---------------
			// while (mRunning) {
			// mRunning = false;
			//
			// SttResult = "你好";
			// while (true) {
			// speak(SttResult);
			// waitSomeTime(3000);
			// SpeechRecognize();
			// waitSomeTime(2000);
			// }
			// }
			while (mRunning) {
				mRunning = false;

				// 对话开始时晓燕首先报时
				String string = "主人，您好！我是晓燕，现在是 " + getNowDate()
						+ "请问有什么可以帮助您的吗？";

				messageText = "晓燕：" + string;
				sendUpdateMessage();

				speak(string);

				// waitSomeTime(string.length()*100);

				// 0表示轮到主人说话，1表示轮到机器人晓燕说话。
				int whoseTurn = 0;
				// 0表示这是一个问题，1表示问题确认。
				int isQuestion = 0;

				String question = null;

				while (true) {

					if (0 == whoseTurn) { // 主人说话
						waitSomeTime(1000);
						while (true) {
							SttResult = null;
							SpeechRecognize();
							waitSomeTime(5000);
							if (null != SttResult) {
								break;
							}
						}
						whoseTurn = 1;
					} else { // 晓燕说话
						if (0 == isQuestion) {
							temp = "请问您是要问" + SttResult + "吗？";
							messageText = "晓燕：请问您是要问" + SttResult + "吗？";
							sendUpdateMessage();
							speak(temp);
							waitSomeTime(temp.length() * 500);
							isQuestion = 1;
							question = SttResult;

						} else {
							if (isPositive(SttResult)) {
								if (isQNowTime(question)) {
									temp = "主人，您好！现在是 " + getNowDate()
											+ "回答完毕，请您再次提问。";
									messageText = "晓燕：" + temp;
									sendUpdateMessage();
									speak(temp);
									waitSomeTime(temp.length() * 300);
								} else if (isChinaFamousUniversity(question)) {
									temp = "主人，您好！中国著名大学有清华大学和北京大学。 "
											+ "回答完毕，请您再次提问。";
									messageText = "晓燕：" + temp;
									sendUpdateMessage();
									speak(temp);
									waitSomeTime(temp.length() * 300);
								} else {
									messageText = "晓燕：主人，对不起，我回答不了您的问题，请重新提问。";
									sendUpdateMessage();
									speak("主人，对不起，我回答不了您的问题，请重新提问。");
									waitSomeTime(5000);
								}
							} else {
								messageText = "晓燕：对不起，我没有听清楚您的问题，请主人重新提问。";
								sendUpdateMessage();
								speak("对不起，我没有听清楚您的问题，请主人重新提问。");
								waitSomeTime(5000);
							}
							isQuestion = 0;
						}
						whoseTurn = 0;
					}
					// break;
				}
			}
			// ----------模拟耗时的操作，结束---------------
		}
	};

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub

		if (!checkSpeechServiceInstalled()) {
			Message message = new Message();
			message.what = 0;
			mHandler.sendMessage(message);
			return;
		}
		switch (view.getId()) {
		case R.id.speech_assistant:
			SpeechUtility.getUtility(MainActivity.this).setAppid(
					getString(R.string.app_id));
			Intent intent = new Intent(this, SpeechAssistant.class);
			startActivity(intent);
			break;
		case R.id.tts:

			int code = mTts.startSpeaking(dialogue.getText().toString(),
					mTtsListener);
			if (code != 0) {
				showTip("start speak error : " + code);
			} else {
				showTip("start speak success.");
			}

			break;
		case R.id.stt:
//			mStt.startListening(mRecognizerListener);
			webView.loadUrl("http://baike.baidu.com/");
			
			
			break;
		case R.id.start_dial:
			webView.loadUrl("http://baike.baidu.com/");

			// startDial();
			break;
		default:
			break;
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mRunning = true;
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		mRunning = false;
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		// TODO Auto-generated method stub
		// ttsBtn.performClick();

	}

	// 语音合成
	private void speak(String text) {
		int code = mTts.startSpeaking(text, mTtsListener);
		if (code != 0) {
			showTip("start speak error : " + code);
		} else
			showTip("start speak success.");
		while (mTts.isSpeaking()) {

		}
	}

	// 语音识别
	private void SpeechRecognize() {
		mStt.startListening(mRecognizerListener);
	}

	// 返回系统现在日期和时间
	private String getNowDate() {
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy年MM月dd日HH时mm分ss秒。");
		Date curDate = new Date(System.currentTimeMillis());
		return formatter.format(curDate);
	}

	// 是否为报时问题
	private boolean isQNowTime(String question) {
		// TODO Auto-generated method stub
		if (question.contains("报时") || question.contains("时间")
				|| (question.contains("报") && question.contains("时"))
				|| (question.contains("现在") && question.contains("几点"))
				|| (question.contains("现在") && question.contains("时"))) {
			return true;
		}
		return false;
	}

	// 是否问中国著名大学
	private boolean isChinaFamousUniversity(String question) {
		// TODO Auto-generated method stub
		if ((question.contains("中国") && question.contains("大学"))
				|| (question.contains("著名") && question.contains("大学"))
				|| (question.contains("中国") && question.contains("著名"))) {
			return true;
		}
		return false;
	}

	// 确认问题
	private boolean isPositive(String answer) {
		if (answer.contains("不")) {
			return false;
		}
		return true;
	}

	// 开始对话
	private void startDial() {
		// 对话线程
		HandlerThread thread = new HandlerThread("DialHandlerThread");
		thread.start();// 创建一个HandlerThread并启动它
		dialHandler = new Handler(thread.getLooper());// 使用HandlerThread的looper对象创建Handler，如果使用默认的构造方法，很有可能阻塞UI线程
		dialHandler.post(mBackgroundRunnable);// 将线程post到Handler中
	}

	private void waitSomeTime(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 初期化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {

		@Override
		public void onInit(ISpeechModule arg0, int code) {
			Log.d(TAG, "InitListener init() code = " + code);
			if (code == ErrorCode.SUCCESS) {
				// ((Button)findViewById(R.id.tts_play)).setEnabled(true);
				mTts.setParameter(SpeechSynthesizer.VOICE_NAME, "xiaoyan");
			}
		}
	};
	/**
	 * 初期化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(ISpeechModule module, int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code == ErrorCode.SUCCESS) {
				// ((Button)findViewById(R.id.start_listen)).setEnabled(true);
			}
		}
	};

	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener.Stub() {
		@Override
		public void onBufferProgress(int progress) throws RemoteException {
			Log.d(TAG, "onBufferProgress :" + progress);
			showTip("onBufferProgress :" + progress);
		}

		@Override
		public void onCompleted(int code) throws RemoteException {
			Log.d(TAG, "onCompleted code =" + code);
			if (ErrorCode.ERROR_LOCAL_RESOURCE == code) {
				// Intent intent = new Intent();
				// intent.setAction("com.iflytek.speechcloud.activity.speaker.SpeakerSetting");
				// startActivity(intent);
				showTip("无本地发音人资源，请到语音+中下载发音人！");
			} else {
				showTip("onCompleted code =" + code);
			}
		}

		@Override
		public void onSpeakBegin() throws RemoteException {
			Log.d(TAG, "onSpeakBegin");
			showTip("onSpeakBegin");
		}

		@Override
		public void onSpeakPaused() throws RemoteException {
			Log.d(TAG, "onSpeakPaused.");
			showTip("onSpeakPaused.");
		}

		@Override
		public void onSpeakProgress(int progress) throws RemoteException {
			Log.d(TAG, "onSpeakProgress :" + progress);
			showTip("onSpeakProgress :" + progress);
		}

		@Override
		public void onSpeakResumed() throws RemoteException {
			Log.d(TAG, "onSpeakResumed.");
			showTip("onSpeakResumed");
		}
	};

	/**
	 * 识别回调。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener.Stub() {

		@Override
		public void onVolumeChanged(int v) throws RemoteException {
			showTip("onVolumeChanged：" + v);
		}

		@Override
		public void onResult(final RecognizerResult result, boolean isLast)
				throws RemoteException {
			// runOnUiThread(new Runnable() {
			// @Override
			// public void run() {
			if (null != result) {
				// 显示
				Log.d(TAG, "recognizer result：" + result.getResultString());
				if (!JsonParser.parseIatResult(result.getResultString())
						.equals("。")) {
					SttResult = JsonParser.parseIatResult(result
							.getResultString());
					messageText = "主人："
							+ SttResult.substring(0, SttResult.length());
					Message message = new Message();
					message.what = 2;
					mHandler.sendMessage(message);
				}

			} else {
				Log.d(TAG, "recognizer result : null");
				showTip("无识别结果");
			}
			// }
			// });

		}

		@Override
		public void onError(int errorCode) throws RemoteException {
			showTip("onError Code：" + errorCode);
		}

		@Override
		public void onEndOfSpeech() throws RemoteException {
			showTip("onEndOfSpeech");
		}

		@Override
		public void onBeginOfSpeech() throws RemoteException {
			showTip("onBeginOfSpeech");
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mTts.stopSpeaking(mTtsListener);
		// 退出时释放连接
		mTts.destory();
		mStt.cancel(mRecognizerListener);
		mStt.destory();

		dialHandler.removeCallbacks(mBackgroundRunnable);
	}

	private void showTip(final String str) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mToast.setText(str);
				mToast.show();
			}
		});
	}

	/**
	 * 如果服务组件没有安装，有两种安装方式。 1.直接打开语音服务组件下载页面，进行下载后安装。
	 * 2.把服务组件apk安装包放在assets中，为了避免被编译压缩，修改后缀名为mp3，然后copy到SDcard中进行安装。
	 */
	private boolean processInstall(Context context, String url, String assetsApk) {
		// 直接下载方式
		// ApkInstaller.openDownloadWeb(context, url);
		// 本地安装方式
		if (!ApkInstaller.installFromAssets(context, assetsApk)) {
			Toast.makeText(MainActivity.this, "安装失败", Toast.LENGTH_SHORT)
					.show();
			return false;
		}
		return true;
	}

	public boolean checkSpeechServiceInstalled() {
		String packageName = UtilityConfig.DEFAULT_COMPONENT_NAME;
		List<PackageInfo> packages = getPackageManager()
				.getInstalledPackages(0);
		for (int i = 0; i < packages.size(); i++) {
			PackageInfo packageInfo = packages.get(i);
			if (packageInfo.packageName.equals(packageName)) {
				return true;
			} else {
				continue;
			}
		}
		return false;
	}

}
