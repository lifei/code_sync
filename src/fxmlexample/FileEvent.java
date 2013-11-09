package fxmlexample;

public class FileEvent {
	
	static enum Type {

		MODIFY ("Modify"),
		CREATE ("Create"),
		RENAME ("Rename");
		
		private final String str;
		
		private Type(String str) {
			this.str = str;
		}
		
		public String toString(){
			return str;
		}
		
	}
	
	public Type eventType;
	public String filename;
	
	public FileEvent(Type eventType, String filename){
		this.eventType = eventType;
		this.filename = filename;
	}

	@Override
	public int hashCode() {
		return filename.hashCode() + eventType.hashCode();
	}

	@Override
	public String toString() {
		return "event: " + eventType + " file:" + filename;
	}
	
	
	
}
