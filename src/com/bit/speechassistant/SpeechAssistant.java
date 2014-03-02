package com.bit.speechassistant;
import com.bit.speech.util.JsonParser;
import com.iflytek.speech.ErrorCode;
import com.iflytek.speech.ISpeechModule;
import com.iflytek.speech.InitListener;
import com.iflytek.speech.RecognizerListener;
import com.iflytek.speech.RecognizerResult;
import com.iflytek.speech.SpeechConstant;
import com.iflytek.speech.SpeechRecognizer;
import com.iflytek.speech.SpeechSynthesizer;
import com.iflytek.speech.SynthesizerListener;
import android.os.Bundle;
import android.os.RemoteException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class SpeechAssistant extends Activity implements OnClickListener{
	private static String TAG = "SpeechAssistant";
	// 语音识别对象。
	private SpeechRecognizer mStt;
	// 语音合成对象
	private SpeechSynthesizer mTts;
	private Toast mToast;
	private RadioGroup mRadioGroup;
	private Button mBtnPerson;     //选择发音人按钮
	private int mNativePerson=0;
	private int mOnlinePerson=0;
	private EditText mEditText;
	
	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.speech_assistant);
		initLayout();
		// 初始化识别对象
		mStt = new SpeechRecognizer(this, mInitListener);
		// 初始化合成对象
 		mTts = new SpeechSynthesizer(this, mTtsInitListener);
 		mToast = Toast.makeText(this,"",Toast.LENGTH_LONG);
	}
	/**
 	 * 初始化。
 	 */
 	private void initLayout(){
 		mEditText = ((EditText)findViewById(R.id.message_text));
 		findViewById(R.id.start_listen).setOnClickListener(this);
 		findViewById(R.id.start_listen).setEnabled(false);
 		findViewById(R.id.tts_play).setOnClickListener(this);
 		findViewById(R.id.tts_play).setEnabled(false);
 		mBtnPerson=(Button) findViewById(R.id.tts_btn_person_select);
 		mBtnPerson.setOnClickListener(this);
 		mRadioGroup=((RadioGroup) findViewById(R.id.tts_rediogroup));
 		mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.tts_radiobtn_native:
					/**
					 * 选择本地合成
					 */
					mTts.setParameter(SpeechConstant.ENGINE_TYPE, "local");
					mTts.setParameter(SpeechSynthesizer.VOICE_NAME,	nativePersons[mNativePerson]);
					break;
				case R.id.tts_radiobtn_online:
					/**
					 * 选择在线合成
					 */
					mTts.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
					mTts.setParameter(SpeechSynthesizer.VOICE_NAME,	onlinePersons[mOnlinePerson]);
					break;
				default:
					break;
				}
				
			}
		} );
 	}
 	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
//		if(v.getTag() == null)
//		{
//			showTip("未知错误");
//			return;
//		}
		
		switch(v.getId())
 		{
 			case R.id.start_listen:
 				SpeechRecognize();
 				break;
 			case R.id.tts_play:
 				startSpeaking();
 				break;
// 			case R.id.tts_cancel:
// 				mTts.stopSpeaking(mTtsListener);
// 				break;
// 			case R.id.tts_pause:
// 				mTts.pauseSpeaking(mTtsListener);
// 				break;
// 			case R.id.tts_resume:
// 				mTts.resumeSpeaking(mTtsListener);
// 				break;
 			case R.id.tts_btn_person_select:
 				showPresonSelectDialog();
 				break;
 		}
	}
	//语音识别
	private void SpeechRecognize() {
		mStt.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		mStt.setParameter(SpeechConstant.ACCENT, "mandarin");
		mStt.setParameter(SpeechConstant.DOMAIN, "iat");
		mStt.setParameter(SpeechConstant.PARAMS, "asr_ptt=1,asr_audio_path=/sdcard/asr.pcm");
		mStt.startListening(mRecognizerListener);
	}
	private void startSpeaking() {
		//语音合成
		String text = mEditText.getText().toString();
		// 设置参数
//		mTts.setParameter(SpeechSynthesizer.VOICE_NAME, "vixk");
		mTts.setParameter(SpeechSynthesizer.SPEED, "50");
		mTts.setParameter(SpeechSynthesizer.PITCH, "50");
		mTts.setParameter(SpeechConstant.PARAMS, "tts_audio_path=/sdcard/tts.pcm");
		int code = mTts.startSpeaking(text, mTtsListener);
		if(code != 0)
		{
			showTip("start speak error : " + code);
		}else
		showTip("start speak success.");

	} 
 	/**
	 * 发音人选择。
	 */
 	final String[] nativePersons = new String[] { "xiaoyan", "nannan", "xiaojing", "xiaofeng" };
 	final String[] onlinePersons = new String[] { "xiaoyan", "vixq", "vixyun", "vixx", "vixl", "vixr" };
 	
	private void showPresonSelectDialog() {
		switch (mRadioGroup.getCheckedRadioButtonId()) {
		case R.id.tts_radiobtn_native:
			// 选择本地合成，目前只支持小燕一个发音人			
			new AlertDialog.Builder(this).setTitle("本地合成发音人选项")
					.setSingleChoiceItems(new String[] { "晓燕" , "楠楠", "晓婧", "晓峰"}, // 单选框有几项,各是什么名字
							mNativePerson, // 默认的选项
							new DialogInterface.OnClickListener() { // 点击单选框后的处理
								public void onClick(DialogInterface dialog,
										int which) { // 点击了哪一项
									mTts.setParameter(SpeechSynthesizer.VOICE_NAME,	nativePersons[which]);
									mNativePerson = which;
									dialog.dismiss();
								}
							}).show();
			break;
		case R.id.tts_radiobtn_online:
			// 选择在线合成
			new AlertDialog.Builder(this)
					.setTitle("在线合成发音人选项")
					.setSingleChoiceItems(
							new String[] { "小燕", "小琪", "小芸", "小新", "小莉", "小蓉" }, // 单选框有几项,各是什么名字
							mOnlinePerson, // 默认的选项
							new DialogInterface.OnClickListener() { // 点击单选框后的处理
								public void onClick(DialogInterface dialog,
										int which) { // 点击了哪一项
									mTts.setParameter(SpeechSynthesizer.VOICE_NAME,	onlinePersons[which]);
									mOnlinePerson = which;
									dialog.dismiss();
								}
							}).show();
			break;
		default:
			break;
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
        		((Button)findViewById(R.id.tts_play)).setEnabled(true);
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
        		((Button)findViewById(R.id.start_listen)).setEnabled(true);
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
            if(ErrorCode.ERROR_LOCAL_RESOURCE == code){
            	Intent intent = new Intent();
            	intent.setAction("com.iflytek.speechcloud.activity.speaker.SpeakerSetting");
            	startActivity(intent);
//            	showTip("无本地发音人资源，请到语音+中下载发音人！");
            }else{
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
            showTip("onVolumeChanged："	+ v);
        }
        
        @Override
        public void onResult(final RecognizerResult result, boolean isLast)
                throws RemoteException {
        	runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (null != result) {
		            	// 显示
						Log.d(TAG, "recognizer result：" + result.getResultString());
						String iattext = JsonParser.parseIatResult(result.getResultString());
						String text = mEditText.getText().toString()+"\n"+iattext;
						mEditText.setText(text);
		            } else {
		                Log.d(TAG, "recognizer result : null");
		                showTip("无识别结果");
		            }	
				}
			});
            
        }
        
        @Override
        public void onError(int errorCode) throws RemoteException {
			showTip("onError Code："	+ errorCode);
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
    }
	
	private void showTip(final String str)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mToast.setText(str);
				mToast.show();
		    }
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
