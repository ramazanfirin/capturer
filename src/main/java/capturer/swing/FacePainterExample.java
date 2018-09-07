package capturer.swing;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.shape.Rectangle;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.common.io.Files;
import com.innovatrics.iface.Face;
import com.innovatrics.iface.FaceBasicInfo;
import com.innovatrics.iface.FaceHandler;
import com.innovatrics.iface.IFace;
import com.innovatrics.iface.IFaceException;
import com.innovatrics.iface.enums.AgeGenderSpeedAccuracyMode;
import com.innovatrics.iface.enums.FaceAttributeId;
import com.innovatrics.iface.enums.FacedetSpeedAccuracyMode;
import com.innovatrics.iface.enums.Parameter;


/**
 * Paint troll smile on all detected faces.
 * 
 * @author Bartosz Firyn (SarXos)
 */
public class FacePainterExample extends JFrame implements Runnable, WebcamPanel.Painter {

	private static final long serialVersionUID = 1L;

	private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
	private static final HaarCascadeDetector detector = new HaarCascadeDetector();
	private static final Stroke STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] { 1.0f }, 0.0f);

	private Webcam webcam = null;
	private WebcamPanel.Painter painter = null;
	//private List<DetectedFace> faces = null;
	//private BufferedImage troll = null;

	IFace iface= null;
	FaceHandler faceHandler = null;
	
	Face[] faces;
	
	public int minEyeDistance = 30;
    public int maxEyeDistance = 3000;
	
	public void startIface() throws IOException {
		iface = IFace.getInstance();
		
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("iengine.lic").getFile());
		 byte[] bytesArray = new byte[(int) file.length()]; 

		  FileInputStream fis = new FileInputStream(file);
		  fis.read(bytesArray); //read file into bytes[]
		  fis.close();
		iface.initWithLicence(bytesArray);
		
//		ClassPathResource cpr = new ClassPathResource("iengine.lic");
//		byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
//		iface.initWithLicence(bdata);
		
		faceHandler = new FaceHandler();
		faceHandler.setParam(Parameter.FACEDET_SPEED_ACCURACY_MODE, FacedetSpeedAccuracyMode.FAST.toString());
		faceHandler.setParam(Parameter.AGEGENDER_SPEED_ACCURACY_MODE, AgeGenderSpeedAccuracyMode.FAST.toString());
		//         

	}
	
	private byte[] convertToByteArray(BufferedImage originalImage) throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write( originalImage, "png", baos );
		baos.flush();
		byte[] imageInByte = baos.toByteArray();
		baos.close();
		return imageInByte;
	}
	
	private BufferedImage resize(BufferedImage img, int height, int width) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
	}
	
	public FacePainterExample() throws IOException {
		
		
		

		super();
		startIface();

//		troll = ImageIO.read(getClass().getResourceAsStream("/troll-face.png"));

		webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open(true);

		WebcamPanel panel = new WebcamPanel(webcam, false);
		panel.setPreferredSize(WebcamResolution.VGA.getSize());
		panel.setPainter(this);
		panel.setFPSDisplayed(true);
		panel.setFPSLimited(true);
		panel.setFPSLimit(20);
		panel.setPainter(this);
		panel.start();

		painter = panel.getDefaultPainter();

		add(panel);

		setTitle("Face Detector Example");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		EXECUTOR.execute(this);
	}

	@Override
	public void run() {
		while (true) {
			if (!webcam.isOpen()) {
				return;
			}
			BufferedImage image  = webcam.getImage();
			//ImageIO.write(image, "PNG", new File("/home/ramazan/webcamtest/phone"+System.currentTimeMillis()+".png"));
			image = resize(image,960,1280);
//			log.info("capture compledted");
			try {
				faces = faceHandler.detectFaces(convertToByteArray(image), minEyeDistance, maxEyeDistance, 3);
				if(faces.length==0){
					System.out.println("No Face Detected");
				}
			} catch (IFaceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//faces = detector.detectFaces(ImageUtilities.createFImage(webcam.getImage()));
		}
	}

	@Override
	public void paintPanel(WebcamPanel panel, Graphics2D g2) {
		if (painter != null) {
			painter.paintPanel(panel, g2);
		}
	}

	@Override
	public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {

		if (painter != null) {
			painter.paintImage(panel, image, g2);
		}

		if (faces == null) {
			return;
		}

//		Iterator<DetectedFace> dfi = faces.iterator();
//		while (dfi.hasNext()) {
//
//			DetectedFace face = dfi.next();
//			Rectangle bounds = face.getBounds();
//
//			int dx = (int) (0.1 * bounds.width);
//			int dy = (int) (0.2 * bounds.height);
//			int x = (int) bounds.x - dx;
//			int y = (int) bounds.y - dy;
//			int w = (int) bounds.width + 2 * dx;
//			int h = (int) bounds.height + dy;
//
//			//g2.drawImage(troll, x, y, w, h, null);
//			g2.drawString("Age:15", x, y);
//			g2.drawString("Gender:Male", x+50, y);
//			
//			g2.setStroke(STROKE);
//			g2.setColor(Color.RED);
//			g2.drawRect(x, y, w, h);
//		}
		
		
		for (int i = 0; i < faces.length; i++) {
			Face face = faces[i];
			Float age = face.getAttribute(FaceAttributeId.AGE);
	        Float gender = face.getAttribute(FaceAttributeId.GENDER);
	        String gendervalue = gender<0?"MALE":"FEMALE";
	        
	        
	        FaceBasicInfo bi = face.getBasicInfo();
	        g2.drawString("Face_:"+i,0, i*100+50);
	        g2.drawString("Age:"+age,0, i*100+70);
			g2.drawString("Gender:"+gendervalue,0, i*100+90);
			
		}
	}

	public static void main(String[] args) throws IOException {
		new FacePainterExample();
	}
}

