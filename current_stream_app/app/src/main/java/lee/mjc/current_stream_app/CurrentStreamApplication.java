package lee.mjc.current_stream_app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 시스템 다크 모드와 무관하게 앱 UI를 라이트 테마로 고정합니다.
 * (레이아웃·drawable이 라이트 팔레트 기준으로 설계됨)
 */
public class CurrentStreamApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
