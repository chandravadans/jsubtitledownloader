import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;


public class OpenSubtitleDownloader {

	String serverUrl="http://api.opensubtitles.org/xml-rpc";
	String userAgent="droidcvs_opensubtitlesagent";
	String userName="";
	String password="";
	String language="en";
	String token=null;
	XmlRpcClientConfigImpl config=null;
	XmlRpcClient client=null;
	boolean moreToDownload=true;
	Movie thisMovie=null;
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

	public void downloadSubtitle(File file) throws NumberFormatException, IOException{

		//Step 1. Do login and get token
		String token=this.doLogin();
		if(token==null){
			System.out.println("Login failed! Exiting..");
			doLogout(token);System.exit(1);
		}

		//Step 2. Compute file hash
		Hasher h=new Hasher();
		String fileHash=null;
		try {
			fileHash=h.computeHashForOsub(file);
		} catch (IOException e) {
			System.out.println("Error Calculating hash! Exiting..");
			e.printStackTrace();
			doLogout(token);
			doLogout(token);System.exit(1);
		}

		thisMovie=new Movie();
		thisMovie.setTitle(file.getAbsolutePath());
		thisMovie.setHash(fileHash);
		thisMovie.setSize((int) file.length());
		thisMovie.setSubLanguage("en");

		//Step 3. Search for subtitles and get a list of possible subtitles
		ArrayList<Subtitle> possible=new ArrayList<Subtitle>();
		possible=searchForSubtitles(thisMovie);

		if(possible.size()<1){
			System.out.println("Exact match not found. Doing a name search!");

			possible=searchForSubtitlesByName();

		}

		if(possible.size()<1){
			System.out.println("Sorry, try somewhere else");
			doLogout(token);
			System.exit(0);
		}

		//Step 4. Choose and keep downloading
		selectAndDownload(possible);

		while(moreToDownload){

			System.out.println("Doing a namesearch.");
			possible=searchForSubtitlesByName();
			if(possible.size()<1){
				System.out.println("Sorry, try somewhere else");
				doLogout(token);
				System.exit(0);
			}
			else
				selectAndDownload(possible);
		}

		//Finally, logout
		doLogout(token);
	}

	String doLogin(){

		config=new XmlRpcClientConfigImpl();
		try {
			config.setServerURL(new URL(serverUrl));
		} catch (MalformedURLException e) {

			System.out.println("Malformed RPC URL! Exiting..");
			e.printStackTrace();
			doLogout(token);System.exit(1);
		}

		client=new XmlRpcClient();
		client.setConfig(config);
		Object[] params={userName,password,language,userAgent};
		HashMap<?, ?> result=null;
		try {
			result=(HashMap<?,?>)client.execute("LogIn",params);
		} catch (XmlRpcException e) {

			System.out.println("Exception in executing RPC call for login! Exiting..");
			e.printStackTrace();
			doLogout(token);System.exit(1);
		}
		token=(String)result.get("token");
		double time=(double)result.get("seconds");
		System.out.println("Took "+time+" sec to login with status "+result.get("status"));
		return token;
	}

	void doLogout(String token){

		Object params[]={token};
		HashMap<?,?> result=null;
		try {
			result=(HashMap<?, ?>)client.execute("LogOut",params);
		} catch (XmlRpcException e) {

			System.out.println("Error while logging out! Exiting..");
			e.printStackTrace();
			doLogout(token);System.exit(1);
		}
		System.out.println("Logged out with status "+result.get("status")+" in "+result.get("seconds")+"s");
	}

	void displayPossibleSubs(ArrayList<Subtitle> subs){

		if(subs.isEmpty())
			return;
		int idx=0;
		for(Subtitle sub : subs){
			System.out.println("["+idx+"]\t"+sub.subFileName);
			idx++;
		}

	}

	void selectAndDownload(ArrayList<Subtitle> subs) throws NumberFormatException, IOException{

		boolean oneMore=true;
		while(oneMore){

			displayPossibleSubs(subs);
			System.out.println("Select index of sub to download: ");
			Integer idx=Integer.parseInt(in.readLine());//in.nextInt();
			downloadSub(subs.get(idx));
			subs.remove(subs.get(idx));
			if(!subs.isEmpty()){
				System.out.println("Test the sub out, I'll wait. If it doesn't work, we could download another one :)");
				System.out.println("If everything's fine, press y");
				if(in.readLine().equalsIgnoreCase("y")){
					oneMore=false;
					moreToDownload=false;
				}
			}
			else{
				System.out.println("Those were all of the exact matches.Satisfied? (y/n)");
				oneMore=false;
				String resp=in.readLine();
				if(resp.equalsIgnoreCase("y")){

					moreToDownload=false;
				}
			}
		}
	}

	void downloadSub(Subtitle sub){

		String Url=sub.getSubDownloadLink();
		String gzipFileName=thisMovie.getTitle()+".gz";
		try{
			URL website = new URL(Url);
			ReadableByteChannel rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(gzipFileName);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}

		String srtFileName=thisMovie.getTitle().substring(0, thisMovie.getTitle().length() - 4)+".srt";
		decompressGzip(gzipFileName, srtFileName);
		System.out.println("Done. Downloaded "+sub.getSubFileName());
	}

	void decompressGzip(String gzipFile, String newFile) {
		try {
			FileInputStream fis = new FileInputStream(gzipFile);
			GZIPInputStream gis = new GZIPInputStream(fis);
			FileOutputStream fos = new FileOutputStream(newFile);
			byte[] buffer = new byte[1024];
			int len;
			while((len = gis.read(buffer)) != -1){
				fos.write(buffer, 0, len);
			}
			fos.close();
			gis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		new File(gzipFile).delete();
	}

	ArrayList<Subtitle> searchForSubtitles(Movie movie){

		ArrayList<Subtitle> results=new ArrayList<Subtitle>();

		Map<String,Object> subParams=new HashMap<String,Object>();
		subParams.put("moviehash", movie.getHash());
		subParams.put("moviebytesize", movie.getSize());
		subParams.put("sublanguageid", "eng");

		Map<String,Object> subLimit=new HashMap<String,Object>();
		subLimit.put("limit", 10);

		Object[] args=new Object[]{token,new Object[]{subParams}};
		HashMap<?, ?> result=null;
		try {
			result = (HashMap<?, ?>) client.execute("SearchSubtitles", args);
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Object[]subs=(Object[])result.get("data");
		System.out.println(subs.length+" Subtitles found!");
		for(int i=0;i<subs.length;i++){

			HashMap<?, ?> res = (HashMap<?, ?>) subs[i];
			Subtitle tmp=new Subtitle();
			tmp.setSubAddDate((String)res.get("SubAddDate"));
			tmp.setSubAuthorComment((String)res.get("SubAuthorComment"));
			tmp.setSubDownloadLink((String)res.get("SubDownloadLink"));
			tmp.setSubFileName((String)res.get("SubFileName"));
			results.add(tmp);
		}

		return results;
	}

	ArrayList<Subtitle> searchForSubtitlesByName(){

		ArrayList<Subtitle> results=new ArrayList<Subtitle>();
		try{

			//Scanner in=new Scanner(System.in);
			int tvshow=1;
			String name;
			String season;
			String episode;

			Map<String,Object> subParams=new HashMap<String,Object>();

			System.out.println("Is this a tv show(1) or a movie[0]? [1]");
			tvshow=Integer.parseInt(in.readLine());
			if(tvshow==1){
				System.out.println("Enter show name : ");
				name=in.readLine();
				System.out.println("Enter Season: ");
				season=in.readLine();
				System.out.println("Enter Episode: ");
				episode=in.readLine();

				subParams.put("query", name);
				subParams.put("season", season);
				subParams.put("episode", episode);
				subParams.put("sublanguageid", "eng");
			}
			else{
				System.out.println("Enter movie name: ");
				name=in.readLine();
				subParams.put("query", name);
				subParams.put("sublanguageid", "eng");
			}

			Map<String,Object> subLimit=new HashMap<String,Object>();
			subLimit.put("limit", 10);

			Object[] args=new Object[]{token,new Object[]{subParams}};
			HashMap<?, ?> result = (HashMap<?, ?>) client.execute("SearchSubtitles", args);
			Object[]subs=(Object[])result.get("data");
			System.out.println(subs.length+"Subtitles found!");
			for(int i=0;i<subs.length;i++){

				HashMap<?, ?> res = (HashMap<?, ?>) subs[i];
				Subtitle tmp=new Subtitle();
				tmp.setSubAddDate((String)res.get("SubAddDate"));
				tmp.setSubAuthorComment((String)res.get("SubAuthorComment"));
				tmp.setSubDownloadLink((String)res.get("SubDownloadLink"));
				tmp.setSubFileName((String)res.get("SubFileName"));
				results.add(tmp);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return results;
	}
}