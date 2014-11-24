package ustc.ssqstone.xueba;

import java.util.Calendar;

import ustc.ssqstone.xueba.R;

import com.renn.rennsdk.RennClient;
import com.renn.rennsdk.RennClient.LoginListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * 用于设置的Activity
 * 
 * @author ssqstone
 */
// 自用版本和发布版的区别：初始手机号不同；自用版无通知栏，保存数据时强制使用了默认手机号，强制晚睡和午睡。

public class MainActivity extends Activity
{
	private static final int	NOON					= 1;
	private static final int	NIGHT					= 2;
	private static final int	STUDY					= 3;
	
	private long				nightBegin, nightEnd, noonBegin, noonEnd, studyEnd, studyBegin;
	
	private int					nowEditting;
	
	private CheckBox			noonCB;
	private CheckBox			nightCB;
	private CheckBox			studyCB;
	private TextView			noonSleepTV;
	private TextView			nightSleepTV;
	private TextView			startStudyTV;
	private Button				okButton;
	private Button				logoutButton;
	private Button				sendStatusButton;
	private Button				screenButton;
	// private EditText phoneText;
	private TextView			phoneTV;
	private TextView			toWhomTV;
	
	protected boolean			initingData;
	
	public static final int		FLAG_HOMEKEY_DISPATCHED	= 0x80000000;
	
	private static boolean		backPressed;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		backPressed = false;
		initingData = true;
		
		setContentView(R.layout.main_activity);
		
		initView();
		
		if (savedInstanceState == null)
		{
			resumeData();
		}
		else
		{
			XueBaYH.showToast("上次未正常退出么? ");
			restoreInstance(savedInstanceState);
		}
		
		trimData();
		initingData = false;
		updateAllDisplay();
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		setIntent(intent);
		initView();
		super.onNewIntent(intent);
	}
	
	/**
	 * 将保存的数据恢复到界面
	 */
	protected void resumeData()
	{
		////Log.i("xueba", "resume");
		XueBaYH.checkParity(null);
		
		SharedPreferences values = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
		
		noonCB.setChecked(values.getBoolean(XueBaYH.NOON_EN, false));
		nightCB.setChecked(values.getBoolean(XueBaYH.NIGHT_EN, false));
		studyCB.setChecked(values.getBoolean(XueBaYH.STUDY_EN, false));
		
		studyEnd = values.getLong(XueBaYH.STUDY_END, 0);
		studyBegin = values.getLong(XueBaYH.STUDY_BEGIN, 0);
		noonBegin = values.getLong(XueBaYH.NOON_BEGIN, 0);
		noonEnd = values.getLong(XueBaYH.NOON_END, 0);
		nightBegin = values.getLong(XueBaYH.NIGHT_BEGIN, 0);
		nightEnd = values.getLong(XueBaYH.NIGHT_END, 0);
		String defaultPhoneString = values.getString(XueBaYH.PHONE_NUM, XueBaYH.myself ? XueBaYH.我的监督人s : XueBaYH.我s);
		phoneTV.setText(values.getString(XueBaYH.PHONE_NUM, defaultPhoneString));
		
		initingData = false;
	}
	
	/**
	 * 将内存中的数据所代表的时间修正; 修正算法都是一样的: ; 如果被修正时间段结束时间大于开始时间 (则可以位于同一天), 则修正为未来最近的一天;
	 * 否则跨越了两天. 如果当前时间点位于时间段内, 修正为最近的两天, 否则修正为今明两天.
	 * 
	 * 防止多天未开机的情况, 这样写性能稍低, 但是保险. 如果用今天0:00的时间作为标准判断, 每次加一天, 或者用二进制加法,
	 * 所有的时间都加一个固定的数字, 在目前看来是最有效的方法. 但是这样的话数据依赖关系又复杂了一些. 目前的方法运行一年不知道会不会拖慢一秒,
	 * 别太把运行时间当真.
	 */
	protected void trimData()
	{
		trim_(NIGHT);
		trim_(NOON);
		trim_(STUDY);
	}
	
	private void trim_(int tag)
	{
		long start, end;
		Calendar startCalendar = Calendar.getInstance(), endCalendar = Calendar.getInstance(), nowCalendar = Calendar.getInstance();
		
		if (tag == NOON)
		{
			start = noonBegin;
			end = noonEnd;
		}
		else if (tag == NIGHT)
		{
			start = nightBegin;
			end = nightEnd;
		}
		else
		{
			start = studyBegin;
			end = studyEnd;
		}
		
		startCalendar.setTimeInMillis(start);
		endCalendar.setTimeInMillis(end);
		
		startCalendar.set(Calendar.YEAR, nowCalendar.get(Calendar.YEAR));
		endCalendar.set(Calendar.YEAR, nowCalendar.get(Calendar.YEAR));
		startCalendar.set(Calendar.MONTH, nowCalendar.get(Calendar.MONTH));
		endCalendar.set(Calendar.MONTH, nowCalendar.get(Calendar.MONTH));
		startCalendar.set(Calendar.DAY_OF_MONTH, nowCalendar.get(Calendar.DAY_OF_MONTH));
		endCalendar.set(Calendar.DAY_OF_MONTH, nowCalendar.get(Calendar.DAY_OF_MONTH));
		startCalendar.set(Calendar.SECOND, 0);
		endCalendar.set(Calendar.SECOND, 0);
		startCalendar.set(Calendar.MILLISECOND, 0);
		endCalendar.set(Calendar.MILLISECOND, 0);
		if (startCalendar.after(endCalendar))
		{
			if (nowCalendar.before(endCalendar))
			{
				startCalendar.add(Calendar.DATE, -1);
			}
			else
			{
				endCalendar.add(Calendar.DATE, 1);
			}
		}
		else if (nowCalendar.after(endCalendar))
		{
			startCalendar.add(Calendar.DATE, 1);
			endCalendar.add(Calendar.DATE, 1);
		}
		start = startCalendar.getTimeInMillis();
		end = endCalendar.getTimeInMillis();
		
		if (tag == NOON)
		{
			noonBegin = start;
			noonEnd = end;
		}
		else if (tag == NIGHT)
		{
			nightBegin = start;
			nightEnd = end;
		}
		else
		{
			studyBegin = start;
			studyEnd = end;
		}
	}
	
	private void updateAllDisplay()
	{
		nowEditting = NOON;
		updateDisplay();
		nowEditting = NIGHT;
		updateDisplay();
		nowEditting = STUDY;
		updateDisplay();
	}
	
	/**
	 * 注意, 仅使用在成功地通过对话框输入时间后. 需要使用私有时间域.
	 */
	private void updateDisplay()
	{
		Calendar startCal = Calendar.getInstance(), endCal = Calendar.getInstance();
		String string;
		switch (nowEditting)
		{
			case NOON:
				startCal.setTimeInMillis(noonBegin);
				endCal.setTimeInMillis(noonEnd);
				
				string = new StringBuilder().append(pad(startCal.get(Calendar.HOUR_OF_DAY))).append(":").append(pad(startCal.get(Calendar.MINUTE))).append(" - ").append(pad(endCal.get(Calendar.HOUR_OF_DAY))).append(":").append(pad(endCal.get(Calendar.MINUTE))).toString();
				
				noonSleepTV.setText(string);
				break;
			
			case NIGHT:
				startCal.setTimeInMillis(nightBegin);
				endCal.setTimeInMillis(nightEnd);
				
				nightSleepTV.setText(new StringBuilder().append(pad(startCal.get(Calendar.HOUR_OF_DAY))).append(":").append(pad(startCal.get(Calendar.MINUTE))).append(" - ").append(pad(endCal.get(Calendar.HOUR_OF_DAY))).append(":").append(pad(endCal.get(Calendar.MINUTE))));
				break;
			
			case STUDY:
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(studyEnd);
				StringBuilder stringBuilder = new StringBuilder();
				
				stringBuilder.append("Now - ").append(pad(cal.get(Calendar.HOUR_OF_DAY))).append(":").append(pad(cal.get(Calendar.MINUTE)));
				string = stringBuilder.toString();
				startStudyTV.setText(string);
			default:
				break;
		}
	}
	
	private String pad(int c)
	{
		if (c >= 10)
		{
			return String.valueOf(c);
		}
		else
		{
			return "0" + String.valueOf(c);
		}
	}
	
	/**
	 * 初始化界面, 得到所有元素的句柄, 配置监听器.
	 */
	private void initView()
	{
		////Log.i("xueba", "initview");
		
		noonCB = (CheckBox) findViewById(R.id.noon_cb);
		nightCB = (CheckBox) findViewById(R.id.night_cb);
		studyCB = (CheckBox) findViewById(R.id.study_cb);
		
		noonCB.setOnCheckedChangeListener(onCheckedCheageListener);
		nightCB.setOnCheckedChangeListener(onCheckedCheageListener);
		studyCB.setOnCheckedChangeListener(onCheckedCheageListener);
		
		noonSleepTV = (TextView) findViewById(R.id.noon_tv);
		nightSleepTV = (TextView) findViewById(R.id.night_tv);
		startStudyTV = (TextView) findViewById(R.id.study_tv);
		
		okButton = (Button) findViewById(R.id.setting_ok_b);
		okButton.setOnClickListener(onSettingOKClickListener);
		
		logoutButton = (Button) findViewById(R.id.log_out_b);
		logoutButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				backPressed = false;
				RennClient rennClient = XueBaYH.getRennClient();
				rennClient.logout();
				XueBaYH.showToast("已注销");
				logoutButton.setVisibility(View.GONE);
				sendStatusButton.setVisibility(View.GONE);
			}
		});
		
		logoutButton.setVisibility(XueBaYH.getRennClient().isLogin() ? View.VISIBLE : View.GONE);
		
		sendStatusButton = (Button) findViewById(R.id.send_status_b);
		sendStatusButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				backPressed = false;
				RennClient rennClient = XueBaYH.getRennClient();
				if (!rennClient.isLogin())
				{
					return;
				}
				
				AlertDialog.Builder builder;
				final EditText editText = new EditText(MainActivity.this);
				
				builder = new AlertDialog.Builder(MainActivity.this).setTitle("发状态").setMessage("您已经登录, 请写入发送状态的内容. ").setIcon(android.R.drawable.ic_dialog_info).setView(editText).setPositiveButton("确定", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						XueBaYH.sendStatus(editText.getText().toString());
						dialog.dismiss();
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						dialog.dismiss();
					}
				});
				
				builder.create().show();
			}
		});
		sendStatusButton.setVisibility(XueBaYH.getRennClient().isLogin() ? View.VISIBLE : View.GONE);
		
		screenButton = (Button) findViewById(R.id.screen_settings_b);
		screenButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Intent intent = new Intent(MainActivity.this, ScreenSettingActivity.class);
				startActivity(intent);
			}
		});
		
		phoneTV = (TextView) findViewById(R.id.phone_tv);
		toWhomTV = (TextView) findViewById(R.id.to_whom_tv);
		
		OnClickListener phoneClickListener = new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				backPressed = false;
				AlertDialog.Builder builder;
				final EditText editText = new EditText(MainActivity.this);
				editText.setText(phoneTV.getText());
				builder = new AlertDialog.Builder(MainActivity.this).setTitle("找谁监督我呢? ").setMessage("请不要轻易改变此值, 因为会给被设置和被取消设置的双方发短信. 请注意, 被替换的监督人会收到即将成为监督人的手机号. \n温馨提示: 如果没有特殊情况, 请把监督人设为你的父母, 只有他们才不会怪罪你的打扰. ").setIcon(android.R.drawable.ic_dialog_info).setView(editText).setPositiveButton("确定", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						phoneTV.setText(editText.getText().toString());
						XueBaYH.showToast("请求已接收. 若确定更改监督人, 请稍后点击保存设置按钮,而那时会发送短信. ");
						dialog.dismiss();
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener()
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
		
		toWhomTV.setOnClickListener(phoneClickListener);
		phoneTV.setOnClickListener(phoneClickListener);
	}
	
	private OnCheckedChangeListener	onCheckedCheageListener		= new OnCheckedChangeListener()
																{
																	@Override
																	public void onCheckedChanged(final CompoundButton cb, boolean isChecked)
																	{
																		backPressed = false;
																		if (isChecked && (!initingData))
																		{
																			switch (cb.getId())
																			{
																				case R.id.noon_cb:
																					nowEditting = NOON;
																					
																					showTimePickers(cb);
																					
																					break;
																				
																				case R.id.night_cb:
																					nowEditting = NIGHT;
																					
																					showTimePickers(cb);
																					
																					break;
																				
																				case R.id.study_cb:
																					nowEditting = STUDY;
																					
																					showTimeInputer(cb);
																					break;
																				
																				default:
																					break;
																			}
																		}
																	}
																	
																	private void showTimeInputer(final CompoundButton cb)
																	{
																		AlertDialog.Builder builder;
																		final EditText editText = new EditText(MainActivity.this);
																		builder = new AlertDialog.Builder(MainActivity.this).setTitle("学多长时间好呢?").setMessage("格式: \n1.2:\n从现在起学习1.2个小时\n\n17:30:\n从现在起学习直到下一个17:30").setIcon(android.R.drawable.ic_dialog_info).setView(editText).setPositiveButton("确定", new DialogInterface.OnClickListener()
																		{
																			@Override
																			public void onClick(DialogInterface dialog, int which)
																			{
																				String string = editText.getText().toString();
																				Calendar cal = null;
																				
																				int pos0 = string.contains(":") ? string.indexOf(':') : string.indexOf('：');
																				
																				if (pos0 > -1)
																				{
																					cal = Calendar.getInstance();
																					cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(string.substring(0, pos0)));
																					cal.set(Calendar.MINUTE, Integer.parseInt(string.substring(pos0 + 1)));
																					
																					if (cal.before(Calendar.getInstance()))
																					{
																						cal.add(Calendar.DATE, 1);
																					}
																					
																					SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
																					long now = Calendar.getInstance().getTimeInMillis();
																					if (!sharedPreferences.getBoolean(XueBaYH.STUDY_EN, false))
																					{
																						studyBegin = now;
																					}
																					studyEnd = cal.getTimeInMillis();
																					updateDisplay();
																				}
																				else
																				{
																					int min;
																					try
																					{
																						min = (int) (Float.parseFloat(string) * 60);
																						if (min < 0)
																							throw new NumberFormatException();
																						
																						cal = Calendar.getInstance();
																						long now = cal.getTimeInMillis();
																						SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
																						
																						if (!(sharedPreferences.getBoolean(XueBaYH.STUDY_EN, false)))
																						{
																							studyBegin = now;
																						}
																						cal.add(Calendar.MINUTE, min);
																						studyEnd = cal.getTimeInMillis();
																						updateDisplay();
																					}
																					catch (NumberFormatException e)
																					{
																						cb.setChecked(false);
																						XueBaYH.showToast("请检查输入格式");
																						return;
																					}
																				}
																				XueBaYH.showToast("学习时间已收到, 请在一分钟内点击界面下方的保存按钮. ");
																				dialog.dismiss();
																			}
																		}).setNegativeButton("取消", new DialogInterface.OnClickListener()
																		{
																			@Override
																			public void onClick(DialogInterface dialog, int which)
																			{
																				// message.what=UNCHECK;
																				// handler.sendMessage(message);
																				cb.setChecked(false);
																				dialog.dismiss();
																			}
																		});
																		
																		builder.create().show();
																	}
																	
																	private void showTimePickers(final CompoundButton cb)
																	{
																		AlertDialog.Builder builder;
																		LayoutInflater inflater;
																		View layout;
																		inflater = getLayoutInflater();
																		layout = inflater.inflate(R.layout.select_time, (ViewGroup) findViewById(R.id.xx_time_selcet));
																		final TimePicker timePicker0 = (TimePicker) layout.findViewById(R.id.timePicker0);
																		final TimePicker timePicker1 = (TimePicker) layout.findViewById(R.id.timePicker1);
																		
																		if (nowEditting == NOON)
																		{
																			Calendar calendar = Calendar.getInstance();
																			if (noonBegin == 0)
																			{
																				calendar.set(Calendar.HOUR, 12);
																				calendar.set(Calendar.MINUTE, 30);
																			}
																			else
																			{
																				calendar.setTimeInMillis(noonBegin);
																			}
																			timePicker0.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
																			timePicker0.setCurrentMinute(calendar.get(Calendar.MINUTE));
																		}
																		else
																		{
																			Calendar calendar = Calendar.getInstance();
																			calendar.setTimeInMillis(nightBegin);
																			if (nightBegin == 0)
																			{
																				calendar.set(Calendar.HOUR, 23);
																				calendar.set(Calendar.MINUTE, 30);
																			}
																			else
																			{
																				calendar.setTimeInMillis(nightBegin);
																			}
																			timePicker0.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
																			timePicker0.setCurrentMinute(calendar.get(Calendar.MINUTE));
																		}
																		
																		if (nowEditting == NOON)
																		{
																			Calendar calendar = Calendar.getInstance();
																			calendar.setTimeInMillis(noonEnd);
																			if (noonEnd == 0)
																			{
																				calendar.set(Calendar.HOUR, 13);
																				calendar.set(Calendar.MINUTE, 30);
																			}
																			else
																			{
																				calendar.setTimeInMillis(noonEnd);
																			}
																			timePicker1.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
																			timePicker1.setCurrentMinute(calendar.get(Calendar.MINUTE));
																		}
																		else
																		{
																			Calendar calendar = Calendar.getInstance();
																			calendar.setTimeInMillis(nightEnd);
																			if (nightEnd == 0)
																			{
																				calendar.set(Calendar.HOUR, 7);
																				calendar.set(Calendar.MINUTE, 30);
																			}
																			else
																			{
																				calendar.setTimeInMillis(nightEnd);
																			}
																			timePicker1.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
																			timePicker1.setCurrentMinute(calendar.get(Calendar.MINUTE));
																		}
																		
																		builder = new AlertDialog.Builder(MainActivity.this).setTitle("请输入" + ((nowEditting == NOON) ? "午睡" : "睡觉") + "时间").setIcon(android.R.drawable.ic_dialog_info).setView(layout).setPositiveButton("确定", new DialogInterface.OnClickListener()
																		{
																			@Override
																			public void onClick(DialogInterface dialog, int which)
																			{
																				long start_, end_;
																				Calendar calendar = Calendar.getInstance();
																				calendar.set(Calendar.MINUTE, timePicker0.getCurrentMinute());
																				calendar.set(Calendar.HOUR_OF_DAY, timePicker0.getCurrentHour());
																				start_ = calendar.getTimeInMillis();
																				
																				calendar.set(Calendar.MINUTE, timePicker1.getCurrentMinute());
																				calendar.set(Calendar.HOUR_OF_DAY, timePicker1.getCurrentHour());
																				
																				if (Calendar.getInstance().getTimeInMillis() > start_)
																				{
																					cb.setChecked(false);
																					XueBaYH.showToast("不能将开始时间设为当前时间之前. ");
																					return;
																				}
																				
																				if ((nowEditting == NOON))
																				{
																					end_ = calendar.getTimeInMillis();
																					if (end_ <= start_)
																					{
																						cb.setChecked(false);
																						XueBaYH.showToast("输入有误。 ");
																					}
																					else
																					{
																						XueBaYH.showToast("设置成功！ ");
																						noonEnd = end_;
																						noonBegin = start_;
																						updateDisplay();
																					}
																				}
																				else
																				{
																					nightBegin = start_;
																					nightEnd = calendar.getTimeInMillis();
																					// trim_(NIGHT);
																					XueBaYH.showToast("设置成功。 ");
																					updateDisplay();
																				}
																			}
																		}).setNegativeButton("取消", new DialogInterface.OnClickListener()
																		{
																			@Override
																			public void onClick(DialogInterface dialog, int which)
																			{
																				cb.setChecked(false);
																				dialog.dismiss();
																			}
																		});
																		
																		builder.create().show();
																	}
																};
	
	private OnClickListener			onSettingOKClickListener	= new OnClickListener()
																{
																	@Override
																	public void onClick(View v)
																	{
																		// setConfirmPhone(false);
																		backPressed = false;
																		switch (v.getId())
																		{
																			case R.id.setting_ok_b:
																				if (saveData())
																				{
																					RennClient rennClient = XueBaYH.getRennClient();
																					
																					if (!rennClient.isLogin())
																					{
																						Toast.makeText(MainActivity.this, "登录人人以利用日志功能发送使用报告. ", Toast.LENGTH_SHORT).show();
																						
																						rennClient.setLoginListener(new LoginListener()
																						{
																							@Override
																							public void onLoginSuccess()
																							{
																								Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
																							}
																							
																							@Override
																							public void onLoginCanceled()
																							{
																								Toast.makeText(MainActivity.this, "登录取消, 我期待你的加入. ", Toast.LENGTH_SHORT).show();
																							}
																						});
																						rennClient.login(MainActivity.this);
																					}
																					
																					XueBaYH.restartMonitorService();
																					finish();
																				}
																				else
																				{
																					XueBaYH.showToast("设置有误, 不要缩短任务, 不要使时间重叠. ");
																				}
																				
																				break;
																			
																			default:
																				break;
																		}
																	}
																};
	
	/**
	 * 将数据保存到文件。 注意: 会让数据规约到今明两天，会检查数据完好性。检查不通过不会保存数据。
	 * 
	 * @return 数据正常返回true
	 */
	protected boolean saveData()
	{
		//Log.i("trace", "save");
		boolean resault = checkData();
		
		if (resault)
		{
			String oldPhoneString = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE).getString(XueBaYH.PHONE_NUM, XueBaYH.myself ? XueBaYH.我的监督人s : XueBaYH.我s);
			String newPhoneString = phoneTV.getText().toString();
			if (!oldPhoneString.equals(newPhoneString))
			{
				XueBaYH.sendSMS(XueBaYH.INFORM_OFF + "现在的监督人号码为" + newPhoneString, oldPhoneString, null);
				XueBaYH.sendSMS(XueBaYH.INFORM_ON, newPhoneString, null);
				XueBaYH.showToast("监督人修改成功");
			}
			
			SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
			EditorWithParity editor = new EditorWithParity(sharedPreferences);
			
			editor.putBoolean(XueBaYH.NIGHT_EN, nightCB.isChecked());
			editor.putBoolean(XueBaYH.NOON_EN, noonCB.isChecked());
			editor.putBoolean(XueBaYH.STUDY_EN, studyCB.isChecked());
			editor.putLong(XueBaYH.NIGHT_BEGIN, nightBegin);
			editor.putLong(XueBaYH.NIGHT_END, nightEnd);
			editor.putLong(XueBaYH.NOON_BEGIN, noonBegin);
			editor.putLong(XueBaYH.NOON_END, noonEnd);
			editor.putLong(XueBaYH.STUDY_END, studyEnd);
			editor.putLong(XueBaYH.STUDY_BEGIN, studyBegin);
			editor.putString(XueBaYH.PHONE_NUM, phoneTV.getText().toString());
			editor.commit();
		}
		return resault;
	}
	
	/**
	 * 检查数据完好性, 同时防止缩小任务. 数据完好性的意思是: 结束时间必须在开始时间之后, 没有学习和睡觉时间重叠的情况,
	 * 电话号码域必须像一个电话号码. (比较基本的关于时间的数据完好性检查已经在对话框返回之前做好) 防止缩小任务的意思是: 在上次成功的提交之后,
	 * 不能提前还未完成的任务的结束时间, 更不能放弃任务.
	 * 
	 * 注意 使用条件: 必须在文件中数据更新前使用. 这样才有新旧两个时间的对照.
	 * 
	 * @return 数据可以接受, 返回true
	 */
	
	private boolean checkData()
	{
		boolean result = true;
		SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
		Calendar calendar = Calendar.getInstance();
		
		if (XueBaYH.myself)
		{
			initingData = true;
			
			phoneTV.setText(XueBaYH.我的监督人s);
			noonCB.setChecked(true);
			nightCB.setChecked(true);
			
			initingData = false;
			
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.MINUTE, 30);
			nightBegin = calendar.getTimeInMillis();
			calendar.set(Calendar.HOUR_OF_DAY, 7);
			calendar.set(Calendar.MINUTE, 30);
			nightEnd = calendar.getTimeInMillis();
			
			calendar.set(Calendar.HOUR_OF_DAY, 12);
			calendar.set(Calendar.MINUTE, 30);
			noonBegin = calendar.getTimeInMillis();
			calendar.set(Calendar.HOUR_OF_DAY, 13);
			calendar.set(Calendar.MINUTE, 30);
			noonEnd = calendar.getTimeInMillis();
			updateAllDisplay();
		}
		
		trimData();
		
		long now = Calendar.getInstance().getTimeInMillis();
		
		// 满足手机号格式
		result = phoneTV.getText().toString().matches(XueBaYH.debugSMS ? "10010" : "1[0-9]{10}");
		// 时间不交错
		result = result && ((noonCB.isChecked() && studyCB.isChecked()) ? ((studyEnd <= noonBegin) || (now >= noonEnd)) : true);
		result = result && ((nightCB.isChecked() && studyCB.isChecked()) ? ((studyEnd <= nightBegin) || (now >= nightEnd)) : true);
		result = result && (((noonCB.isChecked() && nightCB.isChecked()) ? ((noonEnd <= nightBegin) || (noonBegin >= nightEnd)) : true));
		
		// 不能反悔
		result = result && (sharedPreferences.getBoolean(XueBaYH.NOON_EN, false) ? noonCB.isChecked() : true);
		result = result && (sharedPreferences.getBoolean(XueBaYH.NIGHT_EN, false) ? nightCB.isChecked() : true);
		result = result && (sharedPreferences.getBoolean(XueBaYH.STUDY_EN, false) ? studyCB.isChecked() : true);
		
		// 不能缩小任务
		result = result && ((noonCB.isChecked() && sharedPreferences.getBoolean(XueBaYH.NOON_EN, false)) ? ((noonEnd >= sharedPreferences.getLong(XueBaYH.NOON_END, 0)) && (noonBegin <= sharedPreferences.getLong(XueBaYH.NOON_BEGIN, 0))) : true);
		result = result && ((nightCB.isChecked() && sharedPreferences.getBoolean(XueBaYH.NIGHT_EN, false)) ? ((nightEnd >= sharedPreferences.getLong(XueBaYH.NIGHT_END, 0)) && (nightBegin <= sharedPreferences.getLong(XueBaYH.NIGHT_BEGIN, 0))) : true);
		result = result && ((studyCB.isChecked() && sharedPreferences.getBoolean(XueBaYH.STUDY_EN, false)) ? (studyEnd >= sharedPreferences.getLong(XueBaYH.STUDY_END, 0)) : true);
		
		// 手机号更改要通知
		String oldPhoneString = sharedPreferences.getString(XueBaYH.PHONE_NUM, XueBaYH.myself ? XueBaYH.我的监督人s : XueBaYH.我s);
		String newPhoneString = phoneTV.getText().toString();
		
		if (result && (!oldPhoneString.equals(newPhoneString)))
		{
			if (newPhoneString.equals(XueBaYH.getPhoneNum()))
			{
				XueBaYH.showToast("怎么能让自己监督自己呢? 一点常识都没的嘛! ");
				result = false;
			}
			else if (!halting())
			{
				XueBaYH.showToast("任务进行中不能反悔的. ");
				result = false;
			}
			else
			{
				result = true;
			}
		}
		return result;
	}
	
	private boolean halting()
	{
		long now = Calendar.getInstance().getTimeInMillis();
		boolean resualt = false;
		SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.VALUES, MODE_PRIVATE);
		
		resualt = (sharedPreferences.getBoolean(XueBaYH.STUDY_EN, false) && (now > studyBegin) && (now < studyEnd)) || (sharedPreferences.getBoolean(XueBaYH.NOON_EN, false) && (now > noonBegin) && (now < noonEnd)) || (sharedPreferences.getBoolean(XueBaYH.NIGHT_EN, false) && (now > nightBegin) && (now < nightEnd));
		return !resualt;
	}
	
	/**
	 * 按确定键会明确检查是否成功保存数据并提示修改. 按返回键退出时不应该尝试保存.
	 */
	@Override
	protected void onDestroy()
	{
		//Log.i("trace", "onDestroy");
		if (!saveData())
		{
			XueBaYH.showToast(XueBaYH.INFORM_NOT_SAVING);
		}
		else
		{
			XueBaYH.showToast(XueBaYH.INFORM_SAVED);
		}
		
		super.onDestroy();
	}
	
	/**
	 * 不管怎样都不能读取存档。
	 * 
	 * @param savedInstanceState
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);
		restoreInstance(savedInstanceState);
	}
	
	/**
	 * 最小化的时候都会调用这个，但是还原的时候又把bundle丢了。。。
	 * 
	 * @param savedInstanceState
	 */
	private void restoreInstance(Bundle savedInstanceState)
	{
		phoneTV.setText(savedInstanceState.getString(XueBaYH.PHONE_NUM));
		nightCB.setChecked(savedInstanceState.getBoolean(XueBaYH.NIGHT_EN));
		noonCB.setChecked(savedInstanceState.getBoolean(XueBaYH.NOON_EN));
		noonCB.setChecked(savedInstanceState.getBoolean(XueBaYH.STUDY_EN));
		
		nightBegin = savedInstanceState.getLong(XueBaYH.NIGHT_BEGIN);
		nightEnd = savedInstanceState.getLong(XueBaYH.NIGHT_END);
		noonBegin = savedInstanceState.getLong(XueBaYH.NOON_BEGIN);
		noonEnd = savedInstanceState.getLong(XueBaYH.NOON_BEGIN);
		studyBegin = savedInstanceState.getLong(XueBaYH.STUDY_BEGIN);
		studyEnd = savedInstanceState.getLong(XueBaYH.STUDY_END);
		backPressed = savedInstanceState.getBoolean(XueBaYH.BACK_PRESSED);
		// setConfirmPhone(savedInstanceState.getBoolean(XueBaYH.CONFIRM_PHONE));
		nowEditting = savedInstanceState.getInt(XueBaYH.NOW_EDITTING);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		outState.putString(XueBaYH.PHONE_NUM, phoneTV.getText().toString());
		outState.putBoolean(XueBaYH.NIGHT_EN, nightCB.isChecked());
		outState.putBoolean(XueBaYH.NOON_EN, noonCB.isChecked());
		outState.putBoolean(XueBaYH.STUDY_EN, noonCB.isChecked());
		
		outState.putLong(XueBaYH.NIGHT_BEGIN, nightBegin);
		outState.putLong(XueBaYH.NIGHT_END, nightEnd);
		outState.putLong(XueBaYH.NOON_BEGIN, noonBegin);
		outState.putLong(XueBaYH.NOON_END, noonEnd);
		outState.putLong(XueBaYH.STUDY_BEGIN, studyBegin);
		outState.putLong(XueBaYH.STUDY_END, studyEnd);
		
		outState.putBoolean(XueBaYH.BACK_PRESSED, backPressed);
		// outState.putBoolean(XueBaYH.CONFIRM_PHONE, isConfirmPhone());
		outState.putInt(XueBaYH.NOW_EDITTING, nowEditting);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_BACK:
				if (checkData())
				{
					XueBaYH.restartMonitorService();
					finish();
				}
				else
				{
					if (backPressed)
					{
						XueBaYH.showToast(XueBaYH.INFORM_NOT_SAVED);
						XueBaYH.restartMonitorService();
						finish();
					}
					backPressed = true;
					XueBaYH.showToast(XueBaYH.INFORM_SAVING_ERROR);
				}
				return true;
				
			default:
				backPressed = false;
				return false;
		}
	}
}
