package ustc.ssqstone.xueba;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

public class FilterFloatView extends ImageView
{
	private WindowManager					wm			= (WindowManager) getContext().getApplicationContext().getSystemService("window");
	protected WindowManager.LayoutParams	wmParams	= new WindowManager.LayoutParams();
	
	public FilterFloatView(Context context)
	{
		super(context);

		wmParams = new WindowManager.LayoutParams();
		
		wmParams.type = LayoutParams.TYPE_PHONE;
		wmParams.format = PixelFormat.RGBA_8888;
		
		wmParams.flags = LayoutParams.FLAG_NOT_TOUCH_MODAL | LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
		wmParams.gravity = Gravity.LEFT | Gravity.TOP;
		
		wmParams.x = 0;
		wmParams.y = 0;
		
		wmParams.width = -1;
		wmParams.height = -1;
		
		wmParams.alpha = 0.9f;
		
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		
		canvas.drawARGB(255, 0, 0, 0);
	}
	
	@Override
	public void setAlpha(int alpha)
	{
		wmParams.alpha = alpha;
		wm.updateViewLayout(this, wmParams);
	}
	
	protected void show()
	{
		wm.addView(this, this.wmParams);
	}
}
