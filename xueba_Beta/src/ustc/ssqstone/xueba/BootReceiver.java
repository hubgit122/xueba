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
	@Override
	public void onReceive(Context paramContext, Intent paramIntent)
	{
		if (paramIntent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
			if (XueBaYH.debug)
			{
				XueBaYH.getApp().showToast("开机");
				//Log.i("xueba", "开机");
			}
			
			XueBaYH.getApp().trimUsageTime(-1);
			XueBaYH.getApp().checkStatus();
			XueBaYH.getApp().restartMonitorService();
		}
	}
}
