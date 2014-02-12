package ustc.ssqstone.xueba;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import com.renn.rennsdk.RennClient;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

/**
 * 方便服务和活动处理的应用类
 * 
 * @author ssqstone
 */
public class XueBaYH extends Application
{
	protected static final String	SMS_STRING	= "ustc.ssqstone.xueba.SMS_String";
	protected static final String	SMS_NO	= "ustc.ssqstone.xueba.SMS_No";
	protected static final String	SMS_PHONE_NO	= "ustc.ssqstone.xueba.SMS_PhoneNo";
	protected static final String	PENDING_SMSs	= "pending_SMSs";
	protected static final String	LAST_WRITE	= "last_write";
	protected static final String	SHUTDOWN_TIME	= "shutdowntime";
	protected static final String	RESTRICTED_MODE	= "ustc.ssqstone.xueba.restricted_mode";
	protected static final String	START_TIME	= "ustc.ssqstone.xueba.start";
	protected static final boolean myself = true;
	protected static final boolean debug = true;
	protected static final boolean debugSMS = true;

	protected static final String	STATE	= "state";
	protected static final String	ACK_INTERRUTION	= "ack_interrution";
	protected static final String	INTERRUPTED_TIMES	= "interrupted times";
	protected static final String	HOW_MANY_INTERRUPTED_TIMES	= "how_many_interrupted_times";
	protected static final long	我	 = 15556958998l;
	protected static final String	我s	= Long.valueOf(我).toString();
	protected static final long	我的监督人	= debug?10010:18297958221l;
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
	protected static final String	DESTROY_RESTRICTION	= "ustc.ssqstone.xueba.destroy";
	protected final static String	SMS_SENT_S = "ustc.ssqstone.xueba.SMS_Sent";
	protected static final String	PENDING_RESQUESTS	= "PendingRequests";
	
	protected static XueBaYH ApplicationContext;
//	protected static boolean confirmPhone;

	static final String APP_ID = "168802";
	static final String API_KEY = "e884884ac90c4182a426444db12915bf";
	static final String SECRET_KEY = "094de55dc157411e8a5435c6a7c134c5";
	
	private BroadcastReceiver shutdownBroadcastReceiver;
	
	protected RennClient rennClient;
	private SMS_SentReceiver smsSentReceiver;
	
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
			}
		};
		
		IntentFilter intentFilter;
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SHUTDOWN);
		registerReceiver(shutdownBroadcastReceiver, intentFilter);

		/* 自定义IntentFilter为SENT_SMS_ACTIOIN Receiver */  
		intentFilter = new IntentFilter(SMS_SENT_S);
		smsSentReceiver = new SMS_SentReceiver();
		registerReceiver(smsSentReceiver, intentFilter); 
		
//		confirmPhone= false;
	}
	
	@Override
	public void onTerminate()
	{
		if (shutdownBroadcastReceiver!=null)
		{
			unregisterReceiver(shutdownBroadcastReceiver);
		}
		
	    if (smsSentReceiver != null)
	    {  
	        unregisterReceiver(smsSentReceiver);  
	    }
		super.onTerminate();
	}

	protected static XueBaYH getApp()
	{
		return ApplicationContext;
	}

	protected void restartMonitorService()
	{
//		stopService(new Intent("ustc.ssqstone.xueba.MonitorService"));		//在Service退出的时候加入短信通知, 所以不能在此关闭. 其实关闭Service没啥意思. 
		startService(new Intent("ustc.ssqstone.xueba.MonitorService"));
	}

	public class SMS_SentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				Editor editor;
				Bundle bundle = intent.getExtras();
				switch (getResultCode())
				{
					case Activity.RESULT_OK:
						if (bundle.containsKey(DESTROY_RESTRICTION))
						{
							destoryRestrictedActivity(bundle.getString(DESTROY_RESTRICTION));
						}
						XueBaYH.getApp().showToast("已向"+bundle.getString(SMS_PHONE_NO)+"发送短信:\n"+bundle.getString(SMS_STRING));
						editor = getSharedPreferences(XueBaYH.SMS_LOG, MODE_PRIVATE).edit();
						editor.putString(getSimpleTime(Calendar.getInstance().getTimeInMillis()),"to: "+ bundle.getString(SMS_PHONE_NO)+ "; content: " +bundle.getString(SMS_STRING) + "; No."+ bundle.getInt(SMS_NO));
						editor.commit();
						
						editor =getSharedPreferences(VALUES, MODE_PRIVATE).edit();
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					case SmsManager.RESULT_ERROR_RADIO_OFF:
					case SmsManager.RESULT_ERROR_NULL_PDU:
					default:
						/* 发送短信失败 */
						XueBaYH.getApp().showToast("向"+bundle.getString(SMS_PHONE_NO)+"发送短信:\n"+bundle.getString(SMS_STRING)+"失败, 已经记档, 在有网的时候自动发送. ");
						SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
						editor = sharedPreferences.edit();
						String pendingSMS = sharedPreferences.getString(PENDING_SMSs, "");
						pendingSMS += ";;to: "+ bundle.getString(SMS_PHONE_NO)+ "; content: " +bundle.getString(SMS_STRING);
						editor.putString(PENDING_SMSs, pendingSMS);
						editor.commit();
						break;
				}
			}
			catch (Exception e)
			{
				e.getStackTrace();
			}
		}
	}

	protected void sendSMS(String smsString,String phoneText, String mode)
	{
		if (!debug||debugSMS)
		{
//			setOnLine();
			SmsManager sms = SmsManager.getDefault();
			List<String> texts = sms.divideMessage(smsString);

			int i = 0;
			for (String text : texts)
			{
				Intent itSend = new Intent(SMS_SENT_S);
				Bundle bundle = new Bundle();
				bundle.putString(SMS_PHONE_NO, phoneText);
				bundle.putInt(SMS_NO, ++i);
				bundle.putString(SMS_STRING, text);
				if (i==1&&(mode!=null)&&(!mode.isEmpty()))
				{
					bundle.putString(DESTROY_RESTRICTION, mode);
				}
				itSend.putExtras(bundle);
				PendingIntent mSendPI = PendingIntent.getBroadcast(getApplicationContext(), (int) System.currentTimeMillis(), itSend, PendingIntent.FLAG_UPDATE_CURRENT);
				
				sms.sendTextMessage(phoneText, null, text, mSendPI, null);
			}
		}
		else
		{
			showToast("此处向"+phoneText+"发送短信:\n"+smsString);
		}
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
				+ Math.log((double) Long.valueOf(sharedPreferences.getLong(LAST_WRITE, 0))) *143
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

//	private boolean setOnLine()
//	{
//		boolean airplaneModeOn=XueBaYH.getApp().isInAirplaneMode();
//		if (airplaneModeOn)
//		{
//			if(!android.provider.Settings.System.putString(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON , "0"))
//			{
//				XueBaYH.getApp().showToast("自动关闭飞行模式失败，请手动关闭飞行模式。");
//				return false;
//			}
//			else 
//			{
//				Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
//				intent.putExtra(STATE, !airplaneModeOn);
//				sendBroadcast(intent);
//				return true;
//			}
//		}
//		else 
//		{
//			return true;
//		}
//	}

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
	
	protected void destoryRestrictedActivity(String mode)
	{
		Intent intent = new Intent(XueBaYH.this, RestrictedModeActivity.class);
		intent.putExtra(DESTROY_RESTRICTION, true);
		intent.putExtra(RESTRICTED_MODE, mode);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
}
