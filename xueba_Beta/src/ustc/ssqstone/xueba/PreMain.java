package ustc.ssqstone.xueba;

import ustc.ssqstone.xueba.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;

public class PreMain extends Activity
{
	static protected final int	INIT_VIEW	= 0;
	protected Handler			handler		= new Handler()
											{
												@Override
												public void handleMessage(Message msg)
												{
													switch (msg.what)
													{
														case INIT_VIEW:
															
															Intent intent = new Intent(PreMain.this, MainActivity.class);
															intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
															startActivity(intent);
															finish();
															break;
														
														default:
															break;
													}
													super.handleMessage(msg);
												}
											};
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.hello);
		
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				ConditionVariable conditionVariable = new ConditionVariable(false);
				conditionVariable.block(3000);
				
				handler.sendEmptyMessage(INIT_VIEW);
			}
		};
		
		new Thread(runnable, "delay").start();
	}
}
