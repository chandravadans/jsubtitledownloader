
public class Movie {
	
	String title;
	String hash;
	String subLanguage;
	String imdbID;
	String path;
	
	public String getPath(){
		return path;
	}
	
	public void setPath(String path){
		this.path=path;
	}
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	public String getSubLanguage() {
		return subLanguage;
	}
	public void setSubLanguage(String subLanguage) {
		this.subLanguage = subLanguage;
	}
	public String getImdbID() {
		return imdbID;
	}
	public void setImdbID(String imdbID) {
		this.imdbID = imdbID;
	}
	public Integer getSize() {
		return size;
	}
	public void setSize(Integer size) {
		this.size = size;
	}
	Integer size;
	
	

}
