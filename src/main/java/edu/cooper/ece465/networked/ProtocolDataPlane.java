package edu.cooper.ece465.networked;

public class ProtocolDataPlane {
    private static final int WAITING = 0;
    private static final int RECEIVING = 1;
    private static final int PROCESSING = 2;
    private static final int SENDING = 3;


    public ProtocolDataPlane() {}

    public String processInput() {
        String theOutput = null;
        return theOutput;
    }
}
/*
Purpose: a service that receives image files in the format of JPG and returns the image histogram equalized.
Constraints:
- do not store images after processed
- if busy tell user to come back later
Steps
0. wait for a request
1. (get image) (a) receive request of image size and bytestream of image; (b) generate unique name and save locally
2. (like original image proc code before) Convert the image form RGB to YCbCr
3. (like original image proc code before) Calculate histogram
4. (like original image proc code before) create CDF
5. (like original image proc code before) equalize
6. return file

 */

