package com.hackathon.domain.postcard.render;

import com.hackathon.domain.postcard.entity.PostcardTemplate;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
            templates.put(template, new Loaded(layout, background(layout), fonts(layout)));
        }
    }

    public Loaded get(PostcardTemplate template) {
        return templates.get(template);
    }

    /**
     * @param background 배경 이미지 (없으면 null)
     * @param fonts      글꼴 후보 (앞에서부터, 크기 1이라 쓸 때 deriveFont). 비어 있으면 SansSerif만 쓴다
     */
    public record Loaded(PostcardLayout layout, BufferedImage background, List<ScaledFont> fonts) {
    }

    record File(String _comment, Map<String, PostcardLayout> templates) {
    }

    private static BufferedImage background(PostcardLayout layout) throws IOException {
        if (layout.backgroundImage() == null) return null;
        try (InputStream in = new ClassPathResource(layout.backgroundImage()).getInputStream()) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) throw new IllegalStateException("배경 이미지를 읽지 못했습니다: " + layout.backgroundImage());
            // 배경은 사진 위에 덮어 그린다. 사진 칸이 뚫려 있지 않으면 엽서에 사진이 안 보인다
            PostcardLayout.Box art = layout.art();
            int centerX = (art.x() + art.width() / 2) * image.getWidth() / layout.width();
            int centerY = (art.y() + art.height() / 2) * image.getHeight() / layout.height();
            if (!image.getColorModel().hasAlpha() || (image.getRGB(centerX, centerY) >>> 24) != 0) {
                throw new IllegalStateException("배경 이미지의 사진 칸이 투명하지 않습니다: " + layout.backgroundImage());
            }
            return image;
        }
    }

    private static List<ScaledFont> fonts(PostcardLayout layout) throws IOException {
        List<ScaledFont> fonts = new ArrayList<>();
        if (layout.fonts() == null) return fonts;
        for (PostcardLayout.FontRef ref : layout.fonts()) {
            try (InputStream in = new ClassPathResource(ref.file()).getInputStream()) {
                fonts.add(new ScaledFont(Font.createFont(Font.TRUETYPE_FONT, in), ref.scale() != null ? ref.scale() : 1f));
            } catch (FontFormatException e) {
                throw new IllegalStateException("글꼴을 읽지 못했습니다: " + ref.file(), e);
            }
        }
        return fonts;
    }
}
