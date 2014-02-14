
package ustc.ssqstone.xueba;

import ustc.ssqstone.xueba.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.RemoteViews;

/**
 * 开机调用, 在设置程序中按确定按钮也会调用, 一直在后台执行所要进行的监视. 
 * 设置Activity在一个pref文件中储存当前的设置, 此Service只读取设置, 不会改变设置. 
 * 监视频率为3秒, 如果出现违规, 就唤醒响应的Activity.
 * 
 * @author ssqstone
 */
public class MonitorService extends Service
{
	private int checkInterval;
	protected boolean halt;
	private Handler handler;
	
	enum Status
	{
		sleeping("睡觉"), studying("学习"), waiting("等待"), leaving("离开");
		
		private String localeString;
		
		private Status(String localeString)
		{
			this.localeString = localeString;
		}
		
		public String getLocaleString()
		{
			return localeString;
		}
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		readStatus();
		
		screenOffBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				halt = true;
			}
		};
		screenOnBroadcastReceiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(Context context, Intent intent)
			{
				halt = false;
			}
		};
		
		handler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
				case NOTIFICATION_ID:
					updateNotification();
					
					break;
				default:
					break;
				}
				super.handleMessage(msg);
			}
		};
	}
	
	/**
	 * 应该吧信息独立出来, 打开Service时只读取一次. 否则频繁读取不变的文件真是脑抽了.
	 */
	private void readStatus()
	{
		// TODO Auto-generated method stub
		
	}
	
	private Status status = Status.waiting;
	private BroadcastReceiver screenOnBroadcastReceiver;
	private BroadcastReceiver screenOffBroadcastReceiver;
	private InformTimeOutTask monitorTask;
	private Thread monitorThread;
	private ConditionVariable mConditionVariable;
	
	private final static int NOTIFICATION_ID = R.layout.notification;
	
	@Override public int onStartCommand(Intent intent, int flags, int startId) 
	{
		super.onStartCommand(intent,flags,startId);
		if (haveSthToDo())
		{
			monitorTask = new InformTimeOutTask();
			monitorThread = new Thread(null, monitorTask, "Monitoring");
			mConditionVariable = new ConditionVariable(false);
			monitorThread.start();
			startForeground(NOTIFICATION_ID, updateNotification());
		}
		else
		{
			stopSelf();
		}
		
		IntentFilter intentFilter;
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);
		registerReceiver(screenOnBroadcastReceiver, intentFilter);
		
		intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenOffBroadcastReceiver, intentFilter);
		return START_STICKY;
	}
	
	private Notification updateNotification()
	{
		Notification notification;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		RemoteViews contentView;
		Intent intent;
		PendingIntent contentIntent;
		String string;
		
		if ((status == Status.studying) || (status == Status.sleeping))
		{
			string = "按理说应该在" + status.getLocaleString() + "的. ";
			notification = new Notification(R.drawable.icon, string, System.currentTimeMillis());
			notification.flags = Notification.FLAG_NO_CLEAR;
			contentView = new RemoteViews(this.getPackageName(), R.layout.notification);
			notification.contentView = contentView;
			intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			notification.contentIntent = contentIntent;
			mNotificationManager.notify(NOTIFICATION_ID, notification);
		}
		else
		{
			notification = null;
		}
		return notification;
	}
	
	private class InformTimeOutTask implements Runnable
	{
		@Override
		public void run()
		{
			if (notPermitted())
			{
				punish();
			}
			adjustCheckInterval();
			mConditionVariable.block(checkInterval * 1000 * (halt ? 1000 : 1));
		}
	}
	
	/**
	 * 被使能的睡觉, 午睡, 学习三个时间中最晚的结束时间在当前时间之后,则返回true. 如果可以用定时唤醒机制, 就不必调用这个函数了.
	 * 
	 * @return 被使能的睡觉, 午睡, 学习三个时间中最晚的结束时间是否在当前时间之后
	 */
	private boolean haveSthToDo()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * 根据是否在监视时间段, 判断检查时间: 不在监视时间段, 一分钟, 在监视时间段, 1秒钟. 反正锁屏的时候服务被关闭, 不会浪费运行资源.
	 */
	private void adjustCheckInterval()
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * 调用RestrictedModeActivity. 附带status信息, 来判断加载哪个图片.
	 */
	private void punish()
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * 
	 * @return 当前活动应用不合法 (sleep 模式下不是restrictedActivity, study模式下不是电话, 短信或者restrictedActivity)
	 */
	private boolean notPermitted()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	private boolean studying()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	private boolean sleeping()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}
}
