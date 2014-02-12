package ustc.ssqstone.xueba;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import ustc.ssqstone.xueba.MonitorService.Status;
import ustc.ssqstone.xueba.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

/**
 * 违规使用手机时调用, 显示提示信息和一个放弃按钮, 放弃时会有对话框提示, 给一个预先指定的号码发短信.
 * 确定按钮是加着玩的, 按后会锁屏. 
 * 
 * 放弃按钮按下后, 显示发送短信对话框, 提示要求发送短信到原定号码, 并显示短信默认部分: 
 * "我于学习开始xx小时xx分钟后放弃, 原定学习时间是xx小时."//"我到现在了还是要不睡觉鼓捣手机, 原定xx点xx分睡觉的. "
 * 后面还有一个输入框, 供用户输入附加信息. 短信会分条发送. 
 * 确定后, 发送短信, 并且修改任务结束时间, 重启任务. 任务会自动结束本activity, 然后就自由了. 
 * 
 * @author ssqstone
 */

public class RestrictedModeActivity extends Activity
{
	private Button smsButton;
	private Button dialButton;
	private Button denyButton;
	private Button toMainButton;
	
	public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
	protected static final int UNREGISTER = 0;
	protected static final int REGISTER = 1;
//	private static final int DENY=1;
//	protected static final int DENY_DIALOG = 2;
	
	private void acknowledgeInterrupted()
	{
		SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.HOW_MANY_INTERRUPTED_TIMES, MODE_PRIVATE);
		long start=getIntent().getLongExtra(XueBaYH.START_TIME, -1);
		
		String indexString = XueBaYH.getSimpleTime(start)+" - ";

		int interrupted = sharedPreferences.getInt(indexString, 0);
		Editor editor = sharedPreferences.edit();
		editor.putInt(indexString, interrupted+1);
		editor.commit();
		
		if(interrupted<40)
		{
			if (interrupted>=5)
			{
				XueBaYH.getApp().showToast("别再开屏关屏了或者乱动了, 触碰我的底线是会强制发短信的. \n在本次"+getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE)+"结束之前你还有"+(40-interrupted)+"次退出机会. ");
			}
		}
		else
		{
			if (interrupted%20==0)
			{
				String smsString = "我开始了"+getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE) +"模式, 但是手贱, 一直在鼓捣, 肯定没安好心, 请批评我. ";
				XueBaYH .getApp().sendSMS(smsString, getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE).getString(XueBaYH.PHONE_NUM,	 XueBaYH.myself?XueBaYH.我的监督人s:XueBaYH.我s), getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE));
			}
			else
			{
				XueBaYH.getApp().showToast("你还弄! 你再动一下试试?!");
			}
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		XueBaYH.getApp().killBackGround();
		XueBaYH.getApp().vibrateOh();
		
		initView();
		acknowledgeInterrupted();
	}
	
	private OnClickListener onDenyClickListener = new OnClickListener()
	{
		String phoneNum;
		String smsString;
		@Override
		public void onClick(View v)
		{
			AlertDialog.Builder builder;
			final EditText editText = new EditText(RestrictedModeActivity.this);
			SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES,MODE_PRIVATE);
			phoneNum=sharedPreferences.getString(XueBaYH.PHONE_NUM, XueBaYH.myself?XueBaYH.我的监督人s:XueBaYH.我s);
			
			if ((RestrictedModeActivity.this.getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE).equals(Status.sleeping_night.getLocalString()))||(RestrictedModeActivity.this.getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE).equals(Status.sleeping_noon.getLocalString()))) 
			{
				Calendar startCalendar=Calendar.getInstance();
				long start=RestrictedModeActivity.this.getIntent().getLongExtra(XueBaYH.START_TIME, 0);
				startCalendar.setTimeInMillis(start);
				SimpleDateFormat hmFormat = new SimpleDateFormat("hh点mm分"); 
				smsString="我到现在了还想不睡觉鼓捣手机, 原定"+hmFormat.format(startCalendar.getTime())+"睡觉的. ";
			}
			else
			{
				long now=Calendar.getInstance().getTimeInMillis();
	//			Calendar calendar=Calendar.getInstance();
				long start=RestrictedModeActivity.this.getIntent().getLongExtra(XueBaYH.START_TIME, 0);
	//			calendar.setTimeInMillis(now-start);
	//			SimpleDateFormat hmFormat = new SimpleDateFormat("hh小时mm分钟"); 
	//			smsString="我于学习开始"+hmFormat.format(calendar.getTime())+"后放弃.";
				smsString="我于学习开始"+(now-start)/1000/60/60+"时"+(now-start)/1000/60%60+"分"+(now-start)/1000%60+"秒后放弃.";
			}
			
			builder = 
			new AlertDialog.Builder(RestrictedModeActivity.this)
			.setTitle("发个短信给"+phoneNum+"你就自由啦~")
			.setMessage("[以下是本次短信的固定部分]\n"+smsString+"\n[以下是本次短信的可编辑部分]")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setView(editText).setPositiveButton("确定", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					smsString+=editText.getText().toString();
					XueBaYH.getApp().sendSMS(smsString, phoneNum, getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE));
					
					XueBaYH.getApp().showToast("短信发送成功后自动退出. ");
				}
			})
		    .setNegativeButton("取消", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.dismiss();
				}
			});
			builder.create().show();
		}
	};
	
	private OnClickListener onToMainListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			Intent intent=new Intent(RestrictedModeActivity.this,MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	};
	
	private void initView()
	{
//		requestWindowFeature(Window.FEATURE_NO_TITLE); 
//		this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED, FLAG_HOMEKEY_DISPATCHED);
		
//		// 设备安全管理服务
//        policyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
//        ComponentName componentName = new ComponentName(this, RestrictedModeActivity.class);
//        // 判断该组件是否有系统管理员的权限
//        boolean isAdminActive = policyManager.isAdminActive(componentName);
//        if(!isAdminActive)
//        {
//            Intent intent = new Intent();
//            intent.setAction(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
//            startActivity(intent);
//        }
		
		//设置全屏的时候怕有些手机不能通过通知栏打开飞行模式，这样晚上会很费电的。
		if (XueBaYH.myself)
		{
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); //设置全屏 
		}
		
		if (this.getIntent().hasExtra(XueBaYH.RESTRICTED_MODE))
		{
			if ((this.getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE).equals(Status.sleeping_night.getLocalString())) || (this.getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE).equals(Status.sleeping_noon.getLocalString())))
			{
				setContentView(R.layout.restricted_mode_sleeping);
				denyButton = (Button) super.findViewById(R.id.deny_sleeping_b);
				denyButton.setOnClickListener(onDenyClickListener);
				
				toMainButton = (Button) super.findViewById(R.id.sleep_to_main_b);
				toMainButton.setOnClickListener(onToMainListener);
			}
			else
			{
				setContentView(R.layout.restricted_mode_studying);
				denyButton = (Button) super.findViewById(R.id.deny_studying_b);
				denyButton.setOnClickListener(onDenyClickListener);
				
				toMainButton = (Button) super.findViewById(R.id.study_to_main_b);
				toMainButton.setOnClickListener(onToMainListener);
				
				dialButton = (Button) super.findViewById(R.id.dial_b);
				dialButton.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Uri uri = Uri.parse("tel:");
						Intent intent = new Intent(Intent.ACTION_DIAL, uri);
						startActivity(intent);
					}
				});
				
				smsButton = (Button) super.findViewById(R.id.sms_b);
				smsButton.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						Intent intent = new Intent(Intent.ACTION_MAIN);
						intent.setType("vnd.android-dir/mms-sms");
						startActivity(intent);
					}
				});
			}
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		
		if (intent.hasExtra(XueBaYH.DESTROY_RESTRICTION))
		{
			if(getIntent().hasExtra(XueBaYH.RESTRICTED_MODE))
			{
				SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES,MODE_PRIVATE);
				Editor editor=sharedPreferences.edit();
				if (getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE).equals(Status.sleeping_night.getLocalString()))
				{
					editor.putBoolean(XueBaYH.NIGHT_EN, false);
				}
				else if (getIntent().getStringExtra(XueBaYH.RESTRICTED_MODE).equals(Status.sleeping_noon.getLocalString())) 
				{
					editor.putBoolean(XueBaYH.NOON_EN, false);
				}
				else
				{
					editor.putBoolean(XueBaYH.STUDY_EN, false);
				}
				editor.commit();
				editor.putLong(XueBaYH.PARITY, XueBaYH.getApp().getParity());
				editor.commit();
			}
			finish();
		}
		else
		{
			setIntent(intent);
			acknowledgeInterrupted();
			initView();
		}
	}
	
	@Override
	protected void onDestroy()
	{
		XueBaYH.getApp().vibrateOK();
		super.onDestroy();
		Intent intent=new Intent(RestrictedModeActivity.this,MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
	}
	
	@Override
	/**
	 * 置back键为无效.
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{ 
		if((keyCode == KeyEvent.KEYCODE_BACK||keyCode == KeyEvent.KEYCODE_HOME)&& event.getAction() == KeyEvent.ACTION_DOWN)
		{
			XueBaYH.getApp().showToast("返回键这个大bug已经被屏蔽了. ");
			return true;
		}
		
		return super.onKeyDown(keyCode, event); 
	}

/*	@Override
    public void onAttachedToWindow()
	{
        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);  
        super.onAttachedToWindow();
    }*/
	/*
	@Override
	*//**
	 * 置back键和home键为无效.
	 *//*
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
		if((keyCode == KeyEvent.KEYCODE_BACK||keyCode == KeyEvent.KEYCODE_HOME)&& event.getAction() == KeyEvent.ACTION_DOWN)
		{
			Toast.makeText(this, "按什么键都是徒劳的. ", Toast.LENGTH_LONG).show();
			return true;
		}
		
		return super.onKeyDown(keyCode, event); 
	}
*/

//	private DevicePolicyManager policyManager;
}