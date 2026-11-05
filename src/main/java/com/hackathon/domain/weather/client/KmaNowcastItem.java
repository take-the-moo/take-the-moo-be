package com.hackathon.domain.weather.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** 기상청 초단기실황 item. category: PTY(강수형태), T1H(기온), RN1(1시간 강수량) … */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KmaNowcastItem(String category, String obsrValue) {
}
