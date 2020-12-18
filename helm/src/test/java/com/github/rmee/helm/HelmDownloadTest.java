package com.github.rmee.helm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.github.rmee.cli.base.Cli;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

@Ignore
public class HelmDownloadTest {

	@Test
	public void testWindowsDownload() throws IOException {
		testDownload(OperatingSystem.WINDOWS);
	}

	@Test
	public void testLinuxDownload() throws IOException {
		testDownload(OperatingSystem.LINUX);
	}

	@Test
	public void testMacDownload() throws IOException {
		testDownload(OperatingSystem.MAC_OS);
	}

	private void testDownload(OperatingSystem operatingSystem) throws IOException {
		HelmExtension extension = new HelmExtension();
		Cli cli = extension.getCli();
		cli.setDockerized(false);
		cli.setOperationSystem(operatingSystem);

		extension.setProject(Mockito.mock(Project.class));
		extension.init();
		String downloadUrl = cli.getDownloadUrl();

		try {
			try (InputStream inputStream = new URL(downloadUrl).openStream()) {
				inputStream.read();
			}
		} catch (IOException e) {
			throw new IllegalStateException(downloadUrl, e);
		}
	}
}
