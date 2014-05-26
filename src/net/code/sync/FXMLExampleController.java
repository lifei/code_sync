package net.code.sync;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import jfx.messagebox.MessageBox;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class FXMLExampleController {

	FileTransferThread thread = null;

	int watchId = 0;

	@FXML
	private Label status;

	@FXML
	private TextField password;

	@FXML
	private TextField username;

	@FXML
	private TextField host;

	@FXML
	private TextField local;

	@FXML
	private TextField remote;

	@FXML
	private TextField port;

	@FXML
	private TextField key;

	@FXML
	private TextArea includes;

	@FXML
	private TextArea excludes;

	@FXML
	private TextArea logArea;

	@FXML
	private Button startBtn;

	@FXML
	private TabPane tab;

	@FXML
	private WebView about;

	@FXML
	protected void handleStopButton(final ActionEvent event) {
		if (this.thread != null) {
			this.thread.interrupt();
			this.thread = null;
		}
	}

	@FXML
	protected void handleSubmitButtonAction(final ActionEvent event) {

		this.tab.getSelectionModel().select(1);

		if (this.watchId != 0) {
			try {
				JNotify.removeWatch(this.watchId);
				this.watchId = 0;
			} catch (final JNotifyException e) {
				e.printStackTrace();
			}
		}

		if (this.thread != null) {
			this.thread.interrupt();
			this.thread = null;
		}

		new Thread() {
			@Override
			public void run() {

				final int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED
						| JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;

				final String incText = FXMLExampleController.this.includes
						.getText();
				final String excText = FXMLExampleController.this.excludes
						.getText();

				final String[] incs = incText.split("\n");
				final String[] excs = excText.split("\n");

				FXMLExampleController.this.addLog(String.format(
						"Includes规则: %d个", incs.length));
				FXMLExampleController.this.addLog(String.format(
						"Excludes规则: %d个", excs.length));

				final String localText = FXMLExampleController.this.local
						.getText();
				final String hostText = FXMLExampleController.this.host
						.getText();
				final String usernameText = FXMLExampleController.this.username
						.getText();
				final String passwordText = FXMLExampleController.this.password
						.getText();
				final String portText = FXMLExampleController.this.port
						.getText();
				final String keyText = FXMLExampleController.this.key.getText();
				final String remoteText = FXMLExampleController.this.remote
						.getText();

				FXMLExampleController.this.thread = new FileTransferThread(
						remoteText, localText, hostText, usernameText,
						Integer.parseInt(portText), keyText, passwordText,
						FXMLExampleController.this);

				FXMLExampleController.this.thread.setDaemon(true);
				FXMLExampleController.this.thread.start();

				final FileChangedListener listener = new FileChangedListener(
						incs, excs, FXMLExampleController.this.thread);

				try {
					FXMLExampleController.this.watchId = JNotify.addWatch(
							localText, mask, true, listener);
					FXMLExampleController.this.addLog("监控成功：" + localText);
					FXMLExampleController.this.startBtn.setDisable(true);
				} catch (final JNotifyException e) {
					FXMLExampleController.this.addLog("启动失败");
				}
			}
		}.start();
	}

	@FXML
	protected void initialize() {

		try {
			final File fp = new File(".code_sync");
			final FileReader fr = new FileReader(fp);
			final BufferedReader br = new BufferedReader(fr);
			String s;
			final StringBuffer sb = new StringBuffer();
			while ((s = br.readLine()) != null) {
				sb.append(s);
			}
			br.close();
			fr.close();

			final JSONObject json = (JSONObject) JSON.parse(sb.toString());

			this.local.setText(json.containsKey("local") ? json
					.getString("local") : "");
			this.remote.setText(json.containsKey("local") ? json
					.getString("remote") : "");
			this.host.setText(json.containsKey("local") ? json
					.getString("host") : "");
			this.username.setText(json.containsKey("local") ? json
					.getString("username") : "");
			this.password.setText(json.containsKey("local") ? json
					.getString("password") : "");
			this.port.setText(json.containsKey("local") ? json
					.getString("port") : "");
			this.key.setText(json.containsKey("local") ? json.getString("key")
					: "");

			this.includes.setText(json.containsKey("includes") ? json
					.getString("includes") : "");

			this.excludes.setText(json.containsKey("excludes") ? json
					.getString("excludes") : "");

			FXMLExampleController.this.addLog("读取配置成功。");
		} catch (final Exception e) {
			FXMLExampleController.this.addLog("就绪");
		}

		this.about
				.getEngine()
				.loadContent(
						"<h1>Code Sync</h1><p>跨平台代码更改检测同步工具</p>"
								+ "<p>特点：</p><li>跨平台，支持Windows、OS X、Linux系统</li><li>多种登陆方式</li>"
								+ "<li>事件驱动，代替遍历文件</li><li>GUI界面方便使用</li>"
								+ "<p>使用建议和Bug反馈： idoldog@163.com</p>");

	}

	@FXML
	protected void handleSaveButtonAction(final ActionEvent event) {

		new Thread() {
			@Override
			public void run() {

				final String incText = FXMLExampleController.this.includes
						.getText();
				final String excText = FXMLExampleController.this.excludes
						.getText();

				final String localText = FXMLExampleController.this.local
						.getText();
				final String hostText = FXMLExampleController.this.host
						.getText();
				final String usernameText = FXMLExampleController.this.username
						.getText();
				final String passwordText = FXMLExampleController.this.password
						.getText();
				final String portText = FXMLExampleController.this.port
						.getText();
				final String remoteText = FXMLExampleController.this.remote
						.getText();
				final String keyText = FXMLExampleController.this.key.getText();

				final HashMap<String, String> settings = new HashMap<String, String>();

				settings.put("local", localText);
				settings.put("remote", remoteText);
				settings.put("host", hostText);
				settings.put("username", usernameText);
				settings.put("port", portText);
				settings.put("password", passwordText);
				settings.put("includes", incText);
				settings.put("excludes", excText);
				settings.put("key", keyText);

				final String json = JSON.toJSONString(settings, true);

				try {
					final File fp = new File(".code_sync");
					final FileWriter fw = new FileWriter(fp);
					fw.write(json);
					fw.close();

					FXMLExampleController.this.addLog("保存配置成功！");
				} catch (final IOException e) {
					FXMLExampleController.this.addLog("保存配置失败！错误代码："
							+ e.getMessage());
				}
			}
		}.start();
	}

	@FXML
	protected void handleCloseButton(final ActionEvent event) {

		if (this.watchId != 0) {
			try {
				JNotify.removeWatch(this.watchId);
				this.watchId = 0;
			} catch (final JNotifyException e) {
				e.printStackTrace();
			}
		}

		if (this.thread != null) {
			this.thread.interrupt();
			this.thread = null;
		}

		System.exit(0);
	}

	public void addLog(final String log) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				FXMLExampleController.this.status.setText(log);
				final String log2 = FXMLExampleController.this.logArea
						.getText();
				final SimpleDateFormat sFormat = new SimpleDateFormat(
						"HH:mm:ss");
				FXMLExampleController.this.logArea.setText(sFormat
						.format(new Date()) + ":  " + log + "\r\n" + log2);
			}
		});
	}

	public void reset() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				FXMLExampleController.this.startBtn.setDisable(false);
				FXMLExampleController.this.tab.getSelectionModel().select(0);

				if (FXMLExampleController.this.watchId != 0) {
					try {
						JNotify.removeWatch(FXMLExampleController.this.watchId);
						FXMLExampleController.this.watchId = 0;
						FXMLExampleController.this.addLog("停止监控");
					} catch (final JNotifyException e) {
						FXMLExampleController.this.addLog("停止监控失败，错误代码: "
								+ e.getMessage());
					}
				}
			}
		});
	}

	public void showMessage(final String message, final String title,
			final int option) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				MessageBox.show(null, title, title, option);
			}
		});
	}
}