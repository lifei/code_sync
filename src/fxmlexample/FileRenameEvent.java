package fxmlexample;

public class FileRenameEvent extends FileEvent {

	public String newName;
	
	public FileRenameEvent(String oldName, String newName) {
		super(FileEvent.Type.RENAME, oldName);
		this.newName = newName;
	}

}
