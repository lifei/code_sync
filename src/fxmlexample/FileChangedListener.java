package fxmlexample;

import net.contentobjects.jnotify.JNotifyListener;

public class FileChangedListener implements JNotifyListener {

	private final String[] includes;
	private final String[] excludes;
	private final FileTransferThread t;

	public FileChangedListener(final String[] includes,
			final String[] excludes, final FileTransferThread t) {

		this.includes = includes;
		this.excludes = excludes;
		this.t = t;
	}

	public FileChangedListener(final FileTransferThread t) {

		this.includes = new String[0];
		this.excludes = new String[0];
		this.t = t;
	}

	private boolean checkFile(final String file) {

		// 至少满足其一
		boolean m = false;
		boolean match = false;
		for (final String inc : this.includes) {
			if (inc == null || inc.length() == 0) {
				continue;
			}
			m = true;
			if (file.contains(inc)) {
				match = true;
			}
		}

		// 有得玩没玩上才出局
		if (m && !match) {
			return false;
		}

		for (final String exc : this.excludes) {
			if (exc == null || exc.length() == 0) {
				continue;
			}
			if (file.contains(exc)) {
				return false;
			}
		}

		return true;
	}

	private void addFile(final String file) {
		if (!this.checkFile(file)) {
			return;
		}

		this.t.addFile(file);
	}

	@Override
	public void fileCreated(final int arg0, final String arg1, final String arg2) {
		this.addFile(arg2);
	}

	@Override
	public void fileDeleted(final int arg0, final String arg1, final String arg2) {

	}

	@Override
	public void fileModified(final int arg0, final String arg1,
			final String arg2) {
		this.addFile(arg2);

	}

	@Override
	public void fileRenamed(final int arg0, final String arg1,
			final String arg2, final String arg3) {
		this.addFile(arg2);
	}

}
