package com.hackathon.domain.postcard.render;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * 휴대폰 JPEG의 회전 정보(EXIF Orientation, 1~8). 휴대폰은 사진을 눕힌 채 저장하고 "보여줄 때 돌려라"는 값만 붙이는데,
 * Java ImageIO는 이 값을 무시해 사진이 옆으로 누워 나온다. 엽서에 원본 사진을 그대로 쓰므로 합성 전에 바로 세운다.
 * 라이브러리 없이 JPEG APP1(Exif) → TIFF IFD0의 0x0112 태그만 읽는다. 못 읽으면 1(그대로).
 */
final class ExifOrientation {

    static final int NORMAL = 1;
    private static final int TAG_ORIENTATION = 0x0112;

    private ExifOrientation() {
    }

    /** JPEG가 아니거나 회전 정보가 없으면 1 */
    static int read(byte[] jpeg) {
        try {
            if (jpeg.length < 4 || u8(jpeg, 0) != 0xFF || u8(jpeg, 1) != 0xD8) return NORMAL;
            int pos = 2;
            while (pos + 4 <= jpeg.length) {
                if (u8(jpeg, pos) != 0xFF) return NORMAL;
                int marker = u8(jpeg, pos + 1);
                if (marker == 0xDA || marker == 0xD9) return NORMAL;   // 이미지 데이터 시작 전까지만 본다
                int length = (u8(jpeg, pos + 2) << 8) | u8(jpeg, pos + 3);
                if (marker == 0xE1 && isExif(jpeg, pos + 4)) return fromTiff(jpeg, pos + 10);
                pos += 2 + length;
            }
        } catch (IndexOutOfBoundsException e) {
            // 깨진 EXIF는 무시한다
        }
        return NORMAL;
    }

    /** 회전 정보대로 돌리거나 뒤집은 새 이미지. 1이면 그대로 */
    static BufferedImage apply(BufferedImage image, int orientation) {
        if (orientation < 2 || orientation > 8) return image;
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = orientation >= 5;
        // AffineTransform(m00, m10, m01, m11, m02, m12): x' = m00·x + m01·y + m02, y' = m10·x + m11·y + m12
        AffineTransform transform = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);    // 좌우 뒤집기
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);   // 180도
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);    // 위아래 뒤집기
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);     // 대각선 뒤집기
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);    // 시계 방향 90도
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);   // 반대 대각선 뒤집기
            default -> new AffineTransform(0, -1, 1, 0, 0, w);   // 8: 반시계 방향 90도
        };
        BufferedImage rotated = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rotated.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(image, transform, null);
        } finally {
            g.dispose();
        }
        return rotated;
    }

    private static boolean isExif(byte[] b, int pos) {
        return b[pos] == 'E' && b[pos + 1] == 'x' && b[pos + 2] == 'i' && b[pos + 3] == 'f' && b[pos + 4] == 0 && b[pos + 5] == 0;
    }

    private static int fromTiff(byte[] b, int tiff) {
        boolean little = b[tiff] == 'I' && b[tiff + 1] == 'I';
        int ifd = tiff + int32(b, tiff + 4, little);
        int count = int16(b, ifd, little);
        for (int i = 0; i < count; i++) {
            int entry = ifd + 2 + i * 12;
            if (int16(b, entry, little) == TAG_ORIENTATION) {
                int value = int16(b, entry + 8, little);
                return value >= 1 && value <= 8 ? value : NORMAL;
            }
        }
        return NORMAL;
    }

    private static int u8(byte[] b, int pos) {
        return b[pos] & 0xFF;
    }

    private static int int16(byte[] b, int pos, boolean little) {
        return little ? u8(b, pos) | (u8(b, pos + 1) << 8) : (u8(b, pos) << 8) | u8(b, pos + 1);
    }

    private static int int32(byte[] b, int pos, boolean little) {
        return little
                ? u8(b, pos) | (u8(b, pos + 1) << 8) | (u8(b, pos + 2) << 16) | (u8(b, pos + 3) << 24)
                : (u8(b, pos) << 24) | (u8(b, pos + 1) << 16) | (u8(b, pos + 2) << 8) | u8(b, pos + 3);
    }
}
