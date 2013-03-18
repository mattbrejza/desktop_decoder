package com.brejza.decoder.rtty_desktop;

import rtty.Waterfall;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.JTextArea;

import rtty.rtty_receiver;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;



public class Main extends JFrame {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//List<Mixer.Info> usableMixers;
	String[] mixerscmbo;
	
	TargetDataLine targetDataLine;
	boolean stopCapture = false;
	boolean restartCapture = false;
	
	rtty_receiver rcv = new rtty_receiver();
	
	Waterfall wf;
	AudioFormat af;

	
	//decoder settings
	int baud = 50;
	
	//ui elements
	JButton btnBaud;
	private JPanel frame;
	JComboBox<Object> cbAudio;
	JTextArea txtRxChars;
	JLabel lbStatus;
	JLabel lbWaterfall;
	private JButton btnStop;
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Main window = new Main();
					window.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Main() {
		initialize();
		startAudio();	
	}
	
	public boolean startAudio()
	{
		//start audio
		if (mixerscmbo != null)
		{
			if (mixerscmbo.length > 0 && cbAudio.getSelectedIndex() < mixerscmbo.length){
				Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();  
		        int i=0;
				while ( i < mixerInfo.length)
				{
					if (mixerInfo[i].getName().equals(mixerscmbo[cbAudio.getSelectedIndex()]))
					{
						return startAudio(mixerInfo[i]);
					}
					i++;
				}				
			}
		}
				
		return false;
	}
	
	public boolean startAudio(Mixer.Info mixerinf)
	{
		try
		{
			float sampleRate = 8000.0F;
			//8000,11025,16000,22050,44100
			int sampleSizeInBits = 16;
			//8,16
			int channels = 1;
			//1,2
			boolean signed = true;
			//true,false
			boolean bigEndian = true;
			//true,false
			af =  new AudioFormat(sampleRate,sampleSizeInBits,channels,signed,bigEndian);
			
			
			DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, af);
			
			Mixer mixer = AudioSystem.getMixer(mixerinf);
			targetDataLine = (TargetDataLine) mixer.getLine(dataLineInfo);
	
			targetDataLine.open(af);
			targetDataLine.start();
			
			 
			Thread captureThread = new CaptureThread();
			captureThread.start();
		}
		catch (Exception e) {
			 System.out.println(e);
		}  
		
		
		return false;
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 546, 628);
		frame = new JPanel();
		frame.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(frame);
		frame.setLayout(null);
		
		
		
		JLabel lblNewLabel = new JLabel("Audio Input");
		lblNewLabel.setBounds(10, 14, 63, 14);
		frame.add(lblNewLabel);
		
		btnBaud = new JButton("50");
		btnBaud.setBounds(194, 33, 57, 23);
		btnBaud.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {

					if (baud == 50)
					{
						baud = 300;
						btnBaud.setText("300");
					}
					else
					{
						baud = 50;
						btnBaud.setText("50");
					}

				}
			});
		frame.add(btnBaud);
		
		JLabel lblBaud = new JLabel("Baud");
		lblBaud.setBounds(194, 14, 46, 14);
		frame.add(lblBaud);
		
		lbWaterfall = new JLabel("New label");
		lbWaterfall.setBounds(10, 313, 510, 200);
		frame.add(lbWaterfall);
		
		JTextPane txtpnBlaa = new JTextPane();
		txtpnBlaa.setText("blaa");
		txtpnBlaa.setBounds(10, 65, 510, 113);
		frame.add(txtpnBlaa);
		
		txtRxChars = new JTextArea();
		txtRxChars.setText("c");
		txtRxChars.setBounds(10, 189, 510, 113);
		frame.add(txtRxChars);
		
		
		
		try
		{
			 Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();  
	          
	        //Output Available Mixers  
	        System.out.println("Available mixers:");  
	        List<String> ldevices = new ArrayList<String>();
	       // usableMixers = new ArrayList<Mixer.Info>();
	        for(int cnt = 0; cnt < mixerInfo.length; cnt++)
	        {  
	        	Mixer thisMixer = AudioSystem.getMixer(mixerInfo[cnt]);
	        	for (Line.Info lineInfo:thisMixer.getTargetLineInfo())
	        	{
	        		if (lineInfo.getLineClass().getName().equals("javax.sound.sampled.TargetDataLine"))
	        		{
	        			ldevices.add(mixerInfo[cnt].getName());
	        			//usableMixers.add(mixerInfo[cnt]);
	        			System.out.println(cnt + ": " + mixerInfo[cnt].getName());  
	        		}
	        	}	          	
	        }  	
	        String[] devices = ldevices.toArray(new String[ldevices.size()]);
	        mixerscmbo = devices;
          	cbAudio = new JComboBox<Object>(devices);
          	cbAudio.setBounds(10, 34, 174, 20);
          	cbAudio.addActionListener (new ActionListener () {
          	    public void actionPerformed(ActionEvent e) {
          	    	if(targetDataLine.isActive()){
          	    		restartCapture = true;
          	    		//Timer timer = new Timer("RestartAudio");    
          	    		//TaskStartAudio t = new TaskStartAudio();
          	    		//timer.schedule(t, 600);
          	    	}
          	    	else
          	    		startAudio();
          	    }
          	});
  			frame.add(cbAudio);
  			
  			lbStatus = new JLabel("Idle");
  			lbStatus.setBounds(259, 37, 108, 14);
  			frame.add(lbStatus);
  			
  			JLabel lblStatus2 = new JLabel("Status");
  			lblStatus2.setBounds(257, 14, 46, 14);
  			frame.add(lblStatus2);
  			
  			JButton btnStart = new JButton("Start");
  			btnStart.addActionListener(new ActionListener() {
  				public void actionPerformed(ActionEvent arg0) {
  					/*if (mixerscmbo != null)
  					{
  						if (mixerscmbo.length > 0 && cbAudio.getSelectedIndex() < mixerscmbo.length){
  							Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();  
  					        int i=0;
  					        boolean found = false;
  							while ( i < mixerInfo.length && !found)
  							{
  								if (mixerInfo[i].getName().equals(mixerscmbo[cbAudio.getSelectedIndex()]))
  								{
  									startAudio(mixerInfo[i]);
  									found = true;
  								}
  								i++;
  							}
  						}
  					}*/
  					startAudio();
  				}
  			});
  			
  			btnStart.setBounds(322, 10, 89, 23);
  			frame.add(btnStart);
  			
  			btnStop = new JButton("Stop");
  			btnStop.addActionListener(new ActionListener() {
  				public void actionPerformed(ActionEvent e) {
  					stopCapture = true;
  					//targetDataLine.close();
  				}
  			});
  			btnStop.setBounds(421, 10, 89, 23);
  			frame.add(btnStop);
  			
  			try
  			{
  			
	  			BufferedImage grad;
				
				grad = ImageIO.read(new File("C:/grad.png"));
				wf = new Waterfall(grad,200);
  			}
  			catch (Exception e)
  			{
  				
  			}
  		
		}
		catch (Exception e)
		{
			
		}
		
	}
	
	
	
	class CaptureThread extends Thread
	{

		byte tempBuffer[] = new byte[4096];
		//ByteArrayOutputStream byteArrayOutputStream;
		public void run(){
			//byteArrayOutputStream = new ByteArrayOutputStream();
			stopCapture = false;
			restartCapture = false;
			try
			{	
				while(!stopCapture && !restartCapture){

					int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
					if(cnt > 0){

						//byteArrayOutputStream.write(tempBuffer, 0, cnt);

						double a[] = bytes2double(tempBuffer);
	
						//txtDecode.append(rcv.processBlock(a,300));

						txtRxChars.append(rcv.processBlock(a,50));
							 
						
						  
						if (rcv.get_fft_updated())
						{
						 
						//	scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());//getHeight());
							lbStatus.setText(rcv.current_state.toString());
						}
						if (rcv.get_fft_updated())
							lbWaterfall.setIcon(new ImageIcon(wf.UpdateLine(rcv.get_fft())));
						
						// plotint = plot.addLinePlot("my plot", c);
					}//end if
				}//end while
				targetDataLine.close();
				if (restartCapture)
				{
					startAudio();
				}
			}
			catch (Exception e) 
			{
				System.out.println(e);
				System.exit(0);
			}
		}
	}

	private double[] bytes2double(byte[] in)
	{		
	    double[] out = new double[in.length / 2];
	    ByteBuffer bb = ByteBuffer.wrap(in);
	    for (int i = 0; i < out.length; i++) {
	        out[i] = (double)bb.getShort();
	    }
	    return out;
	}
	/*
	class TaskStartAudio extends TimerTask {
	   public void run() {
		   startAudio();

	    }
	} */
}
