import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.io.File;
import java.util.Date;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;

public class Main {

    public static FFmpegFrameRecorder recorder;
    public static String VideoFormat = "mp4";
    public static int VideoFrameRate = 5;
    public static int Bitrate = 9000;
    public static int videoCodec = avcodec.AV_CODEC_ID_MPEG4;
    public static int videoQuality = 0;
    public static int videoWidth = 1920;
    public static int videoHeight = 1080;
    public static int counter;
    public static File videoDir = new File(System.getProperty("user.home"),"drillox.dat");
    public static String imgDir = new File(System.getProperty("user.home"),"img").getPath()+File.separator;
    public static JFrame frame = null;
    public static Thread thread_Scp;
    public static boolean videoCompleted = false;
    public static boolean startedRecording = false;
    public static boolean locked = false;
    public static CanvasFrame canvasFrame = new CanvasFrame("Drillox Recorder");
    public static OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
    public static String vdfl = "";

    public static void main(String [] args) throws FrameGrabber.Exception {

        /*
        *
        * So let me try out something even faster here in this workspace of mine
        * */

        try{
            vdfl = JOptionPane.showInputDialog("Eneter The name of the recorded file");
            initGrabber();
            if (getRecorder() == null){
                System.out.println("Could not make the recorder object. Stopping the program");
                System.exit(0);
            }
            if (getRobot() == null){
                System.out.println("Could not make the robot object. Stopping the program");
                System.exit(0);
            }
            File Imgsfolder = new File(imgDir);
            Imgsfolder.delete();
            Imgsfolder.mkdirs();
            createGUI();
        }catch (Exception e){
            System.out.println("Execption in running the program");
            //System.exit(0);
        }
    }

    public static void initGrabber() throws FrameGrabber.Exception {
        canvasFrame.setLayout(new FlowLayout());
        canvasFrame.setBackground(Color.BLACK);
        canvasFrame.setAlwaysOnTop(true);
        canvasFrame.setSize(400,400);

        grabber.start();
        new Thread(){
            public void run(){
                while (true){
                    //OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
                    try {
                        Frame frame = grabber.grab();
                        //canvasFrame.setSize(frame.imageWidth,frame.imageHeight+1
                        //frame.imageWidth = 400;
                        frame.imageHeight = 300;
                        canvasFrame.showImage(frame);
                    } catch (FrameGrabber.Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
    public static FFmpegFrameRecorder getRecorder() throws Exception {
        if (recorder!=null)
            return recorder;
        else {
            recorder = new FFmpegFrameRecorder(new File(videoDir,vdfl),videoWidth,videoHeight);
            try{
                recorder.setFrameRate(VideoFrameRate);
                recorder.setVideoCodec(videoCodec);
                recorder.setVideoBitrate(Bitrate);
                recorder.setFormat(VideoFormat);
                recorder.setVideoQuality(videoQuality);
                recorder.setAudioChannels(1);
                recorder.setAudioQuality(0);
                recorder.start();
                return recorder;
            }catch (Exception e){
                JOptionPane.showMessageDialog(frame,e.getMessage());
                throw new Exception("issues when initializing recorder");
            }
        }
    }
    public static Robot getRobot() throws Exception {
        Robot robot = null;
        try{
            robot = new Robot();
            return robot;
        }catch (Exception e){
            JOptionPane.showMessageDialog(frame,e.getMessage());
            throw new Exception("Issue initializing the robot object");
        }
    }
    public static void createGUI(){
        frame = new JFrame("Drillox Screen Recorder");

        JButton btn_lock = new JButton("lock");
        JButton btn_start = new JButton("Start");
        JButton btn_stop = new JButton("Stop");
        JLabel label = new JLabel("Full Screen will be selected by default if no region is selected");
        //label.setFont();

        btn_lock.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (!locked){
                    canvasFrame.setResizable(false);
                    btn_lock.setText("Unlock");
                    locked = true;
                }
                else {
                    canvasFrame.setResizable(true);
                    btn_lock.setText("lock");
                    locked = false;
                }
            }
        });

        btn_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                counter = 0;
                if (!startedRecording){
                    startRecording();
                    startedRecording = true;
                }
            }
        });

        btn_stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                stopRecording();
                System.out.println("Exiting ......");
                System.exit(0);
            }
        });


        canvasFrame.add(btn_start);
        canvasFrame.add(btn_stop);
        canvasFrame.add(btn_lock);
        canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        /*
        * some hello world trials here and there all along the way of my code
        * but just that am not getting the audio or sound of the recording
        *

        frame.setBackground(Color.BLUE);
        frame.add(btn_sel);
        frame.add(btn_start);
        frame.add(btn_stop);
        frame.add(label);
        frame.setLayout(new FlowLayout(0));
        frame.setVisible(true);
        frame.setResizable(false);
        frame.setSize(1000,70);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);*/
    }

    public static void startRecording(){
        thread_Scp = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    takeScreenShot(getRobot());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(frame,"issues starting the scp thread "+e.getMessage());
                    System.exit(0);
                }
            }
        });
        thread_Scp.start();
        System.out.println("Started recording at : "+new Date());
    }
    public static void takeScreenShot(Robot _r){
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle rec = new Rectangle(size);

        File scanFolder = new File(imgDir);
        while (!videoCompleted){
            counter++;
            BufferedImage img = _r.createScreenCapture(rec);
            try {
                ImageIO.write(img,"png",new File(imgDir+counter+"."+"png"));
            }catch (Exception e){
                JOptionPane.showMessageDialog(frame,"Got issues when writting img "+e.getMessage());
                counter--;
            }

            File [] inputFiles = scanFolder.listFiles();
            for (int i=0; i<inputFiles.length; i++){
                addImageToVideo(inputFiles[i]);
                inputFiles[i].delete();
            }

        }
        File [] inputFiles = scanFolder.listFiles();
        for (int i=0; i<inputFiles.length; i++){
            addImageToVideo(inputFiles[i]);
            inputFiles[i].delete();
        }
    }
    public static void addImageToVideo(File fl){
        try {
            getRecorder().record(getFrameConverter().convert(cvLoadImage(fl.getAbsolutePath())));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame,"Excepton while adding image to video: "+e.getMessage());
            System.out.println("Excepton while adding image to video: "+e.getMessage());
        }
    }
    public static OpenCVFrameConverter.ToIplImage getFrameConverter(){
        OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
        return grabberConverter;
    }
    public static void stopRecording(){
        try{
            videoCompleted = true;
            System.out.println("Stopping recording At: "+new Date());
            thread_Scp.join();
            System.out.println("Screen shot thread completed");
            getRecorder().stop();
            System.out.println("Recording has stopped");
            JOptionPane.showMessageDialog(frame,"Recording has been Sucessfully Saved at: ");

        }catch (Exception e){
            System.out.println("Exception While dtopping the recorder: "+e.getMessage());
        }
    }

}
