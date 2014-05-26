package net.code.sync;

public class FileRenameEvent extends FileEvent {

	public String newName;
	
	public FileRenameEvent(String oldName, String newName) {
		super(FileEvent.Type.RENAME, oldName);
		this.newName = newName;
	}

	@Override
	public int hashCode() {
		return filename.hashCode() + eventType.hashCode() + this.newName.hashCode();
	}
	
	@Override
	public String toString() {
		return "Rename:" + filename + newName;
	}

}
