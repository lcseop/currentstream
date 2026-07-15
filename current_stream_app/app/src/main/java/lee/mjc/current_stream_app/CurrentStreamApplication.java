package lee.mjc.current_stream_app;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * 앱 전역 Application 클래스임.
 * 시스템 다크 모드랑 상관없이 라이트 테마로 고정함.
 * 레이아웃이 라이트 기준이라 안 하면 색 이상해짐
 */
public class CurrentStreamApplication extends Application {

    /** 앱 프로세스 시작할 때 한 번 호출됨 */
    @Override
    public void onCreate() {
        super.onCreate();
        // 시스템 다크 모드 무시하고 라이트 UI 유지
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
