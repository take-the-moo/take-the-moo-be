package com.hackathon.domain.place.curation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 내 나라 연결 한 건 (data/place-connections.json). 서대문 장소 하나 × 국적 하나에 대해 사람이 고른 비슷한 사례.
 * F2 추천 이유(한 문장 훅)와 F3 한 줄 연결(자세한 풀이)이 같은 값을 써서 두 화면의 말이 어긋나지 않는다.
 *
 * <pre>
 * "VN": {
 *   "counterpart": "하노이 호아로 수용소",                       // 그 나라의 비슷한 장소·사건 (한국어 이름). 없으면 특정 사례를 들지 않는다
 *   "localName": "Nhà tù Hỏa Lò",                               // 현지어 이름 (선택). AI가 그 나라 말로 쓸 때 이 표기를 그대로 쓴다
 *   "link": "식민지 시기 독립운동가들이 갇혔던 감옥을 …"          // 무엇이 닮았는지 (한국어, 짧게). 필수
 * }
 * </pre>
 *
 * @param counterpart 비슷한 사례 이름. null이면 AI는 특정 사례를 들지 않고 link 방향으로만 잇는다 (가해 쪽으로 얽힌 국적 등)
 * @param localName   counterpart의 현지어 이름 (선택). 한국어 이름만 주면 AI가 현지어로 되옮기다 철자를 틀린다 (베트남어 성조 등)
 * @param link        닮은 점. 서대문 장소 쪽 사실은 그 장소 개요에 있는 것만 쓴다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceConnection(String counterpart, String localName, String link) {
}
