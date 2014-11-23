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

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

class TooletFloatView extends ImageView
{
	private static final int				viewHeight	= 200;
	private static final int				viewWidth	= 200;
	private WindowManager					wm			= (WindowManager) getContext().getApplicationContext().getSystemService("window");
	protected FilterFloatView				filterFloatView;
	// 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
	protected WindowManager.LayoutParams	wmParams	= new WindowManager.LayoutParams();
	private boolean							adjustingIA;
	
	public TooletFloatView(Context context)
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
		
		wmParams.width = viewWidth;
		wmParams.height = viewHeight;
		
		wmParams.alpha = 0.6f;
		
		adjustingIA = false;
		
		filterFloatView = new FilterFloatView(getContext());
		refreshStatusHeight();
		
		alpha = XueBaYH.getAlpha();
	}
	
	private float	iconStartX;
	private float	iconStartY;
	private float	x;
	private float	y;
	private long	holdTime	= 0;
	private float	screenStartX;
	private float	screenStartY;
	
	private boolean	skip;
	
	private int		brightness;
	protected int	screenWidth;
	protected int	screenHeight;
	private boolean	relatively	= false;
	private float	resizeScale	= 0.3f;
	private boolean	showing		= false;
	protected int	statusBarHeight;
	private float	alpha;
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		refreshStatusHeight();
		
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
				
				wmParams.width = viewWidth;
				wmParams.height = viewHeight;
				break;
			case MotionEvent.ACTION_MOVE:
				// 调整图标位置
				x = ((event.getRawX() >= iconStartX) && (event.getRawX() - iconStartX + viewWidth <= screenWidth)) ? event.getRawX() : x; // getRawX()获取相对屏幕的坐标，即以屏幕左上角为原点
				y = ((event.getRawY() - iconStartY >= statusBarHeight) && (event.getRawY() - iconStartY + viewHeight <= screenHeight)) ? event.getRawY() - statusBarHeight : y;
				
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
					if (getScreenMode() == 1)
					{
						setScreenMode(0);
					}
				}
				
				if (adjustingIA)
				{
					if (relatively)
					{
						setScreenBrightness(Math.max((int) (brightness * (1 + (x - screenStartX) / (screenWidth - viewWidth))), 5));
						filterFloatView.setAlpha(Math.max((int) (alpha * (1 + (y - screenStartY) / (screenHeight - statusBarHeight - viewHeight))), 5));
					}
					else
					{
						setScreenBrightness(Math.max((int) (255 * (x - iconStartX) / (screenWidth - viewWidth)), 5));
						filterFloatView.setAlpha(Math.max((int) (255 * (y - iconStartY) / (screenHeight - statusBarHeight - viewHeight)), 5));
					}
				}
				break;
			
			case MotionEvent.ACTION_UP:
				setImageResource(R.drawable.toolet_button);
				alpha = XueBaYH.getApp().getAlpha();
				updateSize();
				break;
		}
		return false;
	}
	
	private void refreshStatusHeight()
	{
		Rect frame = new Rect();
		getWindowVisibleDisplayFrame(frame);
		statusBarHeight = frame.top;
	}
	
	private void updateSize()
	{
		if (x - iconStartX < 100)
		{
			x = iconStartX;
			wmParams.width = (int) (resizeScale * viewWidth);
		}
		if (y - iconStartY < 100)
		{
			y = iconStartY;
			wmParams.height = (int) (resizeScale * viewHeight);
		}
		
		if (x - iconStartX + viewWidth + 100 > screenWidth)
		{
			x = screenWidth + iconStartX - resizeScale * viewWidth;
			wmParams.width = (int) (resizeScale * viewWidth);
		}
		
		if (y - iconStartY + viewHeight + 100 > screenHeight)
		{
			y = screenHeight - statusBarHeight + iconStartY - resizeScale * viewHeight;
			wmParams.height = (int) (resizeScale * viewHeight);
		}
		updateViewPosition();
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
	
	public void refresh(boolean filterEn, boolean rgbEn, boolean tooletEn, boolean relativelyEn, int r, int g, int b, int a)
	{
		if (!filterEn)
		{
			filterFloatView.remove();
		}
		else
		{
			filterFloatView.refresh(filterEn, r, g, b, a);
		}
		
		if (!rgbEn)
		{
			filterFloatView.resetColor();
		}
		
		if (!tooletEn || !filterEn)
		{
			try
			{
				remove();
			}
			catch (Exception e)
			{
			}
		}
		else
		{
			show();
		}
		
		relatively = relativelyEn;
	}
	
	public void show()
	{
		if (!showing)
		{
			try
			{
				wm.addView(this, this.wmParams);
				updateSize();
				showing = true;
			}
			catch (Exception e)
			{
			}
		}
		else
		{
			wm.updateViewLayout(this, this.wmParams);
		}
	}
	
	public void remove()
	{
		try
		{
			wm.removeView(this);
			showing = false;
		}
		catch (Exception e)
		{
		}
	}
}