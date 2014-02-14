package ustc.ssqstone.xueba;

import java.util.Set;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class EditorWithParity
{
	Editor mEditor;
	SharedPreferences mSharedPreferences;
	
	EditorWithParity(SharedPreferences sharedPreferences)
	{
		mSharedPreferences = sharedPreferences;
		mEditor = sharedPreferences.edit();
	}
	

	public void apply()
	{
		mEditor.apply();
	}

	public Editor clear()
	{
		return mEditor.clear();
	}

	public void commit()
	{
		XueBaYH.getApp().checkParity(this);
	}

	public Editor putBoolean(String key, boolean value)
	{
		return mEditor.putBoolean(key, value);
	}

	public Editor putFloat(String key, float value)
	{
		return mEditor.putFloat(key, value);
	}

	public Editor putInt(String key, int value)
	{
		return mEditor.putInt(key, value);
	}

	public Editor putLong(String key, long value)
	{
		return mEditor.putLong(key, value);
	}

	public Editor putString(String key, String value)
	{
		return mEditor.putString(key, value);
	}

	@SuppressLint("NewApi")
	public Editor putStringSet(String arg0, Set<String> arg1)
	{
		return mEditor.putStringSet(arg0, arg1);
	}

	public Editor remove(String key)
	{
		return mEditor.remove(key);
	}
	
}
