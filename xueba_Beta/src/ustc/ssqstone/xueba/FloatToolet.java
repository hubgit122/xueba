//package ustc.ssqstone.xueba;
//
//import android.annotation.SuppressLint;
//import android.graphics.PixelFormat;
//import android.view.Gravity;
//import android.view.WindowManager;
//import android.view.WindowManager.LayoutParams;
//
//public class FloatToolet
//{
//
//	@SuppressLint("NewApi")
//	private void createRestView()
//	{
//		myFV = new RestView(getApplicationContext());
//		myFV.setImageResource(R.drawable.resting);
//		wm = (WindowManager) getApplicationContext().getSystemService("window");
//		
//		screenWidth = wm.getDefaultDisplay().getWidth();
//		screenHeight = wm.getDefaultDisplay().getHeight();  
//		
//		wmParams = new WindowManager.LayoutParams();
//		
//		wmParams.type = LayoutParams.TYPE_PHONE;
//		wmParams.format = PixelFormat.RGBA_8888;
//
//		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE; //| LayoutParams.FLAG_NOT_TOUCHABLE ;
//		// | LayoutParams.FLAG_DIM_BEHIND ;// | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
//		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
//		// 以屏幕左上角为原点，设置x、y初始值
//		wmParams.x = 0;
//		wmParams.y = 0;
//		
//		// 设置悬浮窗口长宽数据
//		wmParams.width = 200;//WindowManager.LayoutParams.FILL_PARENT;
//		wmParams.height = 200;//WindowManager.LayoutParams.FILL_PARENT;
//		
//		wmParams.alpha = 0.95f;
//	}
//	private void updateViewPosition()
//	{
//		// 更新浮动窗口位置参数,x是鼠标在屏幕的位置，mTouchStartX是鼠标在图片的位置
//		wmParams.x = (int) (x - mTouchStartX);
//		wmParams.y = (int) (y - mTouchStartY);
//		wm.updateViewLayout(this, wmParams);
//	}
//}
package ustc.ssqstone.xueba;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.provider.Settings;
import android.text.StaticLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

class FloatToolet extends ImageView
{
	private WindowManager					wm			= (WindowManager) getContext().getApplicationContext().getSystemService("window");
	private FilterFloatView filterFloatView;
	// 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
	protected WindowManager.LayoutParams	wmParams	= new WindowManager.LayoutParams();
	private boolean							adjustingIA;
	
	public FloatToolet(Context context)
	{
		super(context);

		screenWidth = wm.getDefaultDisplay().getWidth();
		screenHeight = wm.getDefaultDisplay().getHeight();  
		
		setImageResource(R.drawable.toolet_button);
		wmParams = new WindowManager.LayoutParams();
		
		wmParams.type = LayoutParams.TYPE_PHONE;
		wmParams.format = PixelFormat.RGBA_8888;
		
		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
		
		wmParams.x = 0;
		wmParams.y = 0;
		
		wmParams.width = 150;
		wmParams.height = 150;
		
		wmParams.alpha = 0.6f;
		
		adjustingIA = false;
		filterFloatView = new FilterFloatView(getContext());
		filterFloatView.show();
	}
	
	private float	iconStartX;
	private float	iconStartY;
	private float	x;
	private float	y;
	private long	holdTime	= 0;
	private float	screenStartX;
	private float	screenStartY;
	
	private boolean	skip;

	private int	brightness;
	protected int screenWidth;
	protected int screenHeight;
	private boolean	relatively = false;
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		Rect frame = new Rect();
		getWindowVisibleDisplayFrame(frame);
		int statusBarHeight = frame.top;
		
		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				// getX()获取相对View的坐标，即以此View左上角为原点
				iconStartX = event.getX();
				iconStartY = event.getY();
				holdTime = Calendar.getInstance().getTimeInMillis();
				
				screenStartX = event.getRawX();
				screenStartY = event.getRawY() - statusBarHeight;
				
				adjustingIA = false;
				skip = false;
				break;
			case MotionEvent.ACTION_MOVE:
				// 调整图标位置
				x = event.getRawX(); // getRawX()获取相对屏幕的坐标，即以屏幕左上角为原点
				y = event.getRawY() - statusBarHeight;
				updateViewPosition();
				
				// 判断是否在原地长按
				if (!adjustingIA && ((Math.abs(x - screenStartX) > 20) || (Math.abs(y - screenStartY) > 20)))
				{
					skip = true;
				}
				if (!skip && !adjustingIA && (Calendar.getInstance().getTimeInMillis() - holdTime > 1000 * 1))
				{
					adjustingIA = true;
					XueBaYH.getApp().handler.sendEmptyMessage(XueBaYH.VIBRATE_LITTLE);
					
					setImageResource(R.drawable.toolet_button_i_a);
					
					brightness = this.getScreenBrightness();
					if (getScreenMode()==1)
					{
						setScreenMode(0);
					}
				}
				
				if (adjustingIA)
				{
					if (relatively )
					{
						setScreenBrightness((int)(brightness*(1+(x-screenStartX)/screenWidth)));
						filterFloatView.setAlpha((int)(brightness*(1+(y-screenStartY)/screenWidth)));
					}
					else
					{
						setScreenBrightness((int)(255*x/screenWidth));
						filterFloatView.setAlpha((int)(255*y/screenHeight));
					}
				}
				break;
			
			case MotionEvent.ACTION_UP:
				setImageResource(R.drawable.toolet_button);
				break;
		}
		// String string = "("+x+","+y+")   ("+mTouchStartX+","+mTouchStartY+")"
		// + event.getDownTime();
		// Log.i("xueba", string);
		return false;
	}

	/**
	 * 获得当前屏幕亮度的模式 SCREEN_BRIGHTNESS_MODE_AUTOMATIC=1 为自动调节屏幕亮度
	 * SCREEN_BRIGHTNESS_MODE_MANUAL=0 为手动调节屏幕亮度
	 */
	private int getScreenMode()
	{
		int screenMode = 0;
		try
		{
			screenMode = Settings.System.getInt(XueBaYH.getApp().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
		}
		catch (Exception localException)
		{
			
		}
		return screenMode;
	}
	
	/**
	 * 获得当前屏幕亮度值 0--255
	 */
	private int getScreenBrightness()
	{
		int screenBrightness = 255;
		try
		{
			screenBrightness = Settings.System.getInt(XueBaYH.getApp().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
		}
		catch (Exception localException)
		{
			
		}
		return screenBrightness;
	}
	
	/**
	 * 设置当前屏幕亮度的模式 SCREEN_BRIGHTNESS_MODE_AUTOMATIC=1 为自动调节屏幕亮度
	 * SCREEN_BRIGHTNESS_MODE_MANUAL=0 为手动调节屏幕亮度
	 */
	private void setScreenMode(int paramInt)
	{
		try
		{
			Settings.System.putInt(XueBaYH.getApp().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, paramInt);
		}
		catch (Exception localException)
		{
		}
	}
	
	/**
	 * 设置全局屏幕亮度值 0-255
	 */
	private boolean setScreenBrightness(int paramInt)
	{
		try
		{
			Settings.System.putInt(XueBaYH.getApp().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, paramInt);
		}
		catch (Exception e)
		{
		}
		return true;
	}
	
	private void updateViewPosition()
	{
		// 更新浮动窗口位置参数,x是鼠标在屏幕的位置，mTouchStartX是鼠标在图片的位置
		wmParams.x = (int) (x - iconStartX);
		wmParams.y = (int) (y - iconStartY);
		wm.updateViewLayout(this, wmParams);
	}
	
	// new Runnable()
	// {
	// @Override
	// public void run()
	// {
	// wait(millis)
	// }
	// }.run();
}