package com.hackathon.global.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * application.yml의 app.* 설정 (서비스 기능용). 값이 없어도 서버는 뜬다 (외부 키가 없으면 시뮬레이션·기본값으로 동작).
 * AI 키는 여기가 아니라 spring.ai.anthropic.api-key / spring.ai.openai.api-key에 둔다.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @DefaultValue Storage storage,
        @DefaultValue Weather weather,
        @DefaultValue Tour tour,
        @DefaultValue Station station,
        @DefaultValue Demo demo,
        @DefaultValue Visit visit,
        @DefaultValue Auth auth
) {

    /**
     * 앱 내 로그인 JWT.
     * - jwtSecret: HS256 서명 키, 32바이트 이상 (환경변수 APP_AUTH_JWT_SECRET). 비어 있으면 서버를 켤 때 임시 키를 만든다
     *   (재시작하면 모든 토큰이 무효가 되어 다시 로그인해야 한다 — 배포 환경에서는 꼭 넣는다)
     * - tokenTtl: 토큰 유효 기간. 자동 로그인을 위해 길게 둔다
     * - enforceToken: userId를 쓰는 API에서 토큰의 사용자와 같은지 확인한다. 테스트 설정에서만 끈다 (실제로는 늘 켠다)
     */
    public record Auth(
            @DefaultValue("") String jwtSecret,
            @DefaultValue("30d") Duration tokenTtl,
            @DefaultValue("true") boolean enforceToken
    ) {
    }

    /** 방문 도장. 장소에서 이 반경(미터) 안이어야 도장을 준다 */
    public record Visit(@DefaultValue("200") int stampRadiusMeters) {
    }

    /** 업로드 사진·생성 엽서를 저장하는 로컬 디렉터리 */
    public record Storage(@DefaultValue("./uploads") String dir) {
    }

    /**
     * 기상청 초단기실황(공공데이터포털). serviceKey는 "Decoding" 키를 넣는다.
     * 비어 있으면 실제 조회 없이 기본 날씨(CLEAR)로 동작한다.
     */
    public record Weather(
            @DefaultValue("") String kmaServiceKey,
            @DefaultValue("https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0") String kmaBaseUrl,
            // 서대문구 격자 좌표 (기상청 격자 변환표 기준)
            @DefaultValue("59") int nx,
            @DefaultValue("127") int ny,
            @DefaultValue("10m") Duration cacheTtl
    ) {
    }

    /**
     * 한국관광공사 TourAPI(국문 관광정보). 장소 기본 정보는 이 API로 받아 DB에 동기화한다.
     * serviceKey는 "Decoding" 키. 비어 있으면 동기화를 건너뛰고 DB에 있는 데이터로 동작한다.
     * KorService2는 지역을 법정동 코드(lDongRegnCd·lDongSignguCd)로 거른다. 예전 areaCode로 거르면
     * areacode가 빈 장소가 빠지므로 쓰지 않는다. lDongSignguCd는 lDongRegnCd와 함께 보내야 한다.
     */
    public record Tour(
            @DefaultValue("") String serviceKey,
            @DefaultValue("https://apis.data.go.kr/B551011/KorService2") String baseUrl,
            // 법정동 시도 코드: 서울특별시
            @DefaultValue("11") String lDongRegnCd,
            // 법정동 시군구 코드: 서대문구 (법정동코드 11410의 뒤 3자리)
            @DefaultValue("410") String lDongSignguCd,
            // 받을 관광 타입: 12 관광지, 14 문화시설, 38 쇼핑(전통시장). 음식점(39)·숙박(32)은 받지 않는다
            @DefaultValue({"12", "14", "38"}) List<String> contentTypeIds,
            // 받자마자 노출(ACTIVE)할 타입. 그 밖의 타입(쇼핑 등)은 보강 파일에 있는 곳만 노출한다
            @DefaultValue({"12", "14"}) List<String> autoActiveTypeIds,
            // 발표 전에는 false로 꺼서 리허설한 데이터가 바뀌지 않게 한다
            @DefaultValue("true") boolean syncEnabled,
            // 매일 새벽 4시 (서울 시간)
            @DefaultValue("0 0 4 * * *") String syncCron
    ) {
    }

    /**
     * 서울교통빅데이터플랫폼(T Data) 지하철역 좌표정보 API. 응답의 convX = 경도, convY = 위도.
     * 원천 갱신이 분기 1회라 서버 시작 시 역 테이블이 비어 있으면 1회 + 장소 동기화 스케줄 때 함께 받는다.
     * apiKey가 비어 있으면 건너뛰고 DB에 있는 역으로 동작한다.
     */
    public record Station(
            @DefaultValue("") String apiKey,
            // http로 부르면 302로 https에 리다이렉트되므로 처음부터 https를 쓴다
            @DefaultValue("https://t-data.seoul.go.kr/apig/apiman-gateway/tapi/TaimsKsccDvSubwayStationGeom/1.0") String baseUrl,
            @DefaultValue("true") boolean syncEnabled,
            // 위치 거부 시 "역 선택" 목록: 서대문 생활권 역 이름(station.name과 정확히 같게). 이 순서대로 보여준다.
            // 거리로 거르지 않는다 (3km 반경이면 가좌가 빠지고 시청·명동이 들어간다). 같은 이름의 호선은 모두 보여준다
            @DefaultValue({"독립문", "무악재", "홍제", "서대문", "충정로(경기대입구)", "아현", "이대", "신촌", "가좌"})
            List<String> selectableNames
    ) {
    }

    /**
     * 위치·역을 못 받았을 때 기준 위치 (발표장에서는 GPS가 잡히지 않는다).
     * - defaultLatitude·defaultLongitude가 둘 다 있으면 그 좌표를 쓰고 이름은 defaultName (기본 명지대 인문캠퍼스, 거북골로 34)
     * - 좌표를 비우면(빈 값) defaultStation 역을 쓴다
     * - defaultStation은 "역 선택" 목록의 기본 역(isDefault)이기도 하다
     */
    public record Demo(
            @DefaultValue("독립문") String defaultStation,
            @DefaultValue("37.5803") Double defaultLatitude,
            @DefaultValue("126.9233") Double defaultLongitude,
            @DefaultValue("명지대 인문캠퍼스") String defaultName
    ) {
    }
}
