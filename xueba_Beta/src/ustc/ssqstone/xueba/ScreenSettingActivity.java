package ustc.ssqstone.xueba;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ScreenSettingActivity extends Activity
{
	private CheckBox	filterEnBox, rgbEnBox, tooletEnBox, relativelyBox;
	private SeekBar		rBar, gBar, bBar, aBar;
	private Button		rButton, gButton, bButton, aButton, resetButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_settings);
		initView();
	}
	
	private void initView()
	{
		SharedPreferences sharedPreferences = getSharedPreferences(XueBaYH.SCREEN, MODE_PRIVATE);
		
		filterEnBox = (CheckBox) findViewById(R.id.filter_en_cb);
		rgbEnBox = (CheckBox) findViewById(R.id.rgb_en_cb);
		tooletEnBox = (CheckBox) findViewById(R.id.toolet_en_cb);
		relativelyBox = (CheckBox) findViewById(R.id.relatively_en);
		resetButton = (Button) findViewById(R.id.reset_b);
		rBar = (SeekBar) findViewById(R.id.r_sb);
		gBar = (SeekBar) findViewById(R.id.g_sb);
		bBar = (SeekBar) findViewById(R.id.b_sb);
		aBar = (SeekBar) findViewById(R.id.a_sb);
		rButton = (Button) findViewById(R.id.red_b);
		gButton = (Button) findViewById(R.id.green_b);
		bButton = (Button) findViewById(R.id.blue_b);
		aButton = (Button) findViewById(R.id.alpha_b);
		
		filterEnBox.setChecked(sharedPreferences.getBoolean(XueBaYH.FILTER_EN, false));
		rgbEnBox.setChecked(sharedPreferences.getBoolean(XueBaYH.RGB_EN, false));
		tooletEnBox.setChecked(sharedPreferences.getBoolean(XueBaYH.TOOLET_EN, false));
		//		relativelyBox.setChecked(sharedPreferences.getBoolean(XueBaYH.RELATIVELY_EN, false));
		
		rBar.setProgress(sharedPreferences.getInt(XueBaYH.RED, 0));
		gBar.setProgress(sharedPreferences.getInt(XueBaYH.GREEN, 0));
		bBar.setProgress(sharedPreferences.getInt(XueBaYH.BLUE, 0));
		aBar.setProgress(sharedPreferences.getInt(XueBaYH.ALPHA, 0));
		
		if (!filterEnBox.isChecked())
		{
			disableAll();
		}
		else if (!rgbEnBox.isChecked())
		{
			disableColor();
		}
		else
		{
			enableColor();
		}
		
		//		if (!tooletEnBox.isChecked())
		//		{
		//			relativelyBox.setEnabled(false);
		//		}
		
		rButton.setOnClickListener(onClickListener);
		gButton.setOnClickListener(onClickListener);
		bButton.setOnClickListener(onClickListener);
		aButton.setOnClickListener(onClickListener);
		
		rBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		gBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		bBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		aBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
		
		resetButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				rBar.setProgress(0);
				gBar.setProgress(0);
				bBar.setProgress(0);
				aBar.setProgress(0);
				saveData();
				XueBaYH.refreshToolet();
			}
		});
		
		filterEnBox.setOnCheckedChangeListener(onCheckedCheageListener);
		rgbEnBox.setOnCheckedChangeListener(onCheckedCheageListener);
		tooletEnBox.setOnCheckedChangeListener(onCheckedCheageListener);
		relativelyBox.setOnCheckedChangeListener(onCheckedCheageListener);
		
		relativelyBox.setVisibility(View.GONE);
	}
	
	private OnClickListener			onClickListener			= new OnClickListener()
															{
																@Override
																public void onClick(final View v)
																{
																	final EditText editText = new EditText(ScreenSettingActivity.this);
																	
																	new AlertDialog.Builder(ScreenSettingActivity.this).setTitle("设置颜色分量").setMessage("请输入一个0-255之间的整数. \n如果超出范围, 将截取. ").setIcon(android.R.drawable.ic_dialog_info).setView(editText).setPositiveButton("确定", new DialogInterface.OnClickListener()
																	{
																		@Override
																		public void onClick(DialogInterface dialog, int which)
																		{
																			String string = editText.getText().toString();
																			try
																			{
																				switch (v.getId())
																				{
																					case R.id.red_b:
																						ScreenSettingActivity.this.rBar.setProgress((int) (Integer.parseInt(string)));
																						break;
																					
																					case R.id.green_b:
																						ScreenSettingActivity.this.gBar.setProgress((int) (Integer.parseInt(string)));
																						break;
																					case R.id.blue_b:
																						ScreenSettingActivity.this.bBar.setProgress((int) (Integer.parseInt(string)));
																						break;
																					case R.id.alpha_b:
																						ScreenSettingActivity.this.aBar.setProgress((int) (Integer.parseInt(string)));
																						break;
																					default:
																						android.util.Log.i("wrong id", "" + v.getId());
																						break;
																				}
																			}
																			catch (NumberFormatException e)
																			{
																				XueBaYH.getApp().showToast("请检查输入格式");
																				return;
																			}
																			dialog.dismiss();
																		}
																	}).setNegativeButton("取消", new DialogInterface.OnClickListener()
																	{
																		@Override
																		public void onClick(DialogInterface dialog, int which)
																		{
																			dialog.dismiss();
																		}
																	}).create().show();
																	
																	saveData();
																	XueBaYH.getApp().refreshToolet();
																}
															};
	
	private OnSeekBarChangeListener	onSeekBarChangeListener	= new OnSeekBarChangeListener()
															{
																@Override
																public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
																{
																	saveData();
																	XueBaYH.getApp().refreshToolet();
																}
																
																@Override
																public void onStopTrackingTouch(SeekBar seekBar)
																{
																}
																
																@Override
																public void onStartTrackingTouch(SeekBar seekBar)
																{
																}
															};
	private OnCheckedChangeListener	onCheckedCheageListener	= new OnCheckedChangeListener()
															{
																
																@Override
																public void onCheckedChanged(final CompoundButton cb, boolean isChecked)
																{
																	switch (cb.getId())
																	{
																		case R.id.filter_en_cb:
																			if (isChecked)
																			{
																				aBar.setEnabled(true);
																				aButton.setEnabled(true);
																				tooletEnBox.setEnabled(true);
																				relativelyBox.setEnabled(true);
																				rgbEnBox.setEnabled(true);
																				
																				if (ScreenSettingActivity.this.rgbEnBox.isChecked())
																				{
																					enableColor();
																				}
																			}
																			else
																			{
																				disableAll();
																			}
																			break;
																		
																		case R.id.rgb_en_cb:
																			if (isChecked)
																			{
																				enableColor();
																			}
																			else
																			{
																				disableColor();
																			}
																			break;
																		case R.id.toolet_en_cb:
																			relativelyBox.setEnabled(isChecked);
																			break;
																	}
																	
																	saveData();
																	XueBaYH.getApp().refreshToolet();
																}
															};
	
	private void enableColor()
	{
		rBar.setEnabled(true);
		gBar.setEnabled(true);
		bBar.setEnabled(true);
		
		rButton.setEnabled(true);
		gButton.setEnabled(true);
		bButton.setEnabled(true);
		
		if (aBar.getProgress() <= 20)
		{
			aBar.setProgress(20);
		}
	}
	
	protected void saveData()
	{
		Editor editor = getSharedPreferences(XueBaYH.SCREEN, MODE_PRIVATE).edit();
		
		editor.putBoolean(XueBaYH.FILTER_EN, filterEnBox.isChecked());
		editor.putBoolean(XueBaYH.RGB_EN, rgbEnBox.isChecked());
		editor.putBoolean(XueBaYH.TOOLET_EN, tooletEnBox.isChecked());
		editor.putBoolean(XueBaYH.RELATIVELY_EN, relativelyBox.isChecked());
		
		editor.putInt(XueBaYH.RED, rBar.getProgress());
		editor.putInt(XueBaYH.GREEN, gBar.getProgress());
		editor.putInt(XueBaYH.BLUE, bBar.getProgress());
		editor.putInt(XueBaYH.ALPHA, aBar.getProgress());
		
		editor.commit();
	}
	
	private void disableColor()
	{
		rBar.setEnabled(false);
		gBar.setEnabled(false);
		bBar.setEnabled(false);
		
		rButton.setEnabled(false);
		gButton.setEnabled(false);
		bButton.setEnabled(false);
	}
	
	private void disableAll()
	{
		XueBaYH.getApp().tooletView.remove();
		aBar.setEnabled(false);
		aButton.setEnabled(false);
		tooletEnBox.setEnabled(false);
		relativelyBox.setEnabled(false);
		
		rgbEnBox.setEnabled(false);
		disableColor();
	}
}
