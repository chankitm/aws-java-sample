package com.amazonaws.samples;

import java.io.File;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

public class UploadTask implements Callable<Boolean>{

	private static Logger logger = Logger.getLogger(UploadTask.class);
	
	private String key; 
	private File file;
	
	public UploadTask(String key, File file) {
		super();
		this.key = key;
		this.file = file;
	}
	
	public Boolean call() throws Exception {
		Boolean ret = false;
		try {
			//ret = this.storageService.store(key, file);
			ret = s3UploadTest(key,file);
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}  
		
		return ret;
	}

    private boolean s3UploadTest(String key, File file){
       	boolean ret = false;

        AmazonS3 s3 = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider());
        
        Region apSouthEast1 = Region.getRegion(Regions.fromName("ap-southeast-1"));
        s3.setRegion(apSouthEast1);

        String bucketName = "lc-oomsdoc-loadtest";
        try {
        	long start = System.currentTimeMillis();
            s3.getBucketAcl(bucketName);

			start = System.currentTimeMillis();
			PutObjectResult putResult = s3.putObject(new PutObjectRequest(
					bucketName, key, file));
			long elapTime = System.currentTimeMillis() - start;
			long fileSize = file.length();
			float bytePerSecond = (float) fileSize / ((float) elapTime / 1000);

			// System.out.println("key=" +
			// key+", fileLength(kb)="+fileSize+", elapsedTime(ms)="+elapTime+", kb/ms="+kbPermillisecond);
			logger.info("key=" + key + ", fileLength(byte)=" + fileSize
					+ ", elapsedTime(ms)=" + elapTime + ", bytePerSecond="
					+ bytePerSecond);

			if (null != putResult){
				ret = true;
			}
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		}
        return ret;
    }
}

/*
public class UploadTask implements Runnable{
	private String key; 
	private File file;
	@Autowired
	private StorageService storageService;
	
	public UploadTask(String key, File file) {
		super();
		this.key = key;
		this.file = file;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		 
		try {
			this.storageService.store(key, file);
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}  
	}
}
*/
