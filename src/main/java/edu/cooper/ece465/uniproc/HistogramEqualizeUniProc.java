package edu.cooper.ece465.uniproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

import edu.cooper.ece465.common.Utils;

// hist equalize code from https://github.com/SestoAle/Parallel-Histogram-Equalization
public class HistogramEqualizeUniProc {
    static int resize_scale = 8;
    static String file_name = "dark";
    BufferedImage img = null;
    public HistogramEqualizeUniProc() {}

    public void process() throws java.io.IOException {
        //Load the image
        System.out.println("Loading Image..");
        String pwd = System.getenv("PWD");
        File theFile = new File(pwd +"/images/" + file_name + ".jpg");
        System.out.println("input file = " + theFile.toString());
        img = ImageIO.read(theFile);
        //img = resize(img, img.getWidth()*2, img.getHeight()*2);

        //Display the image
        ImageIcon icon = new ImageIcon(Utils.resize(img, img.getWidth() / resize_scale, img.getHeight() / resize_scale));
        JLabel label = new JLabel(icon, JLabel.CENTER);
        JOptionPane.showMessageDialog(null, label, "Image not Equalized", -1);

        System.out.println("Start processing..");
        long startTime = System.currentTimeMillis();

        int[] histogram = new int[256];

        //Convert the image form RGB to YCbCr
        Utils.RGB_to_YBR(img, histogram);

        long time_1 = System.currentTimeMillis();
        System.out.println("First op done in: " + (time_1 - startTime) + " msec");

        int width = img.getWidth();
        int height = img.getHeight();
        int[] histogram_equalized = new int[256];

        int sum = 0;

        //Equalized the histogram
        for (int i = 0; i < histogram.length; i++) {

            sum += histogram[i];
            histogram_equalized[i] = (int) ((((float) (sum - histogram[0])) / ((float) (width * height - 1))) * 255);

        }

        long time_2 = System.currentTimeMillis();
        System.out.println("Second op done in: " + (time_2 - time_1) + " msec");

        //Map the new value of the Y channel and convert the image from YCbCr to RGB
        Utils.YBR_to_RGB(img, histogram_equalized);

        long endTime = System.currentTimeMillis();
        System.out.println("Third op done in: " + (endTime - time_2) + " msec");
        long totalTime = endTime - startTime;
        System.out.println("Total time: " + totalTime);

        System.out.println("Showing the equalized image..");
        //Display the image
        icon = new ImageIcon(Utils.resize(img, img.getWidth() / resize_scale, img.getHeight() / resize_scale));
        label = new JLabel(icon, JLabel.CENTER);
        JOptionPane.showMessageDialog(null, label, "Image Equalized", -1);

        System.out.println("Saving the image..");
        //Save output image
        File output_file = new File(pwd + "/images/" + file_name + "_equalized_seq.jpg");
        ImageIO.write(img, "jpg", output_file);
        System.out.println("Image saved!");
    }

    public static void main(String[] args) {
        HistogramEqualizeUniProc heUni = new HistogramEqualizeUniProc();
        int exitCode = 0;

        try {
            heUni.process();
        } catch (java.io.IOException ex1) {
            ex1.printStackTrace(System.err);
            exitCode = -10;
        }

        System.exit(exitCode);
    }

}
