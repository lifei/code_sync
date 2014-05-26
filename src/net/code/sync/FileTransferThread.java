package net.code.sync;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;

import jfx.messagebox.MessageBox;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class FileTransferThread extends Thread {

	static {
		// static initializer for JSch
		JSch.setConfig("StrictHostKeyChecking", "no");
		JSch.setConfig("PreferredAuthentications",
				"publickey,password,keyboard-interactive,gssapi-with-mic");
	}

	private final String remote;
	private final String local;
	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final String key;

	private final Set<FileEvent> directoryCreateEvents = new HashSet<>();
	private final Set<FileEvent> fileEvents = new HashSet<>();
	private final HashMap<String, Long> eventTimes = new HashMap<>();
	private final CodeSyncController controller;

	FileTransferThread(final String remote, final String local,
			final String host, final String username, final int port,
			final String key, final String password,
			final CodeSyncController controller) {
		this.remote = remote;
		this.local = local;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.key = key;
		this.controller = controller;
	}

	@Override
	public void run() {
		while (!this.isInterrupted()) {
			try {
				this._run();
				this.controller.addLog("暂停同步, 30秒后重新尝试");
				Thread.sleep(30000);
			} catch (final InterruptedException e) {
				break;
			}
		}
		this.controller.reset();
		this.controller.addLog("同步已退出，请重新同步");
		this.controller.showMessage("同步已退出", "同步已退出",
				MessageBox.ICON_INFORMATION | MessageBox.OK);
	}

	private ChannelSftp getChannel() throws InterruptedException, JSchException {
		final JSch jsch = new JSch();
		ChannelSftp channel = null;
		Session session = null;

		session = jsch.getSession(this.username, this.host, this.port);
		session.setConfig("PreferredAuthentications",
				"publickey,password,gssapi-with-mic,keyboard-interactive");

		if (this.key != null && this.key.length() > 0) {
			if (this.password != null && this.password.length() > 0) {
				jsch.addIdentity(this.key, this.password);
				this.controller.addLog("使用有密码密钥登陆");
			} else {
				jsch.addIdentity(this.key);
				this.controller.addLog("使用密钥登陆");
			}
		} else {
			session.setPassword(this.password);
			this.controller.addLog("使用密码登陆");
		}

		// 尝试连接三次
		boolean isConnected = false;
		JSchException connectException = null;
		for (int i = 0; i < 3; i++) {
			try {
				session.connect();
				isConnected = true;
				break;
			} catch (final JSchException e) {
				connectException = e;
				Thread.sleep(5000);
			}
		}
		if (!isConnected) {
			throw connectException;
		}

		channel = (ChannelSftp) session.openChannel("sftp");
		channel.connect();

		session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
		session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
		return channel;
	}

	private void disconnect(final ChannelSftp channel) {
		if (channel == null) {
			return;
		}
		channel.disconnect();
		try {
			channel.getSession().disconnect();
		} catch (final JSchException e) {
			// ignore intentionally
		}
	}

	private void _run() throws InterruptedException {
		if (this.directoryCreateEvents.size() > 100) {
			this.controller.showMessage("有" + this.directoryCreateEvents.size()
					+ "个文件夹创建事件没有同步了, 再不处理可能内存要爆了!", "同步警告",
					MessageBox.ICON_WARNING | MessageBox.OK);
		}
		if (this.fileEvents.size() > 1000) {
			this.controller.showMessage("有" + this.fileEvents.size()
					+ "个文件事件没有同步了, 再不处理可能内存要爆了!", "同步警告",
					MessageBox.ICON_WARNING | MessageBox.OK);
		}
		ChannelSftp channel = null;
		try {
			channel = this.getChannel();
			channel.cd(this.remote);
			channel.lcd(this.local);

			this.controller.addLog("连接成功，开始同步");
			this.controller.addLog(String.format("从 %s 到 %s@%s:%s", this.local,
					this.username, this.host, this.remote));

			while (!this.isInterrupted()) {
				synchronized (this) {
					for (final FileEvent event : this.directoryCreateEvents) {
						final String filename = event.filename;
						channel.mkdir(filename);
						this.controller.addLog("创建文件夹: " + filename + " ok");
					}
					this.directoryCreateEvents.clear();

					for (final FileEvent event : this.fileEvents) {
						
						if(event.eventType == FileEvent.Type.CREATE || event.eventType == FileEvent.Type.MODIFY) {
							final String filename = event.filename;
                            channel.put(filename, filename.replace("\\", "/"),
                                    null, ChannelSftp.OVERWRITE);
                            this.controller.addLog("上传文件: " + filename + " ok");
						} else if(event.eventType == FileEvent.Type.RENAME) {
							FileRenameEvent e = (FileRenameEvent)event;
							final String newName = e.newName;
							final String oldName = event.filename;
							channel.rename(oldName.replace("\\", "/"), newName.replace("\\", "/"));
                            this.controller.addLog("移动文件: " + oldName + " => " + newName + " ok");
						} else if (event.eventType == FileEvent.Type.DELETE) {
							final String filename = event.filename;
							channel.rm(filename.replace("\\", "/"));
                            this.controller.addLog("删除文件: " + filename + " ok");
						}
					}
					this.fileEvents.clear();
					this.wait();
				}
			}
		} catch (final SftpException e) {
			this.controller.addLog("同步异常: " + e.getMessage());
		} catch (final InterruptedException e) {
			this.controller.addLog("收到停止信号, 准备退出");
			throw e;
		} catch (final JSchException e) {
			this.controller.addLog("连接异常: " + e.getMessage());
		} finally {
			this.disconnect(channel);
		}
	}
	
	private void mark(FileEvent event) {
		
		long t = System.currentTimeMillis();
		this.eventTimes.put(event.toString(), t);
	}
	
	private boolean checkTime(FileEvent event) {
		
		Long time = this.eventTimes.get(event.toString());
		if(time == null) {
			this.mark(event);
			return true;
		}
		
		long t = System.currentTimeMillis();
		if (t - time > 5000) {
			this.mark(event);
			return true;
		}
		
		return false;
	}

	public void addFileEvent(FileEvent event) {
		final String filename = event.filename;
		if (filename.endsWith("/") || filename.endsWith("\\")) {
			this.controller.addLog("ignore event: " + event);
			return;
		}
		final File fp = new File(this.local, filename);
		if (!fp.exists() && (event.eventType == FileEvent.Type.CREATE || event.eventType == FileEvent.Type.MODIFY)) {
			this.controller.addLog("local file not exists, ignore: " + event);
			return;
		}

		event.filename = filename.replace("\\", "/");
		if (fp.isDirectory()) {
			if (event.eventType == FileEvent.Type.CREATE) {
				synchronized (this) {
					this.directoryCreateEvents.add(event);
					this.notify();
				}
			} else {
				this.controller.addLog("ignore directory event: " + event);
				return;
			}
		} else {
			synchronized (this) {
				if(!this.checkTime(event)) {
					return;
				}
				this.fileEvents.add(event);
				this.notify();
			}
		}
	}
}
