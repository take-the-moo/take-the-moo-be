package com.hackathon.domain.postcard.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ExifOrientationTest {

    @Test
    void 휴대폰_JPEG의_회전_정보를_읽는다() throws IOException {
        byte[] plain = jpeg(40, 20);

        assertThat(ExifOrientation.read(plain)).isEqualTo(1);
        assertThat(ExifOrientation.read(withOrientation(plain, 6, false))).isEqualTo(6);
        assertThat(ExifOrientation.read(withOrientation(plain, 8, true))).isEqualTo(8);
        assertThat(ExifOrientation.read(new byte[]{(byte) 0x89, 'P', 'N', 'G'})).isEqualTo(1);
        assertThat(ExifOrientation.read(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1, 0, 99})).isEqualTo(1);
    }

    @Test
    void 시계_방향_90도면_가로세로가_바뀌고_왼쪽_위가_오른쪽_위로_간다() {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());   // 왼쪽 위 점

        BufferedImage rotated = ExifOrientation.apply(image, 6);

        assertThat(rotated.getWidth()).isEqualTo(20);
        assertThat(rotated.getHeight()).isEqualTo(40);
        assertThat(rotated.getRGB(19, 0)).isEqualTo(Color.RED.getRGB());
        assertThat(ExifOrientation.apply(image, 1)).isSameAs(image);
    }

    @Test
    void 반시계_방향_90도면_왼쪽_위가_왼쪽_아래로_간다() {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.RED.getRGB());

        BufferedImage rotated = ExifOrientation.apply(image, 8);

        assertThat(rotated.getRGB(0, 39)).isEqualTo(Color.RED.getRGB());
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpeg", out);
        return out.toByteArray();
    }

    /** SOI 바로 뒤에 Orientation 태그 하나만 있는 APP1(Exif)을 끼운다 */
    private static byte[] withOrientation(byte[] jpeg, int orientation, boolean littleEndian) {
        byte[] tiff = littleEndian
                ? new byte[]{'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, (byte) orientation, 0, 0, 0, 0, 0, 0, 0}
                : new byte[]{'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1, 0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0, 0, 0, 0, 0};
        byte[] header = {'E', 'x', 'i', 'f', 0, 0};
        int length = 2 + header.length + tiff.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(0xFF);
        out.write(0xE1);
        out.write(length >> 8);
        out.write(length & 0xFF);
        out.writeBytes(header);
        out.writeBytes(tiff);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}
