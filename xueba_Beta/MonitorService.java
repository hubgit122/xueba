
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
 * ��������, �����ó����а�ȷ����ťҲ�����, һֱ�ں�ִ̨����Ҫ���еļ���. 
 * ����Activity��һ��pref�ļ��д��浱ǰ������, ��Serviceֻ��ȡ����, ����ı�����. 
 * ����Ƶ��Ϊ3��, �������Υ��, �ͻ�����Ӧ��Activity.
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
		sleeping("˯��"), studying("ѧϰ"), waiting("�ȴ�"), leaving("�뿪");
		
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
	 * Ӧ�ð���Ϣ��������, ��Serviceʱֻ��ȡһ��. ����Ƶ����ȡ������ļ������Գ���.
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
			string = "����˵Ӧ����" + status.getLocaleString() + "��. ";
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
	 * ��ʹ�ܵ�˯��, ��˯, ѧϰ����ʱ��������Ľ���ʱ���ڵ�ǰʱ��֮��,�򷵻�true. ��������ö�ʱ���ѻ���, �Ͳ��ص������������.
	 * 
	 * @return ��ʹ�ܵ�˯��, ��˯, ѧϰ����ʱ��������Ľ���ʱ���Ƿ��ڵ�ǰʱ��֮��
	 */
	private boolean haveSthToDo()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * �����Ƿ��ڼ���ʱ���, �жϼ��ʱ��: ���ڼ���ʱ���, һ����, �ڼ���ʱ���, 1����. ����������ʱ����񱻹ر�, �����˷�������Դ.
	 */
	private void adjustCheckInterval()
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * ����RestrictedModeActivity. ����status��Ϣ, ���жϼ����ĸ�ͼƬ.
	 */
	private void punish()
	{
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * 
	 * @return ��ǰ�Ӧ�ò��Ϸ� (sleep ģʽ�²���restrictedActivity, studyģʽ�²��ǵ绰, ���Ż���restrictedActivity)
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
