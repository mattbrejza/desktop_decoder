package com.brejza.decoder.rtty_desktop;

import graphics.Waterfall;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.msgpack.MessagePack;

import rtty.StringRxEvent;
import rtty.fsk_receiver;
import ukhas.Gps_coordinate;
import ukhas.Habitat_interface;
import ukhas.Listener;
import ukhas.Telemetry_string;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;



public class Main extends JFrame implements StringRxEvent {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//List<Mixer.Info> usableMixers;
	String[] mixerscmbo;
	
	TargetDataLine targetDataLine;
	boolean stopCapture = false;
	boolean restartCapture = false;
	
	fsk_receiver rcv = new fsk_receiver();
	
	Waterfall wf;
	AudioFormat af;
	
	Habitat_interface hi;
	Listener li;
	
	private String _habitat_url = "habitat.habhub.org";
	private String _habitat_db = "habitat";

	
	//decoder settings
	int baud = 50;
	
	
	
	//ui elements
	JButton btnBaud;
	JButton btnModulation;
	JButton btnEncoding;
	JButton btnUpdateUsr;
	private JPanel frame;
	JComboBox<Object> cbAudio;
	JTextArea txtRxChars;
	JLabel lbStatus;
	JLabel lbWaterfall;
	JLabel lbEnc;
	JLabel lbMod;
	JScrollPane scTxtRxChars;
	JScrollPane scTxtRxSent;
	JTextArea txtpnBlaa;
	private JButton btnStop;
	
	private JTextField txtcall = new JTextField();
	private JTextField txtLat;
	private JTextField txtLong;
	private JTextField txtRad;
	private JTextField txtAnt;
	
	
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
		btnBaud.setBounds(194, 33, 70, 23);
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
		
		rcv.addStringRecievedListener(this);
		
		btnEncoding = new JButton("RTTY");
		btnEncoding.setBounds(270, 33, 70, 23);
		btnEncoding.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {

					if (rcv.current_mode == fsk_receiver.Mode.RTTY)
					{
						rcv.setMode(fsk_receiver.Mode.BINARY);
						btnEncoding.setText("BIN");
					}
					else
					{
						rcv.setMode(fsk_receiver.Mode.RTTY);
						btnEncoding.setText("RTTY");
					}

				}
			});
		frame.add(btnEncoding);
		
		btnModulation = new JButton("FSK");
		btnModulation.setBounds(350, 33, 70, 23);
		btnModulation.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {

					if (rcv.current_modulation == fsk_receiver.Modulation.FSK)
					{
						rcv.setModulation(fsk_receiver.Modulation.AFSK);
						btnModulation.setText("AFSK");
						rcv.setFreq(500,750);
					}
					else
					{
						rcv.setModulation(fsk_receiver.Modulation.FSK);
						btnModulation.setText("FSK");
					}

				}
			});
		frame.add(btnModulation);
		
		lbEnc = new JLabel("Encoding");
		lbEnc.setBounds(270, 14, 100, 14);
		frame.add(lbEnc);
		
		lbMod = new JLabel("Modulation");
		lbMod.setBounds(350, 14, 100, 14);
		frame.add(lbMod);
		
		JLabel lblBaud = new JLabel("Baud");
		lblBaud.setBounds(194, 14, 100, 14);
		frame.add(lblBaud);
		
		lbWaterfall = new JLabel("New label");
		lbWaterfall.setBounds(10, 373, 510, 200);
		frame.add(lbWaterfall);
		
		txtpnBlaa = new JTextArea();
		txtpnBlaa.setText("");
		txtpnBlaa.setBounds(10, 125, 510, 113);
		frame.add(txtpnBlaa);
		
		scTxtRxSent = new JScrollPane(txtpnBlaa);
		scTxtRxSent.setBounds(10, 125, 510, 113);
		frame.add(scTxtRxSent);
		
		txtRxChars = new JTextArea();
		txtRxChars.setText("");
		txtRxChars.setBounds(10, 249, 510, 113);
		txtRxChars.setLineWrap(true);
		txtRxChars.setAutoscrolls(true);
		
		scTxtRxChars = new JScrollPane(txtRxChars);
		scTxtRxChars.setBounds(10, 249, 510, 113);
		frame.add(scTxtRxChars);
		
		
		
		txtcall.setText("CALL");	
		
		txtcall.setBounds(64, 68, 70, 20);
		frame.add(txtcall);
		txtcall.setColumns(10);
		
		JLabel lblCallsign = new JLabel("Callsign");
		lblCallsign.setBounds(10, 70, 100, 14);
		frame.add(lblCallsign);
		
		JLabel lblpos = new JLabel("Position");
		lblpos.setBounds(150, 70, 100, 14);
		frame.add(lblpos);
		
		txtLat = new JTextField();
		txtLat.setText("Lat");
		txtLat.setHorizontalAlignment(SwingConstants.LEFT);
		txtLat.setBounds(200, 68, 50, 20);
		frame.add(txtLat);
		txtLat.setColumns(10);
		
		txtLong = new JTextField();
		txtLong.setText("Long");
		txtLong.setBounds(254, 68, 50, 20);
		frame.add(txtLong);
		txtLong.setColumns(10);
		
		
		JLabel lblrad = new JLabel("Radio");
		lblrad.setBounds(10, 100, 100, 14);
		frame.add(lblrad);
		
		txtRad = new JTextField();
		txtRad.setText("");
		txtRad.setHorizontalAlignment(SwingConstants.LEFT);
		txtRad.setBounds(50, 98, 200, 20);
		frame.add(txtRad);
		txtRad.setColumns(20);
		
		JLabel lblant = new JLabel("Antenna");
		lblant.setBounds(265, 100, 100, 14);
		frame.add(lblant);
		
		txtAnt = new JTextField();
		txtAnt.setText("");
		txtAnt.setHorizontalAlignment(SwingConstants.LEFT);
		txtAnt.setBounds(320, 98, 200, 20);
		frame.add(txtAnt);
		txtAnt.setColumns(20);

		
			
		DocumentListener du = new DocumentListener() {			
			public void changedUpdate(DocumentEvent arg0) {
				btnUpdateUsr.setEnabled(true);
			}
			public void insertUpdate(DocumentEvent arg0) {
				btnUpdateUsr.setEnabled(true);
			}
			public void removeUpdate(DocumentEvent arg0) {
				btnUpdateUsr.setEnabled(true);
			}			
		};
		
		txtAnt.getDocument().addDocumentListener(du);
		txtRad.getDocument().addDocumentListener(du);
		txtcall.getDocument().addDocumentListener(du);
		txtLat.getDocument().addDocumentListener(du);
		txtLong.getDocument().addDocumentListener(du);
		
		li = new Listener(txtcall.getText(), new Gps_coordinate(txtLat.getText(),txtLong.getText(),""),false);
		
		btnUpdateUsr = new JButton("Update");
		btnUpdateUsr.setBounds(400, 68, 118, 23);
		btnUpdateUsr.setEnabled(false);
		btnUpdateUsr.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {

					try{
						Gps_coordinate g = new Gps_coordinate(Double.parseDouble(txtLat.getText()),
								Double.parseDouble(txtLong.getText()),0);
						li.set_Gps_coordinates(g);
					}catch (Exception e)
					{
						
					}					
					if (!li.CallSign().equals(txtcall.getText()) )					
						li.SetCallSign(txtcall.getText());
					if (!li.getAntenna().equals(txtAnt.getText()))
						li.setAntenna(txtAnt.getText());
					if (!li.getRadio().equals(txtRad.getText()))
						li.setRadio(txtRad.getText());	
					//TODO: radio
					
					btnUpdateUsr.setEnabled(false);
						
				}
			});
		frame.add(btnUpdateUsr);
		
		
		
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
  			lbStatus.setBounds(440, 37, 108, 14);
  			frame.add(lbStatus);
  			
  			JLabel lblStatus2 = new JLabel("Status");
  			lblStatus2.setBounds(440, 14, 100, 14);
  			frame.add(lblStatus2);
  			
  			/*
  			JButton btnStart = new JButton("Start");
  			btnStart.addActionListener(new ActionListener() {
  				public void actionPerformed(ActionEvent arg0) {  					
  					startAudio();
  				}
  			});
  			
  			btnStart.setBounds(421, 10, 89, 23); //322
  			frame.add(btnStart);
  			
  			btnStop = new JButton("Stop");
  			btnStop.addActionListener(new ActionListener() {
  				public void actionPerformed(ActionEvent e) {
  					stopCapture = true;
  					//targetDataLine.close();
  				}
  			});
  			btnStop.setBounds(421, 35, 89, 23);
  			frame.add(btnStop);
  			*/
  			
  			hi = new Habitat_interface(_habitat_url, _habitat_db, li);
  			
  			try
  			{
  			
	  			BufferedImage grad;
	  
	  			grad = ImageIO.read(this.getClass().getClassLoader().getResource("resources/grad.png"));
				//grad = ImageIO.getr   //(new File("C:/grad.png"));
				wf = new Waterfall(grad,200);
  			}
  			catch (Exception e)
  			{
  				System.out.println(e);
				//System.exit(0);
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

						txtRxChars.append(rcv.processBlock(a,baud));
							 
						
						  
						if (rcv.get_fft_updated())
						{
						 
							scTxtRxChars.getVerticalScrollBar().setValue(scTxtRxChars.getVerticalScrollBar().getMaximum());//getHeight());
							lbStatus.setText(rcv.current_state.toString());
						}
						if (rcv.get_fft_updated())
							lbWaterfall.setIcon(new ImageIcon(wf.UpdateLine(rcv.get_fft(),(int)(rcv.get_f1()*1024),(int)(rcv.get_f2()*1024))));
						
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

	public void StringRx(String strrx, boolean checksum) {
		// TODO Auto-generated method stub
		if (checksum){
			
				
			
			
			scTxtRxSent.getVerticalScrollBar().setValue(scTxtRxSent.getVerticalScrollBar().getMaximum());//getHeight());
			
			Telemetry_string ts = new Telemetry_string(strrx,null);
			txtpnBlaa.append(ts.getSentence());
			
			
			hi.upload_payload_telem(ts);
		}
	}

	public void StringRx(byte[] strrx, boolean checksum, int length, int flags, int fixed) {
		// TODO Auto-generated method stub
		if (checksum){
			
			for (int i = 0; i < strrx.length; i++)			
				txtpnBlaa.append(" " + toHexString(((int)strrx[i])&0xFF));
			txtpnBlaa.append("\n");
			scTxtRxSent.getVerticalScrollBar().setValue(scTxtRxSent.getVerticalScrollBar().getMaximum());//getHeight());
			
			Telemetry_string ts = new Telemetry_string(strrx,null);
			
			ts.habitat_metadata = new HashMap<String,String>();
			ts.habitat_metadata.put("receiver_flags", Integer.toHexString(flags));
			ts.habitat_metadata.put("fec_fixed", Integer.toString(fixed));
			
			byte [][] a = Telemetry_string.gen_telem_mask(strrx);
			rcv.provide_binary_sync_helper(a[0],a[1],ts.callsign.toUpperCase(Locale.US), length);
			
			txtpnBlaa.append(ts.getSentence());
			
			hi.upload_payload_telem(ts);
		}
	}
	
	private static String toHexString(int input)
	{
		String output = "";
		output = Integer.toHexString(input);
		if (output.length() == 1)
			output = "0"+output;
		return output;
	}
}
