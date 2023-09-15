package edu.cooper.ece465.common;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class Utils {
    // from https://github.com/SestoAle/Parallel-Histogram-Equalization/blob/master/histogram%20equalization%20Java/src/main.java
    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, TYPE_INT_RGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    // from https://github.com/SestoAle/Parallel-Histogram-Equalization/blob/master/histogram%20equalization%20Java/src/main.java
    public static void RGB_to_YBR(BufferedImage image, int[] histogram){

        Raster raster = image.getRaster();

        for (int y = 0; y < image.getHeight(); y++) {
            int[] iarray = new int[image.getWidth()*3];

            raster.getPixels(0, y, image.getWidth(), 1, iarray);

            for (int x = 0; x < image.getWidth()*3; x+=3) {

                int r = iarray[x+0];
                int g = iarray[x+1];
                int b = iarray[x+2];

                int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int Cb = (int) (128 - 0.168736 * r - 0.331264 * g + 0.5 * b);
                int Cr = (int) (128 + 0.5 * r - 0.418688 * g - 0.081312 * b);


                iarray[x+0] = Y;
                iarray[x+1] = Cb;
                iarray[x+2] = Cr;

                histogram[Y] ++;
            }

            image.getRaster().setPixels(0, y, image.getWidth(), 1, iarray);
        }
    }

    public static int[] RGB_to_YBR(BufferedImage sub_image) {

        int width = sub_image.getWidth();
        int height = sub_image.getHeight();
        int[] histogram = new int[256];
        int[] iarray = new int[width*3];

        for (int y = 0; y < height; y++) {

            sub_image.getRaster().getPixels(0, y, width, 1, iarray);

            for (int x = 0; x < width*3; x+=3) {

                int r = iarray[x+0];
                int g = iarray[x+1];
                int b = iarray[x+2];

                int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                int Cb = (int) (128 - 0.168736 * r - 0.331264 * g + 0.5 * b);
                int Cr = (int) (128 + 0.5 * r - 0.418688 * g - 0.081312 * b);

                iarray[x+0] = Y;
                iarray[x+1] = Cb;
                iarray[x+2] = Cr;

                histogram[Y]++;

            }
            sub_image.getRaster().setPixels(0, y, width, 1, iarray);
        }

        return histogram;
    }

    // from https://github.com/SestoAle/Parallel-Histogram-Equalization/blob/master/histogram%20equalization%20Java/src/main.java
    public static void YBR_to_RGB(BufferedImage image, int[] histogram_equalized){

        Raster raster = image.getRaster();

        for (int y = 0; y < image.getHeight(); y ++) {
            int[] iarray = new int[image.getWidth()*3];

            raster.getPixels(0, y, image.getWidth(), 1, iarray);

            for (int x = 0; x < image.getWidth()*3; x+=3) {

                int valueBefore = iarray[x];
                int valueAfter = histogram_equalized[valueBefore];

                iarray[x] = valueAfter;

                int Y = iarray[x+0];
                int cb = iarray[x+1];
                int cr = iarray[x+2];

                int R = Math.max(0, Math.min(255, (int) (Y + 1.402 * (cr - 128))));
                int G = Math.max(0, Math.min(255, (int) (Y - 0.344136 * (cb - 128) - 0.714136 * (cr - 128))));
                int B = Math.max(0, Math.min(255, (int) (Y + 1.772 * (cb - 128))));

                iarray[x+0] = R;
                iarray[x+1] = G;
                iarray[x+2] = B;
            }
            image.getRaster().setPixels(0,y, image.getWidth(), 1,  iarray);
        }
    }

    // from https://github.com/SestoAle/Parallel-Histogram-Equalization/blob/master/histogram%20equalization%20Java/src/main_parallel.java
    public static int find_closest_int(int n, int m){

        if(n % m != 0){
            return find_closest_int(n,m-1);
        } else {
            return m;
        }
    }
}
