package com.hackathon.domain.visit.course;

import com.hackathon.global.common.Language;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * data/courses.json을 시작할 때 읽는다. 고친 뒤 서버를 다시 켜면 반영된다.
 * 형식이 틀리면 서버가 뜨지 않는다 (코스가 조용히 빠지는 것보다 낫다).
 */
@Slf4j
@Component
public class CourseRegistry {

    static final String LOCATION = "data/courses.json";

    private final List<Course> courses;

    @Autowired
    public CourseRegistry(JsonMapper jsonMapper) throws IOException {
        this(load(jsonMapper));
    }

    CourseRegistry(List<Course> courses) {
        validate(courses);
        this.courses = List.copyOf(courses);
        log.info("코스 데이터: {}건 ({})", this.courses.size(), LOCATION);
    }

    public List<Course> findAll() {
        return courses;
    }

    /** 이 장소가 들어 있는 코스 (파일 순서) */
    public List<Course> findByContentId(String contentId) {
        return courses.stream().filter(course -> course.contentIds().contains(contentId)).toList();
    }

    private static List<Course> load(JsonMapper jsonMapper) throws IOException {
        try (InputStream in = new ClassPathResource(LOCATION).getInputStream()) {
            return Arrays.asList(jsonMapper.readValue(in, Course[].class));
        }
    }

    private static void validate(List<Course> courses) {
        Set<String> ids = new HashSet<>();
        for (Course course : courses) {
            String id = course.courseId();
            if (id == null || id.isBlank()) throw new IllegalStateException(LOCATION + ": courseId가 없는 코스가 있습니다");
            if (!ids.add(id)) throw new IllegalStateException(LOCATION + ": courseId 중복 " + id);
            if (course.name() == null || !course.name().containsKey(Language.KO)) {
                throw new IllegalStateException(LOCATION + ": 한국어 이름이 없습니다 " + id);
            }
            if (course.contentIds() == null || course.contentIds().isEmpty()) {
                throw new IllegalStateException(LOCATION + ": 장소가 없습니다 " + id);
            }
        }
    }
}
