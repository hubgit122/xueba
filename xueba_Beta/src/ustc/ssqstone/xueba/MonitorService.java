package ustc.ssqstone.xueba;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

/**
 * 发现一个bug，studyEn的值会在我看不到的地方发生翻转。
 */

/**
 * 开机或在设置程序中按确定按钮均会开启此Service, 该service一直在后台执行所要进行的监视. 
 * 设置Activity在一个pref文件中储存当前的设置, 此Service只读取设置, 不会改变设置. 
 * **注:因为要加入强制晚睡等规则, 所以在最终的实现上service是会改变pref文件的. 
 * **原则是先写好基本功能再加强制机制. 只要在service的onBeginCommand函数最前方插入强制机制就可以了. 
 * 
 * 监视频率是可变的, 如果出现违规, 就唤醒RestrictedModeActivity. 
 * 
 * 该Activity会按照intent附加的信息启动相应的界面. (附加描述是睡觉还是学习)
 * 监视的方式是查看最上层的activity, 发现不在允许的包内, 就调用punish函数, 调出RestrictedModeActivity. 
 * 到任务时间后就要求退出RestrictedModeActivity. 
 * 
 * @author ssqstone
 */

/**
 * 在同一个应用任何地方调用 startService() 方法就能启动 Service 了，然后系统会回调 Service 类的 onCreate() 以及
 * onBegin() 方法。 这样启动的 Service 会一直运行在后台，直到 Context.stopService() 或者 selfStop()
 * 方法被调用。 如果一个 Service 已经被启动，其他代码再试图调用 startService() 方法，是不会执行 onCreate()
 * 的，但会重新执行一次 onBegin() 。
 * 
 * 为了方便通过放弃任务而修改任务时间, 数据应在onBeginCommand方法里载入.
 */
public class MonitorService extends Service
{
	private static final String	LAST_SURF_DATE			= "last surf date";
	private static final String	SURF_TIME_OF_S			= "surf time of ";
//	private static final String	SURF_TIME_OF_TODAY_S	= "surf time of today: ";
	private boolean				screenLocked			= false;
	private static final int	SMS						= 2;
	private static final int	TOAST					= 3;
	// private static final int PUNISH=1;
	private static final int	TO_RESTRICT				= 4;
	private static final String	SURF_TIME_LOG			= "surf_time_log";
	// private static final int INT_ACC = 5;
	// protected static final int INT_ZERO = 6;
	/**
	 * 单位是毫秒.
	 */
	private int					checkInterval;
	private boolean				informed;
	static private Handler		handler;
	
	private NetStateReceiver	netStateReceiver;
	
	protected enum Status
	{
		sleeping_noon("午觉"), sleeping_night("睡觉"), studying("学习"), halting("等待"), error("错误");
		
		private String	chineseString;
		
		private Status(String chinese)
		{
			this.chineseString = chinese;
		}
		
		public String getLocalString()
		{
			return chineseString;
		}
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		screenOffBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				stopCurrentThread(monitorTask);
			}
		};
		screenOnBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				startThread(MonitorTask.class.getName());
			}
		};
		
		netStateReceiver = new NetStateReceiver();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(netStateReceiver, intentFilter);
		
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(screenOnBroadcastReceiver, intentFilter);
		
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOffBroadcastReceiver, intentFilter);
		
		handler = new Handler()
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
						XueBaYH.getApp().showToast((String) msg.obj);
						break;
					case TO_RESTRICT:
						Intent intent = new Intent(MonitorService.this, RestrictedModeActivity.class);
						intent.putExtra(XueBaYH.RESTRICTED_MODE, status.getLocalString());
						intent.putExtra(XueBaYH.START_TIME, startTime);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						break;
					default:
						break;
				}
				super.handleMessage(msg);
			}
		};
	}
	
	/**
	 * 用在任务开启, 屏幕解锁, 或检查结束后调用. 更新当前状态和监视时间步长.
	 * 
	 * *注意: 状态完全由是否锁屏，是否开启三类任务决定。
	 */
	private void refreshStatus()
	{
		Calendar calendar = Calendar.getInstance();
		long now = calendar.getTimeInMillis();

		EditorWithParity editor = new EditorWithParity(getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE));
		boolean removeRestriction = false;
		
		if (studyEn && (now > studyEnd))
		{
			editor.putBoolean(XueBaYH.STUDY_EN, false);
			studyEn = false;
			removeRestriction = true;
			status = Status.studying;
		}
		if (noonEn && (now > noonEnd))
		{
			editor.putBoolean(XueBaYH.NOON_EN, false);
			noonEn = false;
			removeRestriction = true;
			status = Status.sleeping_noon;
		}
		if (nightEn && (now > nightEnd))
		{
			editor.putBoolean(XueBaYH.NIGHT_EN, false);
			nightEn = false;
			removeRestriction = true;
			status = Status.sleeping_night;
		}
		
		if (removeRestriction)
		{
			editor.commit();
			
			XueBaYH.getApp().destoryRestrictedActivity(status.getLocalString());
		}
		
		if (screenLocked)
		{
			status = Status.halting;
		}
		else if ((nightEn) && (nightBegin < now) && (now <= nightEnd))
		{
			if ((status == Status.halting) || (status == null))
			{
				startTime = nightBegin;
			}
			informed = false;
			status = Status.sleeping_night;
			XueBaYH.getApp().setOffLine(); 
		}
		else if ((noonEn) && (noonBegin < now) && (now <= noonEnd))
		{
			if ((status == Status.halting) || (status == null))
			{
				startTime = noonBegin;
			}
			status = Status.sleeping_noon;
			informed = false;
			XueBaYH.getApp().setOffLine(); 
		}
		else if ((studyEn) && (now <= studyEnd))
		{
			if ((status == Status.halting) || (status == null))
			{
				startTime = studyBegin;
			}
			status = Status.studying;
			informed = false;
		}
		else
		{
			status = Status.halting;
		}
		
		// 预告即将进入睡眠
		if ((!screenLocked) && (!informed))
		{
			Status status = Status.halting;
			boolean inform = false;
			if ((nightEn) && (nightBegin - 30000 <= now) && (now < nightBegin))
			{
				informed = true;
				inform = true;
				status = Status.sleeping_night;
			}
			else if ((noonEn) && (noonBegin - 30000 <= now) && (now < noonBegin))
			{
				informed = true;
				inform = true;
				status = Status.sleeping_noon;
			}
			
			if (inform)
			{
				XueBaYH.getApp().showToast("请注意:\n距离开始" + status.getLocalString() + "还有不到30秒! ");
				XueBaYH.getApp().vibrateOh();
			}
		}
	}
	
	private long	nightBegin, nightEnd, noonBegin, noonEnd, studyEnd, studyBegin;
	private boolean	nightEn, noonEn, studyEn;
	
	/**
	 * 读取保存的数据到内存. 
	 * 这里需要检查是否有过时的任务. 因为开机时已经消除了所有的过时任务. 这里的过时任务一定是开机期间强制退出躲过去的. 
	 * 而如果没有过时任务, 只是强制退出过, 不会惩罚. 因为可能是被清理内存了. 
	 */
	private void loadStatus()
	{
		XueBaYH.getApp().checkParity(null);

		SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
		EditorWithParity editor = new EditorWithParity(sharedPreferences);
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM月dd日HH时mm分");
		String string;
		long now = calendar.getTimeInMillis();

		noonEn = sharedPreferences.getBoolean(XueBaYH.NOON_EN, false);
		nightEn = sharedPreferences.getBoolean(XueBaYH.NIGHT_EN, false);
		studyEn = sharedPreferences.getBoolean(XueBaYH.STUDY_EN, false);

		studyBegin = sharedPreferences.getLong(XueBaYH.STUDY_BEGIN, 0);
		studyEnd = sharedPreferences.getLong(XueBaYH.STUDY_END, 0);
		noonBegin = sharedPreferences.getLong(XueBaYH.NOON_BEGIN, 0);
		noonEnd = sharedPreferences.getLong(XueBaYH.NOON_END, 0);
		nightBegin = sharedPreferences.getLong(XueBaYH.NIGHT_BEGIN, 0);
		nightEnd = sharedPreferences.getLong(XueBaYH.NIGHT_END, 0);
		
		if (nightEn&&sharedPreferences.getLong(XueBaYH.NIGHT_END, 0) <= now)
		{
			editor.putBoolean(XueBaYH.NIGHT_EN, false);
			nightEn = false;
			string = "有证据表明我曾经强制退出过. 而且我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(XueBaYH.NIGHT_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(XueBaYH.NIGHT_END, 0)) + "睡觉" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(XueBaYH.NIGHT_END, 0) + "\n";
			editor.putString(XueBaYH.PENGDING_LOGS, sharedPreferences.getString(XueBaYH.PENGDING_LOGS, "") + string);
			editor.commit();
		}
		if (noonEn&&sharedPreferences.getLong(XueBaYH.NOON_END, 0) <= now)
		{
			editor.putBoolean(XueBaYH.NOON_EN, false);
			noonEn = false;
			string = "有证据表明我曾经强制退出过. 而且我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(XueBaYH.NOON_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(XueBaYH.NOON_END, 0)) + "睡午觉" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(XueBaYH.NOON_END, 0) + "\n";
			editor.putString(XueBaYH.PENGDING_LOGS, sharedPreferences.getString(XueBaYH.PENGDING_LOGS, "") + string);
			editor.commit();
		}
		if (studyEn&&sharedPreferences.getLong(XueBaYH.STUDY_END, 0) <= now)
		{
			editor.putBoolean(XueBaYH.STUDY_EN, false);
			studyEn = false;
			string = "有证据表明我曾经强制退出过. 而且我所定的" + "从" + simpleDateFormat.format(sharedPreferences.getLong(XueBaYH.STUDY_BEGIN, 0)) + "到" + simpleDateFormat.format(sharedPreferences.getLong(XueBaYH.STUDY_END, 0)) + "学习" + "的计划也没有得到正常的执行, 再口头批评一次! \\timeStamp = " + sharedPreferences.getLong(XueBaYH.STUDY_END, 0) + "\n";
			editor.putString(XueBaYH.PENGDING_LOGS, sharedPreferences.getString(XueBaYH.PENGDING_LOGS, "") + string);
			editor.commit();
		}
		
		refreshCheckInterval();
	}
	
	private Status				status;
	private BroadcastReceiver	screenOnBroadcastReceiver;
	private BroadcastReceiver	screenOffBroadcastReceiver;
	private MonitorTask			monitorTask		= null;
	private WriteTimeTask		writeTimeTask	= null;
	
	// private final int NOTIFICATION_ID=1;
	// private final int UPDATE = 2;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);
		
		loadStatus();
		
		// startForeground(NOTIFICATION_ID, updateNotification());
		
		startThread(MonitorTask.class.getName());
		startThread(WriteTimeTask.class.getName());
		return START_STICKY;
	}
	
	/**
	 * 释放监听器; 释放监视线程; 保存各种记录. 通知监督人应用被关闭了.
	 * 
	 * 调试发现卸载app或者关机的时候不会调用onDestroy.
	 */
	@Override
	// 在设置里强行退出会引发这个函数, 但是使用任务管理软件强行退出就不会进入这个函数了.
	public void onDestroy()
	{
		unregisterReceiver(netStateReceiver);
		unregisterReceiver(screenOffBroadcastReceiver);
		unregisterReceiver(screenOnBroadcastReceiver);
		stopCurrentThread(monitorTask);
		stopCurrentThread(writeTimeTask);
		
		super.onDestroy();
		
		XueBaYH.getApp().restartMonitorService();
	}
	
	/**
	 * 随意弄一个校验
	 * 
	 * 注意: 在log文件里校验的方法是加入条目"last time", 然后对last time 和last time 指向的数据进行校验.
	 * 
	 * @return 校验码
	 */
	private long surfTimeParity()
	{
		SharedPreferences log = getSharedPreferences(SURF_TIME_LOG, MODE_PRIVATE);
		String lastSurfDateString = log.getString(LAST_SURF_DATE, "");
		String surfTimeIndexString = SURF_TIME_OF_S + lastSurfDateString;
		
		return (long) log.getFloat(surfTimeIndexString, 12) * (Math.abs(lastSurfDateString.hashCode()) % 2354667);
	}
	
	protected class StoppableRunnable implements Runnable
	{
		protected ConditionVariable	mConditionVariable	= new ConditionVariable(false);
		
		@Override
		public void run()
		{
		}
	}
	
	/**
	 * 监视线程. 检查活动activity是不是被允许的. 时间步长根据当前需要监视的状态确定. 每次锁屏后第一次运行主循环, 会停止. 开屏后,
	 * 又开启新的实例.
	 * 
	 * @author ssqstone
	 */
	private class MonitorTask extends StoppableRunnable
	{
		/**
		 * 主循环内检查是否锁屏在最先, 等待延时结束在最后, 最节省运行时间.
		 */
		@Override
		public void run()
		{
			while (true)
			{
				if (mConditionVariable.block(checkInterval))
				{
					return;
				}
				
				if (notPermitted())
				{
					Message message = new Message();
					message.what = TO_RESTRICT;
					handler.sendMessage(message);
				}
				
				refreshCheckInterval();
			}
		}
	}
	
	private class NetStateReceiver extends BroadcastReceiver
	{
		@Override
		/**
		 * 检查是否有上周日之前的未发送日志, 发送. 
		 */
		public void onReceive(Context arg0, Intent arg1)
		{
			ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo gprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (gprs.isConnected() || wifi.isConnected())
			{
				Calendar calendar = Calendar.getInstance();
				calendar.setFirstDayOfWeek(Calendar.SUNDAY);
				calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); // 日期变到上一个星期天0点
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				
				SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
				EditorWithParity editor = new EditorWithParity(sharedPreferences);
				
				String pendingString = sharedPreferences.getString(XueBaYH.PENGDING_LOGS, "");
				String[] strings = pendingString.split("\n");
				
				String bufferString = "";
				pendingString = "";
				for (int i = 0; i < strings.length; i++)
				{
					String string = strings[i]+'\n';
					if (string.length()<14)
					{
						continue;
					}
					long stamp = Long.valueOf(string.substring(string.length() - 14, string.length() - 1));
					long now = calendar.getTimeInMillis();
					if (stamp < now)
					{
						bufferString += string;
					}
					else
					{
						for (int j = i; j < strings.length; j++)
						{
							String string_ = strings[j]+'\n';
							pendingString += string_;
						}
						
						break;
					}
				}
				editor.putString(XueBaYH.PENGDING_LOGS, pendingString);
				editor.commit();
				
				if (bufferString.length() > 0)
				{
					XueBaYH.getApp().sendRennLog(bufferString);
				}
			}
		}
	}
	
	private class WriteTimeTask extends StoppableRunnable
	{
		/**
		 * 主循环内检查是否锁屏在最先, 等待延时结束在最后, 最节省运行时间.
		 */
		@Override
		public void run()
		{
			while (true)
			{
				if (mConditionVariable.block(60 * 1000)) // 每分钟记录一次
				{
					return;
				}
				
				Calendar calendar = Calendar.getInstance();
				SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
				EditorWithParity editor = new EditorWithParity(sharedPreferences);
				editor.putLong(XueBaYH.LAST_WRITE, calendar.getTimeInMillis());
				editor.commit();
			}
		}
	}
	
	/**
	 * 根据是否在监视时间段, 判断检查时间: 锁屏时几乎关闭(随便给一个很长的值); 不在监视时间段,1分钟; 在监视时间段, 0.1秒钟.
	 * 虽然0.1秒钟的检查频率比较高, 但锁屏的时候监视线程被释放, 不会浪费运行资源. 设检查频率为0.1秒是为了防止有时间调到主屏幕删除该应用.
	 */
	private void refreshCheckInterval()
	{
		refreshStatus();
		switch (status)
		{
			case halting:
				checkInterval = XueBaYH.debug ? 300 : 10000;
				break;
			
			default:
				checkInterval = 300;
				break;
		}
		// monitorTask.mConditionVariable.open();
	}
	
	private long	startTime;
	
	// private float surfTimeValue;
	// private long lastWrite;
	/**
	 * 
	 * @return 当前活动界面不应该出现 (sleep 模式下不是restrictedActivity, study模式下不是电话, 联系人,
	 *         短信或者restrictedActivity, 空闲模式下上网时间不能超过1小时)
	 */
	private boolean notPermitted()
	{
		ActivityManager activityManager = (ActivityManager) XueBaYH.getApp().getSystemService(ACTIVITY_SERVICE);
		List<RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
		RunningTaskInfo runningTaskInfo = runningTaskInfos.get(0);
		runningTaskInfo = (ActivityManager.RunningTaskInfo) (runningTaskInfo);
		ComponentName localComponentName = runningTaskInfo.topActivity;
		
		String packageName = localComponentName.getPackageName();
		
		if (status == Status.halting)
		{
			SharedPreferences values = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
			// TODO 
			
			if ("com.UCMobile com.uc.browser com.android.chrome com.android.browser com.dolphin.browser.xf com.tencent.mtt sogou.mobile.explorer com.baidu.browser.apps com.oupeng.mini.android ".contains(packageName))
			{
				// 应该给浏览器计时了. 先看是不是到了新的一天.
				SharedPreferences log = getSharedPreferences(SURF_TIME_LOG, MODE_PRIVATE);
				if (log.getLong(XueBaYH.PARITY, 0) != surfTimeParity())
				{
					Message message = new Message();
					message.what = SMS;
					message.obj = "我已经开启了节制上网功能, 每天一个小时. 如果您多次收到本条短信, 说明我修改甚至清空了数据, 这是不好的行为. ";
					handler.sendMessage(message);
				}
				
				Editor logEditor = log.edit();
				String surfTimeIndexString = SURF_TIME_OF_S + XueBaYH.getSimpleDate(Calendar.getInstance().getTimeInMillis());
				float surfTimeValue = log.getFloat(surfTimeIndexString, 0) + ((float) checkInterval / 1000);
				
				logEditor.putString(LAST_SURF_DATE, XueBaYH.getSimpleDate(Calendar.getInstance().getTimeInMillis()));
				logEditor.putFloat(surfTimeIndexString, surfTimeValue);
				logEditor.commit();
				
				if ((surfTimeValue >= 1800) && (surfTimeValue < 3600) && ((int) surfTimeValue % 180 == 0))
				{
					Message message = new Message();
					message.what = TOAST;
					message.obj = "请注意, 你已开启上网限制, 今天上网时间还有" + (int) ((3600 - surfTimeValue) / 60) + "分";
					handler.sendMessage(message);
				}
				else if (surfTimeValue >= 3600)
				{
					Message message = new Message();
					message.what = TOAST;
					message.obj = "你不觉得今天上网时间太长了么? \n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n这个不能发短信, 无解. ";
					handler.sendMessage(message);
					// return true; 在上网超时的时候只生成Toast, 不开启限制界面.
				}
			}
			else
			{
				handler.removeMessages(TOAST);
			}
			return false;
		}
		else
		{
			String permitted = "com.android.settings ustc.ssqstone.xueba GSW.AddinTimer com.zdworks.android.zdclock com.dianxinos.clock com.android.phone com.android.contacts com.android.mms com.jb.gosms-1 org.dayup.gnotes " + ((status == Status.studying) ? ("com.snda.youni cn.ssdl.bluedict com.ghisler.android.TotalCommander udk.android.reader jp.ne.kutu.Panecal com.diotek.diodict3.phone.samsung.chn com.docin.zlibrary.ui.android com.towords com.youdao.note com.duokan.reader com.baidu.wenku com.nd.android.pandareader com.qq.reader com.lectek.android.sfreader bubei.tingshu de.softxperience.android.noteeverything ") : ""); // ,
			
			return !permitted.contains(packageName);
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}
	
	/**
	 * 开始监视进程
	 * 
	 */
	private void startThread(String taskClassName)
	{
		StoppableRunnable task;
		if (taskClassName.equals(MonitorTask.class.getName()))
		{
			stopCurrentThread(monitorTask);
			task = monitorTask = new MonitorTask();
		}
		else if (taskClassName.equals(WriteTimeTask.class.getName()))
		{
			stopCurrentThread(writeTimeTask);
			task = writeTimeTask = new WriteTimeTask();
		}
		else
		{
			return;
		}
		
		new Thread(null, task, taskClassName).start();
		
	}
	
	/**
	 * 完全释放监视进程
	 * 
	 * @param task
	 */
	private void stopCurrentThread(StoppableRunnable task)
	{
		if (task != null)
		{
			task.mConditionVariable.open();
		}
	}
	
	// private Notification updateNotification()
	// {
	// Notification notification;
	// NotificationManager mNotificationManager = (NotificationManager)
	// getSystemService(NOTIFICATION_SERVICE);
	// RemoteViews contentView;
	// Intent intent;
	// PendingIntent contentIntent;
	// String string;
	//
	// string = "学霸银魂正在挽救新一代青年! ";
	// notification = new Notification(R.drawable.ic_launcher, string,
	// System.currentTimeMillis());
	// notification.flags = Notification.FLAG_NO_CLEAR;
	// contentView = new RemoteViews(this.getPackageName(),
	// R.layout.notification);
	//
	// if ((status == Status.studying) || (status == Status.sleeping_night)||
	// (status == Status.sleeping_noon))
	// {
	// contentView.setTextViewText(R.id.text, "按理说应该在" + status.getLocalString()
	// + "的. \n法网恢恢, 请君自重. ");
	//
	// /*intent = new Intent("android.intent.action.DIAL");
	// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
	// Intent.FLAG_ACTIVITY_SINGLE_TOP);
	// contentIntent = PendingIntent.getActivity(this, 0, intent,
	// PendingIntent.FLAG_UPDATE_CURRENT);
	// contentView.setOnClickPendingIntent(R.id.dial_b, contentIntent);
	// */
	// // TODO contentView.setOnClickFillInIntent(viewId, fillInIntent)
	//
	//
	// // intent = new Intent("android.intent.action.SENDTO");
	// // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
	// Intent.FLAG_ACTIVITY_SINGLE_TOP);
	// // contentIntent = PendingIntent.getActivity(this, 0, intent,
	// PendingIntent.FLAG_UPDATE_CURRENT);
	// // contentView.setOnClickPendingIntent(R.id.dial_b, contentIntent);
	// /*Bundle bundle=new Bundle();
	// bundle.putString("intent", "android.intent.action.DIAL");
	// contentView.setBundle(R.id.dial_b, "setOnClickListenerByBundle", bundle);
	//
	// bundle.putString("intent", "android.intent.action.SENDTO");
	// contentView.setBundle(R.id.sms_b, "setOnClickListenerByBundle",
	// bundle);*/
	//
	// }
	// else
	// {
	// contentView.setTextViewText(R.id.text, "法网恢恢, 请君自重. ");
	// }
	//
	// contentView.setImageViewBitmap(R.id.icon,
	// ((BitmapDrawable)getResources().getDrawable(R.drawable.ic_launcher)).getBitmap());
	// notification.contentView = contentView;
	// intent = new Intent(this, MainActivity.class);
	// intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
	// Intent.FLAG_ACTIVITY_SINGLE_TOP);
	// contentIntent = PendingIntent.getActivity(this, 0, intent,
	// PendingIntent.FLAG_UPDATE_CURRENT);
	// notification.contentIntent = contentIntent;
	// mNotificationManager.notify(NOTIFICATION_ID, notification);
	//
	// return notification;
	// }
}
