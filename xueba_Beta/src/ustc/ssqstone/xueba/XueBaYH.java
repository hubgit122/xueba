package ustc.ssqstone.xueba;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.renn.rennsdk.RennClient;
import com.renn.rennsdk.RennResponse;
import com.renn.rennsdk.RennExecutor.CallBack;
import com.renn.rennsdk.exception.RennException;
import com.renn.rennsdk.param.AccessControl;
import com.renn.rennsdk.param.PutBlogParam;

import android.R.integer;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
	protected static final String	SMS_STRING					= "ustc.ssqstone.xueba.SMS_String";
	protected static final String	SMS_NO						= "ustc.ssqstone.xueba.SMS_No";
	protected static final String	SMS_PHONE_NO				= "ustc.ssqstone.xueba.SMS_PhoneNo";
	protected static final String	PENDING_SMSs				= "pending_SMSs";
	protected static final String	LAST_WRITE					= "last_write";
	protected static final String	SHUTDOWN_TIME				= "shutdown_time";
	protected static final String	RESTRICTED_MODE				= "ustc.ssqstone.xueba.restricted_mode";
	protected static final String	START_TIME					= "ustc.ssqstone.xueba.start_time";
	protected static final boolean	myself						= true;
	protected static final boolean	debug						= true;
	protected static final boolean	debugSMS					= false;
	protected static final boolean	debugRest					= false;
	
	protected static final String	STATE						= "state";
	protected static final String	ACK_INTERRUTION				= "ack_interrution";
	protected static final String	INTERRUPTED_TIMES			= "interrupted times";
	protected static final String	HOW_MANY_INTERRUPTED_TIMES	= "how_many_interrupted_times";
	protected static final long		我							= 15556958998l;
	protected static final String	我s							= Long.valueOf(我).toString();
	protected static final long		我的监督人						= debug && debugSMS ? 10010 : 18297958221l;
	protected static final String	我的监督人s						= Long.valueOf(我的监督人).toString();
	protected static final String	默认监督人						= Long.valueOf(myself ? 我的监督人 : 我).toString();
	protected static final String	INFORM_NOT_SAVED			= "本次输入未保存";
	protected static final String	INFORM_SAVING_ERROR			= "本次输入有错误而不能保存, 再次按下返回键退出而不保存. ";
	protected static final String	PHONE_NUM					= "phone_num";
	protected static final String	NOW_EDITTING				= "nowEditting";
	protected static final String	CONFIRM_PHONE				= "confirmPhone";
	protected static final String	BACK_PRESSED				= "backPressed";
	protected static final String	INFORM_NOT_SAVING			= "注意, 直接退出时, 本次数据不被保存. ";
	protected static final String	INFORM_OFF					= "您的监督人身份刚刚被我撤销，请注意。";
	protected static final String	INFORM_ON					= "我已经设定您为学习监督短信的接收人。若您不认识我，请与我联系并要求我更改设置。\n如果您多次收到本短信，说明我曾更改程序数据。这是不好的。 ";
	protected static final String	KEY							= "key";
	protected static final String	INFORM_WON_T_SAVE			= "输入有误, 不能保存";
	protected static final String	INFORM_SAVED				= "已成功保存";
	// protected static final String DENIED = "denied";
	protected static final String	PARITY						= "parity";
	protected static final String	STUDY_DENIED				= "study_denied";
	protected static final String	NOON_DENIED					= "noon_denied";
	protected static final String	NIGHT_DENIED				= "night_denied";
	protected static final String	STUDY_END					= "study_end";
	protected static final String	VALUES						= "values";
	protected static final String	NOON_END					= "noon_end";
	protected static final String	NIGHT_END					= "night_end";
	protected static final String	NOON_BEGIN					= "noon_begin";
	protected static final String	NIGHT_BEGIN					= "night_begin";
	protected static final String	STUDY_BEGIN					= "study_begin";
	protected static final String	STUDY_EN					= "study_en";
	protected static final String	NIGHT_EN					= "night_en";
	protected static final String	NOON_EN						= "noon_en";
	private static final String		SMS_LOG						= "sms_log";
	protected static final String	DESTROY_RESTRICTION			= "ustc.ssqstone.xueba.destroy";
	protected final static String	SMS_SENT_S					= "ustc.ssqstone.xueba.SMS_Sent";
	
	protected static XueBaYH		ApplicationContext;
	// protected static boolean confirmPhone;
	
	static final String				APP_ID						= "168802";
	static final String				API_KEY						= "e884884ac90c4182a426444db12915bf";
	static final String				SECRET_KEY					= "094de55dc157411e8a5435c6a7c134c5";
	protected static final String	PENGDING_LOGS				= "pending_log";
	
	protected Handler				handler						= new Handler()
																{
																	@Override
																	public void handleMessage(Message msg)
																	{
																		switch (msg.what)
																		{
																			case SMS:
																				SharedPreferences values = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
																				XueBaYH.getApp().sendSMS((String) msg.obj, values.getString(XueBaYH.PHONE_NUM, XueBaYH.myself ? XueBaYH.我的监督人s : XueBaYH.我s), null);
																				break;
																			case TOAST:
																				Toast.makeText(getApp(), (String) msg.obj, Toast.LENGTH_LONG).show();
																				break;
																			case CHECK_PARITY:
																				boolean result = (XueBaYH.getApp().getParity() == getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE).getLong(XueBaYH.PARITY, -13));
																				if (!result)
																				{
																					Editor editor = getSharedPreferences(VALUES, MODE_PRIVATE).edit();
																					editor.putLong(PARITY, getParity());
																					editor.commit();
																					
																					punish();
																					showToast("有证据表明数据被破坏. 已经给设定的手机发送短信, 以儆效尤. ");
																				}
																				
																				if (msg.obj != null)
																				{
																					Editor editor = ((EditorWithParity) msg.obj).mEditor;
																					editor.commit();
																					editor.putLong(XueBaYH.PARITY, XueBaYH.getApp().getParity());
																					editor.commit();
																				}
																				break;
																			case CHECK_STATUS:
																				SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
																				EditorWithParity editor = new EditorWithParity(sharedPreferences);
																				Calendar calendar = Calendar.getInstance();
																				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM月dd日HH时mm分");
																				
																				if (sharedPreferences.getLong(LAST_WRITE, 0) > sharedPreferences.getLong(SHUTDOWN_TIME, 0) + 1000 * 3)
																				{
																					// 强制退出过.
																					String string = "我于" + simpleDateFormat.format(new Date(sharedPreferences.getLong(LAST_WRITE, 0))) + "强制退出了学霸银魂(一个监督不乱鼓捣手机的应用), 直到" + simpleDateFormat.format(calendar.getTime()) + "学霸银魂才得以重新启动. 这是非常不道德的行为, 我强烈谴责自己. \\timeStamp = " + sharedPreferences.getLong(LAST_WRITE, 0) + "\n";
																					
																					editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
																					
																					if (sharedPreferences.getLong(NIGHT_END, 0) <= calendar.getTimeInMillis())
																					{
																						string = "在没有监督的日子里, 我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(NIGHT_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(NIGHT_END, 0)) + "睡觉" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(NIGHT_END, 0) + "\n";
																						editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
																					}
																					if (sharedPreferences.getLong(NOON_END, 0) <= calendar.getTimeInMillis())
																					{
																						string = "在没有监督的日子里, 我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(NOON_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(NOON_END, 0)) + "睡午觉" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(NOON_END, 0) + "\n";
																						editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
																					}
																					if (sharedPreferences.getLong(STUDY_END, 0) <= calendar.getTimeInMillis())
																					{
																						string = "在没有监督的日子里, 我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(STUDY_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(STUDY_END, 0)) + "学习" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(STUDY_END, 0) + "\n";
																						editor.putString(PENGDING_LOGS, sharedPreferences.getString(PENGDING_LOGS, "") + string);
																					}
																				}
																				editor.commit();
																				
																				if (sharedPreferences.getLong(NIGHT_END, 0) <= calendar.getTimeInMillis())
																				{
																					editor.putBoolean(NIGHT_EN, false);
																				}
																				if (sharedPreferences.getLong(NOON_END, 0) <= calendar.getTimeInMillis())
																				{
																					editor.putBoolean(NOON_EN, false);
																				}
																				if (sharedPreferences.getLong(STUDY_END, 0) <= calendar.getTimeInMillis())
																				{
																					editor.putBoolean(STUDY_EN, false);
																				}
																				editor.commit();
																				break;
																			default:
																				break;
																		}
																		super.handleMessage(msg);
																	}
																};
	
	private BroadcastReceiver		shutdownBroadcastReceiver;
	private BroadcastReceiver		airReceiver					= new BroadcastReceiver()
																{
																	@Override
																	public void onReceive(Context context, Intent intent)
																	{
																		Bundle bundle = intent.getExtras();
																		if (bundle != null)
																		{
																			switch (bundle.getInt("state"))
																			{
																				case 0: // 飞行模式已关闭
																					SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
																					String pendingString = sharedPreferences.getString(PENDING_SMSs, "");
																					if (pendingString.length() > 0)
																					{
																						EditorWithParity editor = new EditorWithParity(sharedPreferences);
																						String[] strings = pendingString.split(";;");
																						for (int i = 0; i < strings.length; i++)
																						{
																							String string = strings[i];
																							if (string.contains("to"))
																							{
																								sendSMS(string.substring(string.indexOf("content: ") + 8), string.substring(4, 15), string.contains("; mode: ") ? string.substring(string.indexOf("; mode: ") + "; mode: ".length(), string.indexOf(';', string.indexOf("mode: "))) : null);
																							}
																							String bufferString = "";
																							for (int j = i + 1; j < strings.length; j++)
																							{
																								String string_ = strings[j] + ";;";
																								bufferString += string_;
																							}
																							editor.putString(PENDING_SMSs, bufferString);
																							editor.commit();
																						}
																					}
																					break;
																				case 1: // 飞行模式正在关闭
																					break;
																				case 3: // 飞行模式已开启
																					break;
																			}
																		}
																	}
																};
	protected RennClient			rennClient;
	private SMS_SentReceiver		smsSentReceiver;
	
	private static final int		SMS							= 2;
	private static final int		TOAST						= 3;
	private static final int		CHECK_PARITY				= 4;
	private static final int		CHECK_STATUS				= 5;
	protected static final String	LOCKED_TIME					= "locked_time";
	protected static final String	USAGE_TIME					= "usage_time";
	
	public void onCreate()
	{
		super.onCreate();
		
		if (debug)
		{
			SharedPreferences sharedPreferences = getSharedPreferences(SMS_LOG, MODE_PRIVATE);
			Map<String, ?> map = sharedPreferences.getAll();
			
			if (!map.isEmpty())
			{
				Set<String> set = map.keySet();
				
				Editor editor = sharedPreferences.edit();
				
				for (String key : set)
				{
					editor.remove(key);
					if (!key.matches("[0-9]+.[0-9]+.[0-9]+ [0-9]+:[0-9]+:[0-9]+"))
					{
						continue;
					}
					String sms = sharedPreferences.getString(key, "");
					
					try
					{
						if (!sms.isEmpty())
						{
							int year = Integer.valueOf(key.substring(0, 4));
							int month = Integer.valueOf(key.substring(5, 7));
							int day = Integer.valueOf(key.substring(8, 10));
							int hour = Integer.valueOf(key.substring(11, 13));
							int min = Integer.valueOf(key.substring(14, 16));
							int second = Integer.valueOf(key.substring(17, 19));
							
							Calendar calendar = Calendar.getInstance();
							calendar.set(year, month, day, hour, min, second);
							
							ContentValues values = new ContentValues();
							values.put("address", sms.substring(4, 15));
							values.put("body", sms.substring(26));
							values.put("date", calendar.getTimeInMillis());
							values.put("read", 1);
							values.put("type", 2);
							getContentResolver().insert(mSmsUri, values);
						}
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
				editor.commit();
			}
		}
		ApplicationContext = this;
		
		shutdownBroadcastReceiver = new BroadcastReceiver()
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
		
		intentFilter = new IntentFilter("android.intent.action.SERVICE_STATE");
		registerReceiver(airReceiver, intentFilter);
		// confirmPhone= false;
	}
	
	private void accessRoot()
	{
		Process process = null;
		try
		{
			process = Runtime.getRuntime().exec("su");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			process.destroy();
		}
	}
	
	@Override
	public void onTerminate()
	{
		if (shutdownBroadcastReceiver != null)
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
		// stopService(new Intent("ustc.ssqstone.xueba.MonitorService"));
		// //在Service退出的时候加入短信通知, 所以不能在此关闭. 其实关闭Service没啥意思.
		startService(new Intent("ustc.ssqstone.xueba.MonitorService"));
	}
	
	public static Uri	mSmsUri	= Uri.parse("content://sms/inbox");
	
	public class SMS_SentReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			try
			{
				Bundle bundle = intent.getExtras();
				switch (getResultCode())
				{
					case Activity.RESULT_OK:
						Editor editor;
						if (bundle.containsKey(DESTROY_RESTRICTION))
						{
							destoryRestrictedActivity(bundle.getString(DESTROY_RESTRICTION));
						}
						XueBaYH.getApp().showToast("已向" + bundle.getString(SMS_PHONE_NO) + "发送短信:\n" + bundle.getString(SMS_STRING));
						editor = getSharedPreferences(XueBaYH.SMS_LOG, MODE_PRIVATE).edit();
						editor.putString(getSimpleTime(Calendar.getInstance().getTimeInMillis()), "to: " + bundle.getString(SMS_PHONE_NO) + "; content: " + bundle.getString(SMS_STRING) + "; No." + bundle.getInt(SMS_NO));
						editor.commit();
						
						ContentValues values = new ContentValues();
						values.put("address", bundle.getString(SMS_PHONE_NO));
						values.put("body", bundle.getString(SMS_STRING));
						values.put("date", Calendar.getInstance().getTimeInMillis());
						values.put("read", 1);
						values.put("type", 2);
						getContentResolver().insert(mSmsUri, values);
						
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
					case SmsManager.RESULT_ERROR_RADIO_OFF:
					case SmsManager.RESULT_ERROR_NULL_PDU:
					default:
						EditorWithParity editor1;
						/* 发送短信失败 */
						XueBaYH.getApp().showToast("向" + bundle.getString(SMS_PHONE_NO) + "发送短信:\n" + bundle.getString(SMS_STRING) + "失败, 已经记档, 在有网的时候自动发送. ");
						SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
						editor1 = new EditorWithParity(sharedPreferences);
						String pendingSMS = sharedPreferences.getString(PENDING_SMSs, "");
						pendingSMS += "to: " + bundle.getString(SMS_PHONE_NO) + (bundle.containsKey(DESTROY_RESTRICTION) ? "; mode: " + bundle.getString(DESTROY_RESTRICTION) : "") + "; content: " + bundle.getString(SMS_STRING) + ";;";
						editor1.putString(PENDING_SMSs, pendingSMS);
						editor1.commit();
						break;
				}
			}
			catch (Exception e)
			{
				e.getStackTrace();
			}
		}
	}
	
	protected void sendSMS(String smsString, String phoneText, String mode)
	{
		if (!debug || debugSMS)
		{
			// setOnLine();
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
				if (i == 1 && (mode != null) && (!mode.isEmpty()))
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
			showToast("此处向" + phoneText + "发送短信:\n" + smsString);
		}
	}
	
	protected static String getSimpleTime(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		SimpleDateFormat simpleFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
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
		Message msg = new Message();
		msg.what = TOAST;
		msg.obj = string;
		handler.sendMessage(msg);
	}
	
	// protected void showShortToast(String string)
	// {
	// Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
	// }
	
	protected boolean isInAirplaneMode()
	{
		return android.provider.Settings.System.getInt(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, 0) == 1;
	}
	
	// /**
	// * 随便弄一个加密，防止手动改。
	// *
	// * @return 校验码
	// */
	//
	// protected long getDeniedParity()
	// {
	// SharedPreferences sharedPreferences = getSharedPreferences("denied",
	// MODE_PRIVATE);
	// long key = sharedPreferences.getLong("key", XueBaYH.myself ? 18297958221l
	// : 15556958998l);
	//
	// double result = key * (sharedPreferences.getBoolean("night_denied",
	// false) ? 1.1 : 1.2) * (sharedPreferences.getBoolean("noon_denied", false)
	// ? 1.1 : 1.2) * (sharedPreferences.getBoolean("study_denied", false) ? 1.1
	// : 1.2);
	//
	// return (long) (result / 20130906);
	// }
	
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
		
		double result = ((sharedPreferences.getBoolean(STUDY_EN, false) ? 73 : 84) * 346 + (sharedPreferences.getBoolean(NOON_EN, false) ? 7 : 23) * 342 + (sharedPreferences.getBoolean(NIGHT_EN, false) ? 13 : 53) * 454 + (String.valueOf(sharedPreferences.getLong(STUDY_BEGIN, 477)) + String.valueOf(sharedPreferences.getLong(NIGHT_BEGIN, 57)) + String.valueOf(sharedPreferences.getLong(NOON_BEGIN, 53)) + String.valueOf(sharedPreferences.getLong(NIGHT_END, 46)) + String.valueOf(sharedPreferences.getLong(NOON_END, 5)) + String.valueOf(sharedPreferences.getLong(STUDY_END, 153)) + sharedPreferences.getString(PHONE_NUM, myself ? 我s : 我的监督人s) + String.valueOf(sharedPreferences.getLong(SHUTDOWN_TIME, 43)) + String.valueOf(sharedPreferences.getLong(LAST_WRITE, 33))
				+ sharedPreferences.getString(PENDING_SMSs, "") + sharedPreferences.getString(PENGDING_LOGS, "") + Long.valueOf(sharedPreferences.getLong(LOCKED_TIME, 0))).hashCode());
		return (long) result;
	}
	
	protected void setOffLine()
	{
		// boolean airplaneModeOn = XueBaYH.getApp().isInAirplaneMode();
		// if (!airplaneModeOn)
		// {
		// if
		// (!android.provider.Settings.System.putString(this.getContentResolver(),
		// android.provider.Settings.System.AIRPLANE_MODE_ON, "1"))
		// {
		// XueBaYH.getApp().showToast("自动打开飞行模式失败，请手动打开飞行模式。");
		// }
		// else
		// {
		// Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
		// intent.putExtra(STATE, airplaneModeOn);
		// sendBroadcast(intent);
		// }
		// }
	}
	
	private boolean setOnLine()
	{
		boolean airplaneModeOn = XueBaYH.getApp().isInAirplaneMode();
		if (airplaneModeOn)
		{
			if (!android.provider.Settings.System.putString(this.getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, "0"))
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
		if (debug)
		{
			vibrator.vibrate(new long[] { 0, 80 }, -1);
		}
		else
		{
			vibrator.vibrate(new long[] { 0, 400, 400, 400, 400, 200, 200, 200, 200, 400, 400 }, -1);
		}
	}
	
	protected void vibrateOh()
	{
		Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		if (debug)
		{
			vibrator.vibrate(new long[] { 0, 80 }, -1);
		}
		else
		{
			vibrator.vibrate(new long[] { 0, 800, 800, 800, 800, 800 }, -1);
		}
	}
	
	protected void killBackGround()
	{
		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
		
		for (Iterator<ActivityManager.RunningAppProcessInfo> iterator = appList.iterator(); iterator.hasNext();)
		{
			RunningAppProcessInfo runningAppProcessInfo = (RunningAppProcessInfo) iterator.next();
			if (runningAppProcessInfo.importance > RunningAppProcessInfo.IMPORTANCE_SERVICE)
			{
				for (String pkg : runningAppProcessInfo.pkgList)
					am.killBackgroundProcesses(pkg);
			}
		}
		
		showToast("已清理后台应用. ");
	}
	
	public RennClient iniRennClient(Context context)
	{
		rennClient = RennClient.getInstance(context);
		rennClient.init(XueBaYH.APP_ID, XueBaYH.API_KEY, XueBaYH.SECRET_KEY);
		rennClient.setScope("read_user_blog read_user_photo read_user_status read_user_album " + "read_user_comment read_user_share publish_blog publish_share " + "send_notification photo_upload status_update create_album " + "publish_comment publish_feed");
		rennClient.setTokenType("bearer");
		return rennClient;
	}
	
	public void onShutdown()
	{
		Calendar calendar = Calendar.getInstance();
		SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
		EditorWithParity editor = new EditorWithParity(sharedPreferences);
		editor.putLong(SHUTDOWN_TIME, calendar.getTimeInMillis());
		editor.commit();
		Log.i("xueba", "关机记录成功");
	}
	
	protected void destoryRestrictedActivity(String mode)
	{
		Intent intent = new Intent(XueBaYH.this, RestrictedModeActivity.class);
		intent.putExtra(DESTROY_RESTRICTION, true);
		intent.putExtra(RESTRICTED_MODE, mode);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	protected void checkParity(EditorWithParity editor)
	{
		Message message = new Message();
		message.what = CHECK_PARITY;
		message.obj = editor;
		handler.sendMessage(message);
	}
	
	protected void punish()
	{
		sendSMS("我已经设定您为学习监督短信的接收人。若您不认识我，请与我联系并要求我更改设置。\n如果您多次收到本短信，说明我曾经更改程序数据。这是不好的。", getSharedPreferences(VALUES, MODE_PRIVATE).getString(PHONE_NUM, 默认监督人), null);
	}
	
	/**
	 * 检查是否强制退出过, 和是否跳过任务.
	 * 
	 * 先根据lastWrite和shutdown的关系判断是否强退过. 如果没有, 那么取消所有结束时间在现在之前的任务. 如果强退过, 记录强退信息,
	 * 也记录所有跳过的任务.
	 */
	protected void checkStatus()
	{
		checkParity(null);
		handler.sendEmptyMessage(CHECK_STATUS);
	}
	
	protected void sendRennLog(final String content)
	{
		if (debug)
		{
			Message message = new Message();
			message.what = TOAST;
			message.obj = "此处向人人发日志" + content;
			handler.sendMessage(message);
			return;
		}
		
		PutBlogParam param = new PutBlogParam();
		param.setTitle("我是来谴责自己的@" + Calendar.getInstance().getTimeInMillis());
		param.setContent(content);
		param.setAccessControl(AccessControl.PUBLIC);
		try
		{
			RennClient rennClient = iniRennClient(XueBaYH.this);
			
			if (!rennClient.isLogin())
			{
				showToast("登录人人以体验更多功能");
				return;
			}
			
			rennClient.getRennService().sendAsynRequest(param, new CallBack()
			{
				@Override
				public void onSuccess(RennResponse response)
				{
					XueBaYH.getApp().showToast("我已本着惩前毖后治病救人的精神代你发表了一篇自我谴责的日志. ");
				}
				
				@Override
				public void onFailed(String errorCode, String errorMessage)
				{
					SharedPreferences sharedPreferences = getSharedPreferences(VALUES, MODE_PRIVATE);
					EditorWithParity editor = new EditorWithParity(sharedPreferences);
					
					editor.putString(PENGDING_LOGS, content + sharedPreferences.getString(PENGDING_LOGS, ""));
					
					editor.commit();
				}
			});
		}
		catch (RennException e1)
		{
			e1.printStackTrace();
		}
	}
}
