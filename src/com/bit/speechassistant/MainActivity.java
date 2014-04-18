package com.bit.speechassistant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.security.auth.PrivateCredentialPermission;

import android.R.bool;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.opengl.ETC1;
import android.opengl.Visibility;
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
import android.view.KeyEvent;
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
	
	private String baikeAnswer = null;

	private WebView webView = null;

	private Handler dialHandler;

	private boolean mRunning = false;

	private int x = 0;
	private int y = 0;

	private boolean available = true;

	private Object lock = new Object();

	private boolean isFirstTime = true;
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
		init();
	}
	class MyWebViewClient extends WebViewClient {
		
		public MyWebViewClient() {
			super();
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// TODO Auto-generated method stub
			view.loadUrl(url);
			return super.shouldOverrideUrlLoading(view, url);
		}

		public void onPageFinished(WebView view, String url) {
			// TODO Auto-generated method stub
			Log.d(TAG, "onPageFinished" + " : " + url);
			// synchronized (lock) {
			if (url.equals("http://wapbaike.baidu.com/?adapt=1&")) {
				if (SttResult == null) {
					view.loadUrl("javascript:document.getElementsByName('word')[0].value='北京理工大学'");
				} else {
					view.loadUrl("javascript:document.getElementsByName('word')[0].value='"+SttResult+"'");
				}
				
				// view.loadUrl("javascript:document.getElementsByName('submit')[0].click()");
				// lock.notifyAll();
				// available = true;
				mHandler.sendEmptyMessage(3);
			}
//			view.loadUrl("javascript:window.local_obj.showSource('<head>'+"
//					+ "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
			if (!url.equals("http://wapbaike.baidu.com/?adapt=1&")) {
				view.loadUrl("javascript:window.local_obj.showSource(document.getElementsByTagName('p')[1].innerText);");
			}
			// }
			super.onPageFinished(view, url);
		}

	}
	
	private void init() {
		dialogue = (EditText) findViewById(R.id.dialogue);
		dialogue.setMovementMethod(ScrollingMovementMethod.getInstance());
		dialogue.setSelection(dialogue.getText().length(), dialogue.getText().length());
		Button mBtnSpeechAssistant = (Button) findViewById(R.id.speech_assistant);
		mBtnSpeechAssistant.setOnClickListener(this);
		mBtnSpeechAssistant.setEnabled(false);
		mBtnSpeechAssistant.setVisibility(View.INVISIBLE);
		
		Button mBtnStartDial = (Button) findViewById(R.id.start_dial);
		mBtnStartDial.setOnClickListener(this);

		ttsBtn = (Button) findViewById(R.id.tts);
		ttsBtn.setOnClickListener(this);
		ttsBtn.setEnabled(false);
		ttsBtn.setVisibility(View.INVISIBLE);
		
		sttBtn = (Button) findViewById(R.id.stt);
		sttBtn.setOnClickListener(this);
//		sttBtn.setEnabled(false);
//		sttBtn.setVisibility(View.INVISIBLE);
		
		webView = (WebView) findViewById(R.id.web);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
		webView.setWebViewClient(new MyWebViewClient());

		webView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getAction();
				float x = event.getX();
				float y = event.getY();
				event.getMetaState();
				Log.d("ON_TOUCH",
						"Action = " + action + " View:" + v.toString());
				Log.d("ON_TOUCH", "X = " + x + "Y = " + y);
				return false;
			}
		});
		

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

	class WebTouchThread extends Thread {
		@Override
		public void run() {
			// synchronized (lock) {
			// while (available == false) {
			// try {
			// lock.wait();
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// }
			// available = false;

			try {
				sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			touchScreen(webView, 447.35028f, 192.3757f);
			
			if(!webView.canGoBack()) {
				touchScreen(webView, 447.35028f, 192.3757f);
			}
			Log.d(TAG, String.valueOf(this.getId()));
			// }
		}
	}

	class WebLoadThread extends Thread {
		private String url;

		public WebLoadThread(String url) {
			super();
			this.url = url;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			// synchronized (lock) {
			// while (available == false) {
			// try {
			// lock.wait();
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// }
			// available = false;

			webView.loadUrl(url);
			Log.d(TAG, String.valueOf(this.getId()));
			Log.d(TAG, url);
			// }
			super.run();
		}

	}


	final class InJavaScriptLocalObj {
		public void showSource(String html) {
			Log.d(TAG, "网页文本：  "+html);
			if (SttResult != null && html.contains(SttResult)) {
				baikeAnswer = html;
			}
			System.out.println("====>html=" + html);
		}
	}

	private void simulateDoubleTapEvent(View view, float x, float y, int action) {
		long downTime = SystemClock.uptimeMillis();
		long eventTime = SystemClock.uptimeMillis() + 100;
		// List of meta states found here:
		// developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
		int metaState = 0;
		MotionEvent me = MotionEvent.obtain(downTime, eventTime, action, x, y,
				metaState);
		view.dispatchTouchEvent(me);
	}

	public void touchScreen(View view, float x, float y) {
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
				WebTouchThread webTouchThread = new WebTouchThread();
				webTouchThread.start();
				try {
					webTouchThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case 4:
				WebLoadThread webLoadThread = new WebLoadThread(
						"http://baike.baidu.com/");
				webLoadThread.start();
				try {
					webLoadThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
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
			while (mRunning) {
				mRunning = false;

				// 对话开始时晓燕首先报时
				String string = "您好！现在是 " + getNowDate() + "请问有什么可以帮助您的吗？";

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
									temp = "您好！现在是 " + getNowDate()
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
								} else if (isBaikeQuestion(question)) {
									temp = "请问您要问什么百科知识？";
									messageText = "晓燕：" + temp;
									sendUpdateMessage();
									speak(temp);
									waitSomeTime(temp.length() * 300);

									while (true) {
										SttResult = null;
										SpeechRecognize();
										waitSomeTime(5000);
										if (null != SttResult) {
											break;
										}
									}
									if (webView.canGoBack()) {
										webView.goBack();
									} else {
										mHandler.sendEmptyMessage(4);
									}
									
									while (true) {
										waitSomeTime(2000);
										if (null != baikeAnswer) {
											break;
										}
									}
									
									messageText = "晓燕：" + baikeAnswer;
									sendUpdateMessage();
									speak(baikeAnswer + "回答完毕！请您重新提问！");
									waitSomeTime(baikeAnswer.length()*125);

									waitSomeTime(5000);
									baikeAnswer = null;
									
								} else {
									messageText = "晓燕：对不起，我回答不了您的问题，请重新提问。";
									sendUpdateMessage();
									speak("主人，对不起，我回答不了您的问题，请重新提问。");
									waitSomeTime(5000);
								}
							} else {
								messageText = "晓燕：对不起，我没有听清楚您的问题，请重新提问。";
								sendUpdateMessage();
								speak("对不起，我没有听清楚您的问题，请重新提问。");
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
			// mStt.startListening(mRecognizerListener);
			WebLoadThread webLoadThread = new WebLoadThread(
					"http://baike.baidu.com/");
			webLoadThread.start();
			try {
				webLoadThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// WebTouchThread webTouchThread = new WebTouchThread();
			// webTouchThread.start();
			// try {
			// webLoadThread.join();
			// } catch (InterruptedException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// webLoadThread.setPriority(Thread.MAX_PRIORITY);

			break;
		case R.id.start_dial:
			sttBtn.performClick();
			startDial();
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
			showTip("语音合成错误!!!" + code);
		} else
			showTip("开始合成语音……");
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

	// 是否问中国著名大学
	private boolean isBaikeQuestion(String question) {
		// TODO Auto-generated method stub
		if (question.contains("百科") || question.contains("百度")
				|| (question.contains("百") && question.contains("科"))) {
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
				showTip("机器人说话完毕。 code =" + code);
			}
		}

		@Override
		public void onSpeakBegin() throws RemoteException {
			Log.d(TAG, "onSpeakBegin");
			showTip("机器人开始说话。请保持安静！");
		}

		@Override
		public void onSpeakPaused() throws RemoteException {
			Log.d(TAG, "onSpeakPaused.");
			showTip("机器人说话中断！");
		}

		@Override
		public void onSpeakProgress(int progress) throws RemoteException {
			Log.d(TAG, "onSpeakProgress :" + progress);
			showTip("机器人正在说话。请保持安静！ " + progress);
		}

		@Override
		public void onSpeakResumed() throws RemoteException {
			Log.d(TAG, "onSpeakResumed.");
			showTip("机器人重新开始说话。");
		}
	};

	/**
	 * 识别回调。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener.Stub() {

		@Override
		public void onVolumeChanged(int v) throws RemoteException {
			// showTip("请开始说话。onVolumeChanged：" + v);
//			showTip("");
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
			showTip("识别错误 Code：" + errorCode);
		}

		@Override
		public void onEndOfSpeech() throws RemoteException {
			showTip("语音识别完毕。");
		}

		@Override
		public void onBeginOfSpeech() throws RemoteException {
			showTip("请您开始说话……");
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

	/* 退出软件 */
	private void cofirmExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setTitle("退出软件").setMessage("是否退出软件?")
				.setPositiveButton("是", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						/* 调用finish()方法,退出程序 */
						MainActivity.this.finish();
					}
				})
				.setNegativeButton("否", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						/* 若选择否则不需要填写代码 */
					}
				}).show(); /* 记得调用show()方法,否则显示不出来Dialog */
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			/* 用户按返回键,如果可返回则回退 */
			System.out.println("test onKeyDown-->1");
			webView.goBack();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			/* 用户按返回键,若不可返回时则退出程序 */
			System.out.println("test onKeyDown-->2");
			cofirmExit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
