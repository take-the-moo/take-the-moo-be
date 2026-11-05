package com.hackathon.domain.postcard.render;

import com.hackathon.domain.postcard.entity.PostcardTemplate;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * resources/postcard/templates.json을 시작할 때 읽는다. 상수에 맞는 배치가 없거나 배경·글꼴 파일을 못 읽으면
 * 서버가 시작되지 않는다 (시연 중에 엽서가 깨지는 것보다 낫다). 고친 뒤 서버를 다시 켜면 반영된다.
 */
@Component
public class PostcardTemplateRegistry {

    static final String PATH = "postcard/templates.json";

    private final Map<PostcardTemplate, Loaded> templates = new EnumMap<>(PostcardTemplate.class);

    public PostcardTemplateRegistry(JsonMapper jsonMapper) throws IOException {
        File file;
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            file = jsonMapper.readValue(in, File.class);
        }
        for (PostcardTemplate template : PostcardTemplate.values()) {
            PostcardLayout layout = file.templates().get(template.name());
            if (layout == null) {
                throw new IllegalStateException(PATH + "에 " + template + " 템플릿이 없습니다");
            }
            templates.put(template, new Loaded(layout, background(layout), font(layout)));
        }
    }

    public Loaded get(PostcardTemplate template) {
        return templates.get(template);
    }

    /** @param background 배경 이미지 (없으면 null) / @param font 기본 글꼴 (크기 1, 쓸 때 deriveFont) */
    public record Loaded(PostcardLayout layout, BufferedImage background, Font font) {
    }

    record File(String _comment, Map<String, PostcardLayout> templates) {
    }

    private static BufferedImage background(PostcardLayout layout) throws IOException {
        if (layout.backgroundImage() == null) return null;
        try (InputStream in = new ClassPathResource(layout.backgroundImage()).getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IllegalStateException("배경 이미지를 읽지 못했습니다: " + layout.backgroundImage());
            return image;
        }
    }

    private static Font font(PostcardLayout layout) throws IOException {
        if (layout.fontFile() == null) return new Font(Font.SANS_SERIF, Font.PLAIN, 1);
        try (InputStream in = new ClassPathResource(layout.fontFile()).getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (FontFormatException e) {
            throw new IllegalStateException("글꼴을 읽지 못했습니다: " + layout.fontFile(), e);
        }
    }
}
