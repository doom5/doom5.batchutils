package doom5.batchutils.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.io.InputStreamByteChunkProvider;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.utils.SmbFiles;

public class SmbUtils{
	private Logger logger;
	private DiskShare diskShare;
	private SMBClient client;
	private Session session;
	private Connection connection;
	private String destServer;
	private String destShare;
	private String destDomain;
	private String destUser;
	private String destPass;

	public SmbUtils(String destServer, String destShare, String destDomain, String destUser, String destPass) {
		logger = LogManager.getRootLogger();
		this.destServer = destServer;
		this.destShare = destShare;
		this.destDomain = destDomain;
		this.destUser = destUser;
		this.destPass = destPass;
	}

	public boolean connect() {
		boolean ret;
		try {

			this.client = new SMBClient();
			this.connection = client.connect(destServer);
			AuthenticationContext ac = new AuthenticationContext(destUser, destPass.toCharArray(), destDomain);
			this.session = connection.authenticate(ac);
			this.diskShare = (DiskShare) session.connectShare(destShare);
			ret = true;
		} catch (IOException e) {
			ret = false;
			e.printStackTrace();
		}
		return ret;
	}
	
	public boolean close() {
		boolean ret;
		try {
			this.diskShare.close();
			this.session.close();
			this.connection.close();
			ret = true;
		} catch (IOException e) {
			ret = false;
			e.printStackTrace();
		}
		return ret;
	}
	
	public List<String> listRemoteFile(String path, String searchPattern) throws SMBApiException{
		List<String> ret = new ArrayList<String> ();
		for (FileIdBothDirectoryInformation f : this.diskShare.list(path, searchPattern))
		{
			ret.add(f.getFileName());
		}
		return ret;		
	}
	
	///
	public boolean guessIfFileNotInUseByDate(String remoteFile, long diffMillis) throws SMBApiException{
		boolean ret = false;
		Date now = new Date();
		Date creationTime = diskShare.getFileInformation(remoteFile).getBasicInformation().getCreationTime().toDate();
		Date writeTime = diskShare.getFileInformation(remoteFile).getBasicInformation().getLastWriteTime().toDate();
		Date changeTime = diskShare.getFileInformation(remoteFile).getBasicInformation().getChangeTime().toDate();
		long diffInMillies1 = Math.abs(now.getTime() - creationTime.getTime());
		long diffInMillies2 = Math.abs(now.getTime() - writeTime.getTime());
		long diffInMillies3 = Math.abs(now.getTime() - changeTime.getTime());
		if (diffInMillies1>diffMillis && diffInMillies2>diffMillis && diffInMillies3>diffMillis)
		{
			ret = true;
		}

		return ret;		
	}
	

	public boolean localToRemoteFileCopy(String sourceFile, String destFile, boolean overwrite) {
		boolean ret;
		try {
			java.io.File source = new java.io.File(sourceFile);
			SmbFiles.copy(source, diskShare, destFile, overwrite);
			ret = true;
		} catch (NullPointerException | IOException e) {
			ret = false;
			e.printStackTrace();
		}
		return ret;
	}

	public boolean remoteToLocalCopy(String remoteSourcePath, String localDestinationPath, boolean overwrite) {
		boolean ret;
		try {
			InputStream stream = readBytes(transformPath(remoteSourcePath));
			File targetFile = new File(localDestinationPath);
			if (overwrite) {
				java.nio.file.Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} else {
				java.nio.file.Files.copy(stream, targetFile.toPath());
			}

			IOUtils.closeQuietly(stream);
			ret = true;
		} catch (SMBApiException | IOException e) {
			ret = false;
			e.printStackTrace();
		}
		return ret;
	}

	public boolean remoteToRemoteCopy(String remoteSourcePath, String remoteDestinationPath, boolean overwrite){
		boolean ret;
		try {
			InputStream stream = readBytes(transformPath(remoteSourcePath));
			write(transformPath(remoteDestinationPath), stream, overwrite);
			ret = true;
		} catch (SMBApiException | IOException e) {
			ret= false;
			e.printStackTrace();
		}
		return ret;
	}
	
	public boolean deleteFile (String remoteFile)
	{
		boolean ret;
		try {
			diskShare.rm(remoteFile);
			ret = true;
		} catch (SMBApiException e) {
			ret=false;
			e.printStackTrace();
		}
		return ret;
	}

	private void write(final String remotePath, InputStream is, boolean overwrite) throws IOException, SMBApiException {
		SMB2CreateDisposition oW = SMB2CreateDisposition.FILE_CREATE;
		if (overwrite) {
			oW = SMB2CreateDisposition.FILE_OVERWRITE_IF;
		}
		
		try (com.hierynomus.smbj.share.File file = diskShare.openFile(transformPath(remotePath),
				EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL,
				oW, null)) {
			file.write(new InputStreamByteChunkProvider(is));
		}
	}

	private String transformPath(String path) {
		return path.replace("/", "\\");
	}

	private InputStream readBytes(String remotePath) throws IOException, SMBApiException {
		ByteArrayInputStream byteStream;
		try (com.hierynomus.smbj.share.File file = diskShare.openFile(transformPath(remotePath),
				EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN,
				null)) {
			byteStream = new ByteArrayInputStream(IOUtils.toByteArray((file.getInputStream())));
		}
		return byteStream;
	}
}
