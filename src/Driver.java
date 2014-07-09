import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


public class Driver {

	public static void main(String[] args) throws NumberFormatException, IOException {
		
		System.out.println("Enter the file name: ");
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		String inp=in.readLine();
		in.close();
		OpenSubtitleDownloader osdl=new OpenSubtitleDownloader();
		File ipFile=new File(inp); 
		osdl.downloadSubtitle(ipFile);
	}

}
