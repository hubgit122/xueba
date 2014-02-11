package ustc.ssqstone.xueba;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.renn.rennsdk.RennClient;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * 方便服务和活动处理的应用类
 * 
 * @author ssqstone
 */
public class XueBaYH extends Application
{
	protected static final String	SHUTDOWN_TIME	= "shutdowntime";
	protected static final boolean myself = true;
	protected static final boolean debug = false;

	protected static final String	STATE	= "state";
	protected static final String	ACK_INTERRUTION	= "ack_interrution";
	protected static final String	INTERRUPTED_TIMES	= "interrupted times";
	protected static final String	HOW_MANY_INTERRUPTED_TIMES	= "how_many_interrupted_times";
	protected static final long		我	 = 15556958998l;
	protected static final String	我s	= Long.valueOf(我).toString();
	protected static final long		我的监督人	= 18297958221l;
	protected static final String	我的监督人s	= Long.valueOf(我的监督人).toString();
	protected static final String	INFORM_NOT_SAVED	= "本次输入未保存";
	protected static final String	INFORM_SAVING_ERROR	= "本次输入有错误而不能保存, 再次按下返回键退出而不保存. ";
	protected static final String	PHONE_NUM	= "phone_num";
	protected static final String	NOW_EDITTING	= "nowEditting";
	protected static final String	CONFIRM_PHONE	= "confirmPhone";
	protected static final String	BACK_PRESSED	= "backPressed";
	protected static final String	INFORM_NOT_SAVING	= "注意, 直接退出时, 本次数据不被保存. ";
	protected static final String	INFORM_OFF	= "您的监督人身份刚刚被我撤销，请注意。";
	protected static final String	INFORM_ON	= "我已经设定您为学习监督短信的接收人。若您不认识我，请与我联系并要求我更改设置。\n如果您多次收到本短信，说明我曾更改程序数据。这是不好的。 ";
	protected static final String	KEY	= "key";
	protected static final String	INFORM_WON_T_SAVE	= "输入有误, 不能保存";
	protected static final String	INFORM_SAVED	= "已成功保存";
//	protected static final String	DENIED	= "denied";
	protected static final String	PARITY	= "parity";
	protected static final String	STUDY_DENIED	= "study_denied";
	protected static final String	NOON_DENIED	= "noon_denied";
	protected static final String	NIGHT_DENIED	= "night_denied";
	protected static final String	STUDY_END	= "study_end";
	protected static final String	VALUES	= "values";
	protected static final String	NOON_END	= "noon_end";
	protected static final String	NIGHT_END	= "night_end";
	protected static final String	NOON_BEGIN	= "noon_begin";
	protected static final String	NIGHT_BEGIN	= "night_begin";
	protected static final String	STUDY_BEGIN	= "study_begin";
	protected static final String	STUDY_EN	= "study_en";
	protected static final String	NIGHT_EN	= "night_en";
	protected static final String	NOON_EN	= "noon_en";
	private static final String	SMS_LOG	= "sms_log";
	protected static XueBaYH ApplicationContext;
//	protected static boolean confirmPhone;

	static final String APP_ID = "168802";
	static final String API_KEY = "e884884ac90c4182a426444db12915bf";
	static final String SECRET_KEY = "094de55dc157411e8a5435c6a7c134c5";
	
	private BroadcastReceiver shutdownBroadcastReceiver;
	
//	private Toast toast=null;
	protected RennClient rennClient;
	
	public void onCreate()
	{
		super.onCreate();
		ApplicationContext = this;

		shutdownBroadcastReceiver= new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				onShutdown();
				Log.i("","用广播接收器得到的关机信号");
			}
		};
		
		IntentFilter intentFilter;
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SHUTDOWN);
		registerReceiver(shutdownBroadcastReceiver, intentFilter);
//		confirmPhone= false;
	}
	
	@Override
	public void onTerminate()
	{
		unregisterReceiver(shutdownBroadcastReceiver);
		super.onTerminate();
	}

	protected static XueBaYH getApp()
	{
		return ApplicationContext;
	}

	protected void startReportService()
	{
		startService(new Intent("ustc.ssqstone.xueba.ReportService"));
	}
	
	protected void restartMonitorService()
	{
//		stopService(new Intent("ustc.ssqstone.xueba.MonitorService"));		//在Service退出的时候加入短信通知, 所以不能在此关闭. 其实关闭Service没啥意思. 
		startService(new Intent("ustc.ssqstone.xueba.MonitorService"));
	}

	protected boolean sendSMS(String smsString,String phoneText)
	{
		boolean result = setOnLine();
		if (!XueBaYH.debug)
		{
			SmsManager sms = SmsManager.getDefault();
			List<String> texts = sms.divideMessage(smsString);
			for (String text : texts)
			{
				sms.sendTextMessage(phoneText, null, text, null, null);       
			}
			XueBaYH.getApp().showToast("已向"+phoneText+"发送"+texts.size()+"条短信:\n"+smsString);
			Editor editor = getSharedPreferences(XueBaYH.SMS_LOG, MODE_PRIVATE).edit();
			editor.putString(getSimpleTime(Calendar.getInstance().getTimeInMillis()),"to: "+ phoneText+ "; content: " +smsString);
			editor.commit();
		}
		else 
		{
			showToast("此处向"+phoneText+"发送短信:\n"+smsString);
		}
		return result;
	}
	
	protected static String getSimpleTime(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss"); 
		return simpleFormat.format(calendar.getTime());
	}
	
	protected static String getSimpleDate(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy.MM.dd"); 
		return simpleFormat.format(calendar.getTime());
	}
	
	protected void showToast(String string) 
	{
//		if (toast == null)
//		{
//			toast = Toast.makeText(this, string, Toast.LENGTH_LONG);
//			toast.show();
//		}
//		else
//		{
//			toast.cancel();
//			toast = null;
//		}
		Toast.makeText(this, string, Toast.LENGTH_LONG).show();
	}
	
//	protected void showShortToast(String string)
//	{
//		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
//	}
	
	protected boolean isInAirplaneMode()
	{
		return android.provider.Settings.System.getInt(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON , 0)==1;
	}
	
	/**
	 * 随便弄一个加密，防止手动改。
	 * 
	 * @return 校验码
	 */
	
	protected long getDeniedParity()
	{
		SharedPreferences sharedPreferences = getSharedPreferences("denied",MODE_PRIVATE);
		long key = sharedPreferences.getLong("key", XueBaYH.myself?18297958221l:15556958998l);
		
		double result = key * (sharedPreferences.getBoolean("night_denied", false)?1.1:1.2) *
		(sharedPreferences.getBoolean("noon_denied", false)?1.1:1.2) * 
		(sharedPreferences.getBoolean("study_denied", false)?1.1:1.2);
		
		return (long)(result/20130906);
	}
	
	/**
	 * 防止存档数据篡改. 防止数据篡改的方法是: 在文件中加入校验域. 数据校验通过后才能作为有效数据被读取.
	 * 防止数据篡改应该在保存数据时写入当前parity, 在读取数据时检查parity. 随便定一个校验规则. 只要将重要信息都校验过就好.
	 * 
	 * *注意, 仅在保存数据时checkData成功后使用.
	 * 
	 * @return 校验码
	 */
	protected long getParity()
	{
		SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
		
		double result = 
				(sharedPreferences.getBoolean(STUDY_EN, false) ? 1 : 0) * 3464
				+ (sharedPreferences.getBoolean(NOON_EN, false) ? 1 : 0) * 342453
				+ (sharedPreferences.getBoolean(NIGHT_EN, false) ? 1 : 0) * 454325
				+ Math.log((double) sharedPreferences.getLong(STUDY_BEGIN, 0) *3414
				+ Math.log((double) sharedPreferences.getLong(NIGHT_BEGIN, 0)) *45134
				+ Math.log((double) sharedPreferences.getLong(NOON_BEGIN, 0)) *123412341
				+ Math.log((double) sharedPreferences.getLong(NIGHT_END, 0)) *124
				+ Math.log((double) sharedPreferences.getLong(NOON_END, 0)) *14314
				+ Math.log((double) Long.valueOf(sharedPreferences.getString(PHONE_NUM, myself?我s:我的监督人s))) *14314
				+ Math.log((double) Long.valueOf(sharedPreferences.getLong(SHUTDOWN_TIME, 0))) *143
				);
		return (long) result;
	}

//	private void setOffLine()
//	{
//		boolean airplaneModeOn= XueBaYH.getApp().isInAirplaneMode();
//		if (!airplaneModeOn)
//		{
//			if(!android.provider.Settings.System.putString(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON , "1"))
//			{
//				XueBaYH.getApp().showToast("自动打开飞行模式失败，请手动打开飞行模式。");
//			}
//			else 
//			{
//				Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//				intent.putExtra(STATE, airplaneModeOn);
//				sendBroadcast(intent);
//			}
//		}
//	}

	private boolean setOnLine()
	{
		boolean airplaneModeOn=XueBaYH.getApp().isInAirplaneMode();
		if (airplaneModeOn)
		{
			if(!android.provider.Settings.System.putString(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON , "0"))
			{
				XueBaYH.getApp().showToast("自动关闭飞行模式失败，请手动关闭飞行模式。");
				return false;
			}
			else 
			{
				Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
				intent.putExtra(STATE, !airplaneModeOn);
				sendBroadcast(intent);
				return true;
			}
		}
		else 
		{
			return true;
		}
	}

	protected String getPhoneNum()
	{
		TelephonyManager phoneManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String phsString = phoneManager.getLine1Number();
		return phsString;
	}
	
	protected void vibrateOK() 
	{
		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(new long[]{0,400,400,400,400,200,200,200,200,400,400},-1);
	}

	protected void vibrateOh() 
	{
		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(new long[]{0,800,800,800,800,800},-1);
	}
	
	protected void killBackGround() 
	{
		ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
		
		for (Iterator<ActivityManager.RunningAppProcessInfo> iterator = appList.iterator(); iterator.hasNext();) 
		{
			RunningAppProcessInfo runningAppProcessInfo = (RunningAppProcessInfo) iterator .next();
			if (runningAppProcessInfo.importance > RunningAppProcessInfo.IMPORTANCE_SERVICE) 
			{
				for(String pkg : runningAppProcessInfo.pkgList)
					am.killBackgroundProcesses(pkg);
			}
		}
		
		showToast("已清理后台应用. ");
	}
	
	public RennClient iniRennClient(Context context)
	{
		rennClient = RennClient.getInstance(context);
		rennClient.init(XueBaYH.APP_ID, XueBaYH.API_KEY, XueBaYH.SECRET_KEY);
		rennClient
				.setScope("read_user_blog read_user_photo read_user_status read_user_album "
						+ "read_user_comment read_user_share publish_blog publish_share "
						+ "send_notification photo_upload status_update create_album "
						+ "publish_comment publish_feed");
		rennClient.setTokenType("bearer");		
		return rennClient;
	}

	public void onShutdown()
	{
		Calendar calendar = Calendar.getInstance();
		SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
		Editor editor = sharedPreferences.edit();
		editor.putLong(SHUTDOWN_TIME, calendar.getTimeInMillis());
		editor.commit();
		editor.putLong(PARITY, getParity());
		editor.commit();
	}
}
