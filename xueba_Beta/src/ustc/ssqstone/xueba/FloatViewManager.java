package ustc.ssqstone.xueba;

import android.content.Context;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

class RestView extends ImageView
{
	// 此wmParams为获取的全局变量，用以保存悬浮窗口的属性
	private WindowManager.LayoutParams	wmParams	= new WindowManager.LayoutParams();
																																		
	public RestView(Context context)
	{
		super(context);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		return false;
	}
}