package ustc.ssqstone.xueba;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

class RestView extends ImageView
{
	private float						mTouchStartX;
	private float						mTouchStartY;
	private float						x;
	private float						y;
	
	private WindowManager				wm			= (WindowManager) getContext().getApplicationContext().getSystemService("window");
	
	// 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
	private WindowManager.LayoutParams	wmParams	= new WindowManager.LayoutParams();												// ((XueBaYH)
																																		// getContext().getApplicationContext()).getMywmParams();
																																		
	public RestView(Context context)
	{
		super(context);
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
				Log.i(tag , "ACTION_DOWN: "+mTouchStartX+", "+mTouchStartY);
				break;
			case MotionEvent.ACTION_MOVE:
				mTouchStartX = event.getX();
				mTouchStartY = event.getY();
//				Log.i(tag , "ACTION_MOVE: "+mTouchStartX+", "+mTouchStartY);
//				updateViewPosition();
				break;
			
			case MotionEvent.ACTION_UP:
				mTouchStartX = event.getX();
				mTouchStartY = event.getY();
				Log.i(tag , "ACTION_UP: "+mTouchStartX+", "+mTouchStartY);
//				updateViewPosition();
				mTouchStartX = mTouchStartY = 0;
				break;
			case MotionEvent.ACTION_OUTSIDE:
				Log.i(tag , "ACTION_OUTSIDE: "+event.getDownTime());
				break;
				
//			case MotionEvent.ACTION_HOVER_ENTER:
//				Log.i(tag , "ACTION_HOVER_ENTER");
//				break;
		}
		return false;
	}
	
	private void updateViewPosition()
	{
		// 更新浮动窗口位置参数,x是鼠标在屏幕的位置，mTouchStartX是鼠标在图片的位置
		wmParams.x = (int) (x - mTouchStartX);
		System.out.println(mTouchStartX);
		wmParams.y = (int) (y - mTouchStartY);
		wm.updateViewLayout(this, wmParams);
	}
}