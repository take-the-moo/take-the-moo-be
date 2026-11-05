package com.hackathon.domain.postcard.render;

import com.hackathon.domain.postcard.entity.PostcardTemplate;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 템플릿 + 원본 사진 + 제목 + 한 문장 → 엽서 JPEG. 사진은 회전 정보(EXIF)대로 세운 뒤 사진 칸에 맞춰 가운데를 자른다.
 * 그리는 순서: 배경색 → 사진 → 배경 PNG(사진 칸이 투명) → 제목·한 문장.
 */
@Component
@RequiredArgsConstructor
public class PostcardRenderer {

    private static final float JPEG_QUALITY = 0.9f;

    private final PostcardTemplateRegistry registry;

    /**
     * @param photo jpeg·png·webp 원본 사진 바이트
     * @throws IOException 사진을 읽지 못하면 (지원하지 않는 형식·깨진 파일)
     */
    public byte[] render(PostcardTemplate template, byte[] photo, String title, String comment) throws IOException {
        PostcardTemplateRegistry.Loaded loaded = registry.get(template);
        PostcardLayout layout = loaded.layout();
        BufferedImage read = ImageIO.read(new ByteArrayInputStream(photo));
        if (read == null) throw new IOException("사진을 읽지 못했습니다 (지원하지 않는 형식)");
        BufferedImage photoImage = ExifOrientation.apply(read, ExifOrientation.read(photo));

        BufferedImage canvas = new BufferedImage(layout.width(), layout.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g.setColor(Color.decode(layout.backgroundColor()));
            g.fillRect(0, 0, layout.width(), layout.height());
            drawCover(g, photoImage, layout.art());
            if (loaded.background() != null) {
                // 사진 칸이 뚫린 배경을 사진 위에 덮는다 → 칸 테두리·둥근 모서리가 사진 위에 남는다
                g.drawImage(loaded.background(), 0, 0, layout.width(), layout.height(), null);
            }
            TextPainter.draw(g, title, layout.title(), loaded.fonts());
            TextPainter.draw(g, comment, layout.comment(), loaded.fonts());
        } finally {
            g.dispose();
        }
        return jpeg(canvas);
    }

    /** 칸을 꽉 채우도록 늘리고 넘치는 쪽 가운데를 자른다 */
    private static void drawCover(Graphics2D g, BufferedImage image, PostcardLayout.Box box) {
        double scale = Math.max((double) box.width() / image.getWidth(), (double) box.height() / image.getHeight());
        int sourceWidth = (int) Math.round(box.width() / scale);
        int sourceHeight = (int) Math.round(box.height() / scale);
        int sourceX = (image.getWidth() - sourceWidth) / 2;
        int sourceY = (image.getHeight() - sourceHeight) / 2;
        g.drawImage(image, box.x(), box.y(), box.x() + box.width(), box.y() + box.height(),
                sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight, null);
    }

    private static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
