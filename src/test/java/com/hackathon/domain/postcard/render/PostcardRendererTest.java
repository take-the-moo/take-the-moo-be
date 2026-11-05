package com.hackathon.domain.postcard.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.domain.postcard.entity.PostcardTemplate;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class PostcardRendererTest {

    private final PostcardRenderer renderer = new PostcardRenderer(new PostcardTemplateRegistry(JsonMapper.builder().build()));

    PostcardRendererTest() throws IOException {
    }

    @Test
    void 템플릿_크기의_JPEG를_만든다_긴_글과_여러_언어도_깨지지_않는다() throws IOException {
        String comment = "Хайрт Сөүл 독립문 앞에서 처음으로 한국의 가을을 느꼈다. Lần đầu tiên tôi cảm nhận mùa thu. 秋天的首尔很美 "
                .repeat(2);

        byte[] jpeg = renderer.render(PostcardTemplate.BASIC, png(1600, 900), "2026년 11월 7일, 독립문에서", comment);

        assertThat(jpeg[0]).isEqualTo((byte) 0xFF);
        assertThat(jpeg[1]).isEqualTo((byte) 0xD8);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertThat(image.getWidth()).isEqualTo(1200);
        assertThat(image.getHeight()).isEqualTo(1800);
    }

    @Test
    void 읽을_수_없는_사진이면_IOException() {
        assertThatThrownBy(() -> renderer.render(PostcardTemplate.BASIC, new byte[]{1, 2, 3}, "t", "c"))
                .isInstanceOf(IOException.class);
    }

    private static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
