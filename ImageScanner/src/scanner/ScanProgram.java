package scanner;

import java.awt.EventQueue;
import javax.swing.JFrame;
import java.util.stream.*;

import eu.gnome.morena.Camera;
import eu.gnome.morena.Device;
import eu.gnome.morena.Manager;
import eu.gnome.morena.Scanner;
import eu.gnome.morena.TransferDoneListener;
import eu.gnome.morena.TransferListener;
import eu.gnome.morena.wia.WIAScanner;
import ij.ImagePlus;
import ij.gui.Line;
import ij.io.Opener;
import ij.process.*;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.awt.event.ActionEvent;
import javax.swing.JProgressBar;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JSeparator;
import javax.swing.JCheckBox;
import java.awt.Font;
import java.awt.Image;

import javax.swing.SwingConstants;
import javax.swing.border.Border;

import java.awt.Color;

public class ScanProgram {
	
	Path fileDestination;
	Path outputPath;
	boolean askSaveLoca = true;
	boolean numberFiles = true;
	boolean addCropBoo = true;
	int fileNum = 0;
	String filePrefix = "";
	String fileSuffix = "";
	String fileName;
	int autoScanDelay =1;
	int imgLocaOnBed = 0;
	int lineThickness = 3;
	int addCrop = 10;
	
	Scanner scanner;
	
	BufferedImage currentImage;
	BufferedImage croppedImage;
	BufferedImage uiImage;
	int w=0;
	int h=0;
	int cropX=0;
	int cropY=0;
	
	JLabel currentImageDisplay = new JLabel("Image Placeholder");
	JButton saveBut = new JButton("Save");
	JButton editCropBut = new JButton("Edit Crop");
	JCheckBox stopAutoScan = new JCheckBox("Stop Auto Scan");




	private JFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ScanProgram window = new ScanProgram();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 * @throws Exception 
	 */
	public ScanProgram() throws Exception {
		initialize();
		scannerSetup();
	}

	/**
	 * Initialize the contents of the frame.
	 * @throws Exception 
	 */
	private void initialize() throws Exception {
		
		frame = new JFrame();
		frame.setBounds(100, 100, 1538, 835);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JButton startScanBut = new JButton("Start scan");
		startScanBut.setFont(new Font("Tahoma", Font.PLAIN, 20));
		startScanBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
					try {
						scanImage();
						processImage();
						drawLines();
						cropImage();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
		});
		startScanBut.setBounds(107, 54, 198, 102);
		frame.getContentPane().add(startScanBut);
		
		stopAutoScan.setFont(new Font("Tahoma", Font.PLAIN, 20));
		stopAutoScan.setBounds(107, 218, 175, 73);
		frame.getContentPane().add(stopAutoScan);
		
		Border border = BorderFactory.createLineBorder(Color.BLACK);
		if (currentImageDisplay.getIcon() == null) {
			currentImageDisplay.setEnabled(false);
		}
		else {
			currentImageDisplay.setEnabled(true);
		}
		currentImageDisplay.setBorder(border);
		currentImageDisplay.setBackground(new Color(192, 192, 192));
		currentImageDisplay.setHorizontalAlignment(SwingConstants.CENTER);
		currentImageDisplay.setFont(new Font("Tahoma", Font.PLAIN, 20));
		currentImageDisplay.setBounds(425, 34, 509, 701);
		frame.getContentPane().add(currentImageDisplay);
		
		if (currentImageDisplay.isEnabled()) {
			saveBut.setEnabled(true);
		}
		else{
			saveBut.setEnabled(false);
		}
		
		saveBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					saveFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		saveBut.setFont(new Font("Tahoma", Font.PLAIN, 20));
		saveBut.setBounds(1096, 54, 198, 102);
		frame.getContentPane().add(saveBut);
		
		if (currentImageDisplay.isEnabled()) {
			editCropBut.setEnabled(true);
		}
		else{
			editCropBut.setEnabled(false);
		}
		editCropBut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String inputx = JOptionPane.showInputDialog("X value, current X value:"+cropX);
				try {
					int newX = Integer.parseInt(inputx);
					cropX=newX;
				}
				catch (Exception e1) {
					JOptionPane.showMessageDialog(frame, "ERROR: Must be an integer");
					return;
				}
				
				String inputy = JOptionPane.showInputDialog("Y value, current Y value:"+cropY);
				try {
					int newY = Integer.parseInt(inputy);
					cropY=newY;
				}
				catch (Exception e1) {
					JOptionPane.showMessageDialog(frame, "ERROR: Must be an integer");
					return;
				}
				drawLines();
				cropImage();
				
			}
		});
		editCropBut.setFont(new Font("Tahoma", Font.PLAIN, 20));
		editCropBut.setBounds(1096, 218, 198, 102);
		frame.getContentPane().add(editCropBut);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem menuSaveScan = new JMenuItem("Save Scan");
		mnFile.add(menuSaveScan);
		
		final JCheckBoxMenuItem menuAutoScan = new JCheckBoxMenuItem("Auto Scan Mode");
		menuAutoScan.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(menuAutoScan.isSelected() & !stopAutoScan.isSelected() ) {
					try {
						autoScan();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});
		mnFile.add(menuAutoScan);
		
		JMenu mnSettings = new JMenu("Settings");
		menuBar.add(mnSettings);
		
		final JCheckBoxMenuItem menuAskSave = new JCheckBoxMenuItem("Ask Where to Save File");
		menuAskSave.setSelected(true);
		menuAskSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(menuAskSave.isSelected()) {
					askSaveLoca = true;
				}
				else {
					askSaveLoca = false;
				}
			}
		});
		mnSettings.add(menuAskSave);
		
		JMenuItem menuSaveLoc = new JMenuItem("Set Save Destination");
		menuSaveLoc.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				if (fileDestination != null) {
					fc.setCurrentDirectory(fileDestination.toFile());
				}
				fc.setDialogTitle("Save Location");
				fc.setApproveButtonText("Select");
				
				int returnVal = fc.showOpenDialog(frame);
				fileDestination = fc.getCurrentDirectory().toPath();
				System.out.println("file destination : "+fileDestination);
			}
		});
		
		final JCheckBoxMenuItem fileNumCheck = new JCheckBoxMenuItem("Add Numbering to File Name");
		fileNumCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(fileNumCheck.isSelected()) {
					numberFiles = true;
				}
				else {
					numberFiles = false;
				}
			}
		});
		fileNumCheck.setSelected(true);
		mnSettings.add(fileNumCheck);
		
		final JCheckBoxMenuItem chckbxmntmAdditionalCrop = new JCheckBoxMenuItem("Additional crop");
		chckbxmntmAdditionalCrop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addCropBoo = chckbxmntmAdditionalCrop.isSelected();
				if (currentImage != null) {
					cropImage();
				}
			}
		});
		
		JMenuItem mntmSetFileNumber = new JMenuItem("Set File Number");
		mntmSetFileNumber.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String input = JOptionPane.showInputDialog("File Number, Current Number:" + fileNum);
				try {
					if(input != null) {
						fileNum = Integer.parseInt(input);
					}
				}
				catch (Exception e1) {
					JOptionPane.showMessageDialog(frame, "ERROR: Must be an integer");
					return;
				}
			}
		});
		mnSettings.add(mntmSetFileNumber);
		chckbxmntmAdditionalCrop.setSelected(true);
		mnSettings.add(chckbxmntmAdditionalCrop);
		
		JMenuItem mntmSetAdditionalCrop = new JMenuItem("set additional crop value");
		mntmSetAdditionalCrop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String input = JOptionPane.showInputDialog("number of pixles to cut off right and bottom");
				try {
					addCrop = Integer.parseInt(input);
					
					if (currentImage != null) {
						cropImage();
					}
				}
				catch (Exception e1) {
					JOptionPane.showMessageDialog(frame, "ERROR: Must be an integer");
					return;
				}
			}
		});
		mnSettings.add(mntmSetAdditionalCrop);
		mnSettings.add(menuSaveLoc);
		
		JMenuItem menuNamePre = new JMenuItem("File Name Prefix");
		menuNamePre.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String input = JOptionPane.showInputDialog("File name prefix?");
				if (input!=null) {
					filePrefix=input;
				}
				System.out.println("prefix : " + filePrefix);
			}
		});
		mnSettings.add(menuNamePre);
		
		JMenuItem menuNamePost = new JMenuItem("File Name Suffix");
		menuNamePost.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String input = JOptionPane.showInputDialog("File name suffix?");
				if (input!=null) {
					fileSuffix=input;
				}
				System.out.println("suffix : " + fileSuffix);
			}
		});
		mnSettings.add(menuNamePost);
		
		JSeparator separator = new JSeparator();
		mnSettings.add(separator);
		
		JMenu menuAutoScanDelay = new JMenu("Auto Scan Delay");
		mnSettings.add(menuAutoScanDelay);
		
		JMenuItem menu1s = new JMenuItem("1 second");
		menu1s.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoScanDelay = 1;
			}
		});
		menuAutoScanDelay.add(menu1s);
		
		JMenuItem menu3s = new JMenuItem("3 seconds");
		menu3s.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoScanDelay = 3;
			}
		});
		menuAutoScanDelay.add(menu3s);
		
		JMenuItem menu5s = new JMenuItem("5 seconds");
		menu5s.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoScanDelay = 5;
			}
		});
		menuAutoScanDelay.add(menu5s);
		
		JMenuItem menu10s = new JMenuItem("10 seconds");
		menu10s.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoScanDelay = 10;
			}
		});
		menuAutoScanDelay.add(menu10s);
		
		JMenuItem menu15s = new JMenuItem("15 seconds");
		menu15s.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoScanDelay = 15;
			}
		});
		menuAutoScanDelay.add(menu15s);
		
		JMenuItem menu20s = new JMenuItem("20 seconds");
		menu20s.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				autoScanDelay = 20;
			}
		});
		menuAutoScanDelay.add(menu20s);
		
		JSeparator separator_2 = new JSeparator();
		mnSettings.add(separator_2);
		
		JMenu menuImageLoca = new JMenu("Image Location on Bed");
		mnSettings.add(menuImageLoca);
		
		JMenuItem menuTopLeft = new JMenuItem("Top Left");
		menuImageLoca.add(menuTopLeft);
		
		JMenuItem menuTopRight = new JMenuItem("Top Right");
		menuImageLoca.add(menuTopRight);
		
		JSeparator separator_1 = new JSeparator();
		menuImageLoca.add(separator_1);
		
		JMenuItem menuBottomLeft = new JMenuItem("Bottom Left");
		menuImageLoca.add(menuBottomLeft);
		
		JMenuItem menuBottomRight = new JMenuItem("Bottom Right");
		menuImageLoca.add(menuBottomRight);
		
		JMenuItem mntmUiLineThickness = new JMenuItem("UI Line Thickness");
		mnSettings.add(mntmUiLineThickness);
		
	}
	
	public void scannerSetup() {
		
		Manager manager = Manager.getInstance();
		List devices=manager.listDevices();
		
		Device device = (Device)devices.get(0);
		if (device instanceof Scanner) {
			scanner = (Scanner) device;
			
		scanner.setMode(Scanner.RGB_8);
		scanner.setResolution(300);
		scanner.setFrame(0, 0, 2544, 3504);
			
		}
		else {
			System.out.println("error, device is not a printer");
			return;
		}
	}
	
	public void scanImage() throws Exception {
		currentImage = SynchronousHelper.scanImage(scanner);
		uiImage =currentImage;
		
		w = currentImage.getWidth();
		h = currentImage.getHeight();
		
		System.out.println("w: "+w+"h: "+h);
		
		updateUiImage();

		saveBut.setEnabled(true);
		editCropBut.setEnabled(true);
	}
	
	public void processImage() {
		System.out.println("process image");
			
		ImagePlus imgPlus = new ImagePlus("",currentImage);
		ImageConverter convert = new ImageConverter(imgPlus);
	
		 convert.convertToGray16();
		 BufferedImage grayimg = imgPlus.getProcessor().getBufferedImage();
		 ImageProcessor processor = new ColorProcessor(grayimg);
		 
		 float[] pixelRow = new float [w];
		 float rowMean = 0;
		 float rowStddev=0;
		 
		 float[] pixelCol = new float [h];
		 float colMean=0;
		 float colStddev=0;
		 
		 //find y value (bottom of image)
		 for(int y=0;y<h;y++) {
			 for(int x=0;x<w;x++) {
				 pixelRow[x]=processor.getPixelValue(x, y);
			 }
			 //if row's average is higher than normal (is outside of image)break loop
			 rowMean = (sum(pixelRow)/pixelRow.length);
			 rowStddev = stddev(pixelRow,rowMean);
			 if (rowMean>180) {
				 if(rowStddev<10) {
					 cropY=y;
					 break;
				 }
			 }
		 }
		 
		 //find x value (side of image)
		 for(int x=0;x<w;x++) {
			 for (int y=0;y<h;y++) {
				 pixelCol[y]=processor.getPixelValue(x, y);
			 }
			 //if col's average is higher than normal (is outside of image)break loop
			 colMean= (sum(pixelCol)/pixelCol.length);
			 colStddev= stddev(pixelCol,colMean);
			 if (colMean>180) {
				 if(colStddev<10) {
					 cropX=x;
					 break;
				 }
			 }
		 }
		 
		 System.out.println("cropX:"+cropX+" cropY:"+cropY);

		 updateUiImage();
		 cropImage();
	}
	
	public void drawLines() {
		ImagePlus imp = new ImagePlus("",currentImage);
		
		ImageProcessor ip = imp.getProcessor();
		ip.setRoi(0,0,cropX,cropY);
		
		//draw x value (side of image)
		ip.setColor(Color.RED);
		ip.fillRect(cropX, 0, lineThickness, h);
		
		
		//draw y value (bottom of image)
		ip.setColor(Color.BLUE);
		ip.fillRect(0, cropY, w, lineThickness);
		
		BufferedImage markedImage = ip.getBufferedImage();
		uiImage = markedImage;
		updateUiImage();
	}
	
	public void cropImage() {
		ImagePlus imp = new ImagePlus("",currentImage);
		
		ImageProcessor ip = imp.getProcessor();
		ip.setRoi(0, 0, cropX, cropY);
		if (addCropBoo) {
			ip.setRoi(0, 0, cropX-addCrop, cropY-addCrop);
		}
		
		ip = ip.crop();
		BufferedImage cropImg=ip.getBufferedImage();
		
		croppedImage = cropImg;
		updateUiImage();
	}
	
	public void autoScan() throws InterruptedException {
		System.out.println("autoScan");
		
		
		if (stopAutoScan.isSelected()) {
			return;
		}
		else {
			autoScan();
		}
	}
	
	public void saveFile() throws IOException {
		
 		if (askSaveLoca) {
			
			String input = JOptionPane.showInputDialog("File name?");
			if (input!=null) {
				fileName=input;
			}
			
			final JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			if (fileDestination != null) {
			fc.setCurrentDirectory(fileDestination.toFile());
			}
			fc.setDialogTitle("Save Location");
			fc.setApproveButtonText("Select");
			
			int returnVal = fc.showOpenDialog(frame);
			if(returnVal == 0) {
				fileDestination = fc.getSelectedFile().toPath();
				String filePathString = fileDestination.toString();
				if (numberFiles) {
					filePathString = filePathString + "\\" + filePrefix + fileName + fileSuffix+", "+fileNum +".png";
				}
				else {
					filePathString = filePathString + "\\" + filePrefix + fileName + fileSuffix +".png";
				}
				outputPath = Paths.get(filePathString);
			}
			else {
				return;
			}
		}
		else if(fileDestination == null) {
			fileDestination = Paths.get(System.getProperty("user.dir"));
		}
		else if(numberFiles) {
			String filePathString = fileDestination.toString() + "\\" + filePrefix + fileName + fileSuffix+", "+fileNum +".png";
			outputPath = Paths.get(filePathString);			
		}
		else {
			String filePathString = fileDestination.toString() + "\\" + filePrefix + fileName + fileSuffix +".png";
			outputPath = Paths.get(filePathString);		
		}
		
		
		System.out.println("file destination"+fileDestination);
		System.out.println("output path" + outputPath);
		
		File outputFile = new File(outputPath.toString());
		ImageIO.write(croppedImage, "png", outputFile);
		if (numberFiles) {
			fileNum++;
		}
	}
	
	public void updateUiImage() {
		Image scaledImage = uiImage.getScaledInstance(509, 701, Image.SCALE_SMOOTH);
		
		currentImageDisplay.setIcon(new ImageIcon(scaledImage));
		currentImageDisplay.setEnabled(true);
	}
	
	public float sum(float[] array) {
		float sum =0;
		for(int i=0; i<array.length; i++) {
			sum += array[i];
		}
		
		return sum;
	}
	public float stddev(float[] array,float mean) {
		float stddev=0;
		float[] valuesSqrd = new float[array.length];
		
		for(int i=0; i<array.length; i++) {
			float value = array[i];
			float sqrdValue = (float) Math.pow((value-mean), 2);
			valuesSqrd[i] = sqrdValue;
		}
		float valuesSum = sum(valuesSqrd);
		float variance = valuesSum/array.length;
		stddev = (float) Math.sqrt(variance);
		
		return stddev;
	}
}


