package fxmlexample;

import java.io.File;
import java.util.HashSet;

import jfx.messagebox.MessageBox;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class FileTransferThread extends Thread {

	private final String remote;
	private final String local;
	private final String host;
	private final int port;
	private final String username;
	private final String password;
	private final String key;

	private final HashSet<String> files = new HashSet<String>();
	private final FXMLExampleController controller;

	FileTransferThread(final String remote, final String local,
			final String host, final String username, final int port,
			final String key, final String password,
			final FXMLExampleController controller) {
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
		JSch.setConfig("StrictHostKeyChecking", "no");

		final JSch jsch = new JSch();
		ChannelSftp channel = null;
		Session session = null;
		try {

			session = jsch.getSession(this.username, this.host, this.port);
			session.setConfig("PreferredAuthentications", "publickey,gssapi-with-mic,keyboard-interactive,password");

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

			session.connect();

			channel = (ChannelSftp) session.openChannel("sftp");
			channel.connect();

			session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
			session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");

			channel.cd(this.remote);
			channel.lcd(this.local);

			this.controller.addLog("连接成功，开始同步");
			this.controller.addLog(String.format("从 %s 到 %s@%s:%s", this.local,
					this.username, this.host, this.remote));

			while (!this.isInterrupted()) {
				Thread.sleep(500);

				synchronized (this) {
					for (final String file : this.files) {
						if (file.endsWith("/") || file.endsWith("\\")) {
							continue;
						}

						final File fp = new File(this.local, file);
						if (!fp.exists()) {
							continue;
						}

						if (fp.isDirectory()) {
							continue;
						}

						channel.put(file, file.replace("\\", "/"), null,
								ChannelSftp.OVERWRITE);
						this.controller.addLog("上传文件: " + file + " ok");
					}
					this.files.clear();
				}

			}
		} catch (final JSchException | SftpException | InterruptedException e) {
			this.controller.reset();
			this.controller.addLog("同步发生错误，错误代码：" + e.getMessage());
		} finally {
			if (channel != null) {
				channel.disconnect();
			}

			if (session != null) {
				session.disconnect();
			}
		}

		this.controller.addLog("同步已退出，请重新同步");
		this.controller.showMessage("同步已退出", "同步已退出",
				MessageBox.ICON_INFORMATION | MessageBox.OK);

	}

	public void addFile(final String file) {
		synchronized (this) {
			this.files.add(file);
		}
	}
}
