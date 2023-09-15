package edu.cooper.ece465.multiproc;

import edu.cooper.ece465.common.Utils;

import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;

public class mtRGB_to_YCbCr implements Callable<int[]> {
    final private BufferedImage img;

    mtRGB_to_YCbCr(BufferedImage sub_img){
        this.img = sub_img;
    }

    public int[] call(){
        return Utils.RGB_to_YBR(this.img);
    }


}
