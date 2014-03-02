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

import android.R;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

class FloatToolet extends ImageView
{
	private float						mTouchStartX;
	private float						mTouchStartY;
	private float						x;
	private float						y;
	private WindowManager				wm			= (WindowManager) getContext().getApplicationContext().getSystemService("window");
	
	// 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
	private WindowManager.LayoutParams	wmParams	= new WindowManager.LayoutParams();
	
	public FloatToolet(Context context)
	{
		super(context);
		
//		setImageResource(R.drawable.);
		wmParams = new WindowManager.LayoutParams();
		
		wmParams.type = LayoutParams.TYPE_PHONE;
		wmParams.format = PixelFormat.RGBA_8888;
		
		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
		// 以屏幕左上角为原点，设置x、y初始值
		wmParams.x = 0;
		wmParams.y = 0;
		
		// 设置悬浮窗口长宽数据
		wmParams.width = 150;
		wmParams.height = 150;
		
		wmParams.alpha = 0.9f;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		// getRawX()获取相对屏幕的坐标，即以屏幕左上角为原点
		x = event.getRawX();
		y = event.getRawY() - 25;
		
		String tag = "xueba";
		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				// getX()获取相对View的坐标，即以此View左上角为原点
				mTouchStartX = event.getX();
				mTouchStartY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				updateViewPosition();
				break;
			
			case MotionEvent.ACTION_UP:
				mTouchStartX = mTouchStartY = 0;
				break;
		}
		return false;
	}
	
	private void updateViewPosition()
	{
		// 更新浮动窗口位置参数,x是鼠标在屏幕的位置，mTouchStartX是鼠标在图片的位置
		wmParams.x = (int) (x - mTouchStartX);
		wmParams.y = (int) (y - mTouchStartY);
		wm.updateViewLayout(this, wmParams);
	}
}