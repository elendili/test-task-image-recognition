package imr;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class Card {
    final static Function<BufferedImage, Integer> sCGetter = img -> img.getRGB(2 * img.getWidth() / 3, 4 * img.getHeight() / 5);
    final static Function<BufferedImage, Integer> bgCGetter = img -> img.getRGB(img.getWidth() / 2, img.getHeight() / 2);
    final static BiFunction<Integer, Integer, Integer> diff = (c1, c2) ->
            max(max(abs(((c1 >> 16) & 0xFF) - ((c2 >> 16) & 0xFF)), abs(((c1 >> 8) & 0xFF) - ((c2 >> 8) & 0xFF))), abs(((c1) & 0xFF) - ((c2) & 0xFF)));

    final static String[] ranks = new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
    final static int[][] rankPatterns = new int[][]{{40, 45, 69, 0, 17, 62, 55, 67, 28}, {31, 51, 80, 0, 60, 47, 43, 29, 71},
            {0, 35, 62, 34, 26, 62, 36, 42, 77}, {58, 43, 42, 44, 42, 57, 38, 29, 69}, {44, 49, 48, 83, 40, 56, 63, 31, 60},
            {40, 42, 76, 0, 33, 54, 17, 64, 3}, {51, 44, 63, 51, 47, 70, 63, 31, 57}, {51, 44, 60, 54, 45, 63, 31, 28, 65},
            {56, 47, 50, 38, 39, 33, 38, 49, 51}, {0, 0, 36, 0, 0, 37, 37, 32, 65}, {42, 45, 55, 54, 4, 34, 51, 39, 89},
            {58, 17, 51, 74, 71, 3, 60, 11, 57}, {2, 76, 7, 33, 30, 45, 55, 31, 59},
    };

    public static String rank(BufferedImage img) {
        int[] actual = rankCellCounts(shrink(img.getSubimage(img.getWidth() / 10, 0, img.getWidth() / 2, img.getHeight() / 3), bgCGetter.apply(img)), sCGetter.apply(img));
        String out = "U";
        for (int r = 0, minDiff = Integer.MAX_VALUE, accDiff = 0; r < rankPatterns.length; r++, accDiff = 0)
            for (int i = 0; i < actual.length; i++) {
                accDiff += abs(actual[i] - rankPatterns[r][i]);
                if (accDiff > 100) break;
                if (i == actual.length - 1 && accDiff < minDiff) {
                    minDiff = accDiff;
                    out = ranks[r];
                }
            }
        return out;
    }

    protected static int[] rankCellCounts(final BufferedImage imgR, final int suitColor) {
        int cdiv = 3, cellWidth = imgR.getWidth() / cdiv, cellHeight = imgR.getHeight() / cdiv, size = cdiv * cdiv;
        int[] actual = new int[size];
        for (int xc = 0; xc < cdiv; xc++)
            for (int yc = 0, count = 0, index = xc; yc < cdiv; yc++, count = 0, index = yc * cdiv + xc) {
                for (int x = 0; x < cellWidth; x++)
                    for (int y = 0; y < cellHeight; y++, count++) {
                        actual[index] += diff.apply(imgR.getRGB(xc * cellWidth + x, yc * cellHeight + y), suitColor) < 30 ? 1 : 0;
                    }
                actual[index] = (int) (((double) actual[index] / count) * 100);
            }
        return actual;
    }

    public static String suit(BufferedImage img) {
        int suitColor = sCGetter.apply(img), bgColor = bgCGetter.apply(img);
        BufferedImage imgS = shrink(img.getSubimage(img.getWidth() / 3 + 3, img.getHeight() - 32,
                img.getWidth() - img.getWidth() / 3 - 3, 32), bgColor);
        int c = imgS.getRGB(imgS.getWidth() / 2, imgS.getHeight() / 2), suitColorCount = 0;
        for (int x = 0; x < imgS.getWidth(); x++) suitColorCount += imgS.getRGB(x, 2) == suitColor ? 1 : 0;
        return (((c >> 16) & 0xFF) < 40) ? (suitColorCount < 5 ? "s" : "c") : (suitColorCount < 5 ? "d" : "h");
    }

    protected static String cardValues(String f) {
        try {
            return cutCards(ImageIO.read(Paths.get(f).toFile())).stream().map(img -> rank(img) + suit(img)).collect(Collectors.joining());
        } catch (IOException e) {
            throw new UncheckedIOException("Error on reading '"+f+"'",e);
        }
    }

    public static BufferedImage shrink(BufferedImage img, int bgColor) {
        int d = 50, leftX = -1, rightX = -1, bottomY = -1, topY = -1, zMax = max(img.getWidth(), img.getHeight());
        for (int z = 0; z < zMax; z++)
            for (int t = 0; t < zMax; t++) {
                if (t < img.getWidth()) {
                    if (topY < 0 && diff.apply(img.getRGB(t, z), bgColor) > d) topY = z; // from top
                    if (bottomY < 0 && diff.apply(img.getRGB(t, img.getHeight() - z - 1), bgColor) > d)
                        bottomY = img.getHeight() - z - 1;  // from bottom
                }
                if (t < img.getHeight()) {
                    if (leftX < 0 && diff.apply(img.getRGB(z, t), bgColor) > d) leftX = z;  // from left
                    if (rightX < 0 && diff.apply(img.getRGB(img.getWidth() - z - 1, t), bgColor) > d)
                        rightX = img.getWidth() - z - 1; // from right
                }
            }
        return img.getSubimage(leftX, topY, rightX - leftX, bottomY - topY);
    }

    protected static List<BufferedImage> cutCards(BufferedImage img) {
        List<BufferedImage> list = new ArrayList<>(5);
        int top = 122 * img.getHeight() / 240, height = 137 * img.getHeight() / 240 - top;
        int left = 18 * img.getWidth() / 80, right = 62 * img.getWidth() / 80;
        int wcard = 36 * (right - left) / 200, wgap = 6 * (right - left) / 200, yWhite = top + 24;
        for (int x = left, newWidth = 0, leftShift = -1; x <= right; x += wcard + wgap, newWidth = 0, leftShift = -1)
            for (int i = 0; i < wcard; i++) {
                int c = img.getRGB(x + i, yWhite);
                if (c == -1 || (((c >> 16) & 0xFF) == 120 && ((c >> 8) & 0xFF) == 120 && (c & 0xFF) == 120)) {
                    leftShift = leftShift < 0 ? i : leftShift;
                    newWidth = i - leftShift;
                }
                if (i == wcard - 1 && leftShift > -1) {
                    list.add(img.getSubimage(x + leftShift, top, newWidth, height));
                }
            }
        return list;
    }

    public static void main(String[] args) {
        assert args.length>0:"provide path to dir";
        Arrays.stream(Objects.requireNonNull(Paths.get(args[0]).toFile().listFiles(), "provide path to dir")).forEach(f ->
                System.out.println(f + " " + cardValues(f.getAbsolutePath())));
    }
}
