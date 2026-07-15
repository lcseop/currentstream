package lee.mjc.current_stream_app;

/**
 * API 베이스 URL 같은 네트워크 설정 상수 모음임.
 * BuildConfig에서 서버 주소 받아와 쓰는 용도.
 */
public final class ApiConfig {
    /** 백엔드 서버 기준 URL */
    public static final String BASE_URL = BuildConfig.API_BASE_URL;

    /** 유틸 클래스라 인스턴스 생성 막음 */
    private ApiConfig() {
    }
}
