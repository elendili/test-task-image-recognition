package imr;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class CardTest {
    @BeforeEach
    public void setup() {
        Arrays.stream(Objects.requireNonNull(Paths.get("tmp/").toFile().listFiles())).forEach(File::delete);
    }

    @Test
    void cutCards() {
        String name = "10h10s7d5h";
        List<BufferedImage> s = Card.cutCards(parse("imgs/" + name + ".png"));
        for (int i = 0; i < s.size(); i++) {
            save(s.get(i), name.substring(i * 2, i * 2 + 2) + ".png");
        }
    }

    @Test
    void recognizeRanks() {
        Arrays.stream(Objects.requireNonNull(Paths.get("cards/").toFile().list())).forEach(name -> {
            String expected = name.replaceAll("\\..*png", "").replaceAll(".$", "");
            assertEquals(expected, Card.rank(parse("cards/" + name)));
        });
    }

    @Test
    void recognizeRank() {
        BufferedImage img = parse("cards/5h.1.png");
        assertEquals("5", Card.rank(img));
    }

    @Test
    void recognizeFullCards() {
        AtomicInteger count = new AtomicInteger();
        Arrays.stream(Objects.requireNonNull(Paths.get("imgs/").toFile().list()))
                .forEach(name -> {
                    assertEquals(
                            name.replaceAll(".png", ""),
//                            name.replaceAll("\\..*$", ""),
                            Card.cardValues("imgs/" + name));
                    System.out.println(count.incrementAndGet() + ". passed for " + name);
                });
    }

    @Test
    void recognizeCard() {
        String name = "10h10s7d5h";
        assertEquals(name, Card.cardValues("imgs/" + name + ".png"));
    }


    @Test
    void cellCount() {
        cellCountTestHelper("2c", "2d", "2h", "2s");
        cellCountTestHelper("3c", "3d", "3h", "3s");
        cellCountTestHelper("4c", "4d", "4h", "4s");
        cellCountTestHelper("5c", "5d", "5h", "5s");
        cellCountTestHelper("6c", "6d", "6h", "6s");
        cellCountTestHelper("7c", "7d", "7h", "7s");
        cellCountTestHelper("8c", "8d", "8h", "8s");
        cellCountTestHelper("9c", "9d", "9h", "9s");
        cellCountTestHelper("10c", "10d", "10h", "10s");
        cellCountTestHelper("Jc", "Jd", "Jh", "Js");
        cellCountTestHelper("Qc", "Qd", "Qh", "Qs");
        cellCountTestHelper("Kc", "Kd", "Kh", "Ks");
        cellCountTestHelper("Ac", "Ad", "Ah", "As");
    }

    @Test
    void patternsGeneratorTest() {
        for (String r : Card.ranks) {
            int[] averages = new int[20];
            int size = 0;
            for (String s : asList("c", "d", "h", "s")) {
                String fileName = r + s;
                BufferedImage img = parse("cards/" + fileName + ".png");
                int[] counts = Card.rankCellCounts(img, Card.sCGetter.apply(img));
                for (int i = 0; i < counts.length; i++) {
                    averages[i] += counts[i];
                }
                size = counts.length;
                String rank = Card.rank(img);
                Assertions.assertEquals(r, rank);
//                    System.out.println(fileName + ": " + Arrays.stream(counts).boxed()
//                            .collect(Collectors.toList()) +", => "+rank);
            }
            averages = Arrays.copyOfRange(averages, 0, size);
            for (int i = 0; i < size; i++) {
                averages[i] /= 4;
            }
            System.out.println(Arrays.stream(averages).mapToObj(i -> "" + i).collect(Collectors.joining(",", "{", "},")));
        }
    }

    @Test
    void Jd() {
        cellCountTestHelper("Jd");
    }

    void cellCountTestHelper(String... fileNames) {
        int[] averages = new int[20];
        int size = 0;
        int wrongGuesses = 0;
        for (String fileName : fileNames) {
            BufferedImage img = parse("cards/" + fileName + ".png");
            int[] counts = Card.rankCellCounts(img, Card.sCGetter.apply(img));
            for (int i = 0; i < counts.length; i++) {
                averages[i] += counts[i];
            }
            size = counts.length;
            String rank = Card.rank(img);
            if (!fileName.startsWith(rank)) {
                wrongGuesses++;
                rank += "  <-";
            }
            System.out.println(fileName + ": " + Arrays.stream(counts).boxed()
                    .collect(Collectors.toList()) + ", => " + rank);
        }
        averages = Arrays.copyOfRange(averages, 0, size);
        for (int i = 0; i < size; i++) {
            averages[i] /= fileNames.length;
        }
        System.out.println("av: " + Arrays.stream(averages).boxed()
                .collect(Collectors.toList()) + ", bad: " + wrongGuesses);
    }

    @Test
    void recognizeSuits() {
        Arrays.stream(Objects.requireNonNull(Paths.get("cards/").toFile().list())).forEach(name -> {
                    String s = name.replaceAll("\\..*png", "");
                    s = "" + s.charAt(s.length() - 1);
                    assertEquals(s, Card.suit(parse("cards/" + name)), "error in " + name);
                    System.out.println("passed for " + name);
                }
        );
    }

    public static void cross(BufferedImage img, int x, int y) {
        for (int i = img.getHeight() - 1; i > -1; i--) {
            img.setRGB(x, i, Color.RED.getRGB());
        }
        for (int i = img.getWidth() - 1; i > -1; i--) {
            img.setRGB(i, y, Color.RED.getRGB());
        }
    }

    public static void save(List<BufferedImage> imgs, String pref) {
        for (int i = 0; i < imgs.size(); i++) {
            save(imgs.get(i), pref + "_" + i);
        }
    }

    public static void save(BufferedImage img, Object name) {
        try {
            ImageIO.write(img, "png", Paths.get("tmp/" + name).toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static BufferedImage parse(String s) {
        try {
            return ImageIO.read(Paths.get(s).toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}