import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JFileChooser;
import javax.swing.JFrame;


public class Driver {

	public static void main(String[] args) throws NumberFormatException, IOException {
		
		OpenSubtitleDownloader osdl=new OpenSubtitleDownloader();
		System.out.println("Enter the file name (or enter 'c' for GUI): ");
		
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		String inp=in.readLine();
		
		if(inp.equalsIgnoreCase("c")){
			JFrame frame=new JFrame();
			frame.setVisible(true);
			JFileChooser chooser = new JFileChooser();
			chooser.setMultiSelectionEnabled(true);
			chooser.showOpenDialog(frame);
			File[] files = chooser.getSelectedFiles();
			for(File f:files){
				osdl.downloadSubtitle(f);
			}
			frame.dispose();
			return;
		}
		File ipFile=new File(inp); 
		osdl.downloadSubtitle(ipFile);
		in.close();
	}

}
