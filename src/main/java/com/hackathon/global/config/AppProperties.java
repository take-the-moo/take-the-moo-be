package com.hackathon.global.config;

import java.time.Duration;
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
        @DefaultValue Demo demo
) {

    /** 업로드 사진·생성 엽서를 저장하는 로컬 디렉터리 */
    public record Storage(@DefaultValue("./uploads") String dir) {
    }

    /**
     * 기상청 초단기실황(공공데이터포털). serviceKey는 "Decoding" 키를 넣는다.
     * 비어 있으면 실제 조회 없이 기본 날씨(CLEAR)로 동작한다.
     */
    public record Weather(
            @DefaultValue("") String kmaServiceKey,
            @DefaultValue("http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0") String kmaBaseUrl,
            // 서대문구 격자 좌표 (기상청 격자 변환표 기준)
            @DefaultValue("59") int nx,
            @DefaultValue("127") int ny,
            @DefaultValue("10m") Duration cacheTtl
    ) {
    }

    /**
     * 한국관광공사 TourAPI(국문 관광정보). 장소 기본 정보는 이 API로 받아 DB에 동기화한다.
     * serviceKey는 "Decoding" 키. 비어 있으면 동기화를 건너뛰고 DB에 있는 데이터로 동작한다.
     * 지역 코드는 TourAPI 버전에 따라 체계가 다를 수 있으니 문서에서 확인하고 고친다.
     */
    public record Tour(
            @DefaultValue("") String serviceKey,
            @DefaultValue("https://apis.data.go.kr/B551011/KorService2") String baseUrl,
            // 서울
            @DefaultValue("1") String areaCode,
            // 서대문구
            @DefaultValue("14") String sigunguCode,
            // 발표 전에는 false로 꺼서 리허설한 데이터가 바뀌지 않게 한다
            @DefaultValue("true") boolean syncEnabled,
            // 매일 새벽 4시 (서울 시간)
            @DefaultValue("0 0 4 * * *") String syncCron
    ) {
    }

    /** 위치·역을 못 받았을 때 기준이 되는 역 */
    public record Demo(@DefaultValue("dongnimmun") String defaultStation) {
    }
}
