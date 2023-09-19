package edu.cooper.ece465.multiproc;

import edu.cooper.ece465.common.Utils;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// hist equalize code from https://github.com/SestoAle/Parallel-Histogram-Equalization
public class HistogramEqualizeMultiProc {

    static int resize_scale = 8;
    static String file_name = "Sample-jpg-image-30mb"; //"dark"; //"webb"; //"space"; //"milk"; //"mayor"; //"dark";
    BufferedImage img = null;
    String pwd = System.getenv("PWD");

    public HistogramEqualizeMultiProc() {}

    protected void loadImage() throws IOException, InterruptedException, ExecutionException {
        System.out.println("Loading Image..");
        File theFile = new File(pwd +"/images/" + file_name + ".jpg");
        System.out.println("input file = " + theFile.toString());
        img = ImageIO.read(theFile);
        //img = resize(img, img.getWidth()*2, img.getHeight()*2);
    }

    protected void saveImage() throws IOException, SecurityException {
        String filename = pwd +"/images/" + file_name + "_equalized_thread.jpg";
        System.out.printf("Trying to write to file %s\n", filename);
        File output_file = new File(filename);
        if (output_file.createNewFile()) {
            System.out.println("File created: " + output_file.getName());
        } else {
            System.out.println("File already exists.");
        }
        ImageIO.write(img, "jpg", output_file);
        System.out.println("Image saved!");
    }

    protected void displayImage(String message) {
        ImageIcon icon = new ImageIcon(Utils.resize(img, img.getWidth()/resize_scale, img.getHeight()/resize_scale));
        JLabel label = new JLabel(icon, JLabel.CENTER);
        JOptionPane.showMessageDialog(null, label, message, -1);
    }

    public void process() throws IOException, InterruptedException, ExecutionException, SecurityException {
        //Load the image
        this.loadImage();

        //Display the original image
        displayImage("Image not Equalized");

        // Parallel processing of histogram equalization on one computer with multi core CPUs.

        System.out.println("Start threaded processing");
        long startTime = System.currentTimeMillis();

        int total_threads = Runtime.getRuntime().availableProcessors()/4;
        System.out.println("CPU Threads available = " + total_threads);

        // First pool: convert the image from RGB to YCbCr, with a ThreadPool with fixed size of total_thread.
        // Some callables are created, each converting and computing the histogram of a part of the image.
        // Take the result of each callable through Futures, and add together all the local histograms

        // Create a pool of threads to distribute the calculation
        ExecutorService es = Executors.newFixedThreadPool(total_threads);
        ArrayList<Future> futures = new ArrayList<>();

        // these values will be the result of stitching together the results from each thread in pool
        int[] histogram = new int[256];
        int[] histogram_equalized = new int[256];

        int starting_point = (int) Math.ceil((double) img.getHeight() / (double)total_threads);
        int sub_height = (int) Math.ceil((double) img.getHeight() / (double)total_threads);
        System.out.printf("height = %d\n",img.getHeight());
        System.out.printf("starting_point = %d\n",starting_point);
        System.out.printf("sub_height = %d\n",sub_height);

        // prepare each thread
        for (int i = 0; i < total_threads; i++) {
            // calc last sub_height if height not divisible wholly by total_threads
            if( (i == total_threads-1) && (img.getHeight() % total_threads != 0)) {
                sub_height = img.getHeight() - i * (int) Math.ceil((double) img.getHeight() / (double) total_threads);
            }
            int next_starting_point = i*starting_point;
            System.out.printf("next_starting_point = %d\n",next_starting_point);
            System.out.printf("sub_height = %d\n",sub_height);
            // create individual threads each with the workload you want it to produce
            futures.add(
                es.submit(
                    new mtRGB_to_YCbCr(
                        img.getSubimage(0, next_starting_point, (img.getWidth()), (sub_height))
                    )
                )
            );
        }

        // Wait for the pool to finish
        for(Future<int[]> future : futures){
            int[] local_histogram = future.get();
            for(int j = 0; j < 256; j++) {
                histogram[j] += local_histogram[j];
            }
        }

        long time_1 = System.currentTimeMillis();
        System.out.println("First pool ended in : " + (time_1 - startTime) + " msec");

        //Compute the cdf of the histogram
        int sum = 0;
        int[] cdf = new int[256];

        for(int i = 0; i < histogram.length; i++) {
            sum += histogram[i];
            cdf[i] = sum;
        }

        // Compute the equalized histogram dividing the histogram in total_threads part, each one
        // processed by a single thread
        int histogram_threads;

        // Find the closest number to total_thread that is multiple of 256
        histogram_threads = Utils.find_closest_int(256, total_threads);
        System.out.printf("histogram_threads = %d\n", histogram_threads);

        futures.clear();

        // This thread pool calculates the histogram_equalized array
        for(int i = 0; i < histogram_threads; i++){
            futures.add( es.submit(
                    new mtHistEqualizer(
                            i, histogram_threads, histogram, cdf, histogram_equalized, img.getWidth(), img.getHeight()
                    )
            ));
        }

        // loop through each thread result to successfully exit from each thread
        // note since histogram_equalized variable is in this code block, it's ready to be read after this loop
        for(Future future : futures){
            future.get();
        }

        long time_2 = System.currentTimeMillis();
        System.out.println("Second pool ended in : " + (time_2 - time_1) + " msec");

        // Third pool: map the new values of the Y channel and convert the image from YCbCr to RGB
        sub_height = (int) Math.ceil((double) img.getHeight() / (double) total_threads);
        futures.clear();

        // This thread pool applies the histogram_equalized to each pixel via each sub-image in a thread
        for (int i = 0; i < total_threads; i++){

            if(i == total_threads-1 && img.getHeight()%total_threads != 0){
                sub_height = img.getHeight() - i * (int) Math.ceil((double) img.getHeight() / (double) total_threads);
            }

            futures.add(es.submit(new mtYCbCr_to_RGB(
                    img.getSubimage(
                            0,
                            (i*starting_point),
                            (img.getWidth()),
                            (sub_height)),
                    histogram_equalized)));
        }

        // Shutdown the ExecutorService that provides the thread pool
        // no need to loop through each future, since pool is shutting down after successful exit of each thread.
        es.shutdown();
        es.awaitTermination(1, TimeUnit.MINUTES);

        long endTime   = System.currentTimeMillis();
        System.out.println("Third pool ended in: " + (endTime - time_2) + " msec");
        System.out.println("Total time in : " + (endTime - startTime) + " msec");

        System.out.println("Showing the equalized image..");

        //Display the newly histogram equalized image
        displayImage("Image Equalized");


        System.out.println("Saving the image..");
        // Save output image
        this.saveImage();
    }

    public static void main(String[] args) {
        HistogramEqualizeMultiProc heMulti = new HistogramEqualizeMultiProc();
        int exitCode = 0;

        try {
            heMulti.process();
        } catch (SecurityException ex0) {
            ex0.printStackTrace(System.err);
            exitCode = -5;
        } catch (IOException ex1) {
            ex1.printStackTrace(System.err);
            exitCode = -10;
        } catch (InterruptedException ex2) {
            ex2.printStackTrace(System.err);
            exitCode = -15;
        } catch (ExecutionException ex3) {
            ex3.printStackTrace(System.err);
            exitCode = -20;
        }
        System.exit(exitCode);
    }

}
