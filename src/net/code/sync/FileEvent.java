package net.code.sync;

public class FileEvent {
	
	static enum Type {

		MODIFY ("Modify"),
		CREATE ("Create"),
		RENAME ("Rename"),
		DELETE ("Delete");
		
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
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof FileEvent)) return false;
		return this.eventType == ((FileEvent)o).eventType  && this.filename == ((FileEvent)o).filename;
	}

	@Override
	public String toString() {
		return "event: " + eventType + " file:" + filename;
	}
	
	
	
}
