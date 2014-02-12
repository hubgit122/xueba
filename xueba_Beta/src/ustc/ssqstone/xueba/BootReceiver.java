package ustc.ssqstone.xueba;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 捕获开机通知, 启动监控服务
 * 
 * @author ssqstone
 */
public class BootReceiver extends BroadcastReceiver
{
	public void onReceive(Context paramContext, Intent paramIntent)
	{
		XueBaYH.getApp().checkStatus();
		if (paramIntent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
		{
			XueBaYH.getApp().restartMonitorService();
		}
	}
}
