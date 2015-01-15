/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.samples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * This sample demonstrates how to make basic requests to Amazon S3 using
 * the AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web Services developer
 * account, and be signed up to use Amazon S3. For more information on
 * Amazon S3, see http://aws.amazon.com/s3.
 * <p>
 * <b>Important:</b> Be sure to fill in your AWS access credentials in the
 *                   AwsCredentials.properties file before you try to run this
 *                   sample.
 * http://aws.amazon.com/security-credentials
 */
public class S3Sample {
	private static Logger logger = Logger.getLogger(S3Sample.class);
	private static String pathformat = "C:\\Users\\roychan\\git\\aws-java-sample\\temp\\pdf_%s.pdf";
//	private static String pathformat = "/tmp/pdf_%s.pdf";
	private static int NTHREDS = 10;
	private static int loop = 3;
	private static int filePerloop = 5;
	
    public static void main(String[] args) throws IOException {
    	
    	s3Test();
    	/*
    	for(int i = 1; i<6;i++){
    		testgetfile(i);
    	}
    	*/
 
//    	loadInputParam(args);
 //   	testUpload(loop,filePerloop);
    }

    

	
    
    private static void loadInputParam(String[] args){
    	if(null == args || args.length != 3){
    		logger.info("Default setting" + ", pathformat=" + pathformat + ", NTHREDS=" + NTHREDS+ ", loop=" + loop);
    	} else {
			try {
				pathformat = args[0];
				NTHREDS = Integer.parseInt(args[1]);
				loop = Integer.parseInt(args[2]);
				logger.info("Custom setting" + ", pathformat=" + pathformat + ", NTHREDS=" + NTHREDS+ ", loop=" + loop);
			} catch (NumberFormatException nfe) {
				logger.error(nfe.getMessage());
	    		logger.info("Default setting" + ", pathformat=" + pathformat + ", NTHREDS=" + NTHREDS+ ", loop=" + loop);
			}
    	}
    }

    private static void testUpload(int loop, int filePerloop){
    	long totalFileLength = 0;
    	String keyformat = "%s/%s";
    	String key = null;
    	File file = null;
    	String orderId = null;
    	String path = null;
    	long start = System.currentTimeMillis();
    	

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
 //       List<Future<Boolean>> list = new ArrayList<Future<Boolean>>();
        List<FutureTask<Boolean>> futureTasklist = new ArrayList<FutureTask<Boolean>>();
    	
    	logger.info("testUpload Start");
		try {
			for (int i = 0; i < loop; i++) {
				orderId = String.format("o%s", String.valueOf(1000000 + i));

				for (int j = 1; j <= filePerloop; j++) {
					path = String.format(pathformat, j);
					file = getSampleFile(path);
					key = String.format(keyformat, orderId, file.getName());
					// logger.info("key="+key);

					//Static Test
					//s3UploadTest(key, file);
					
					// threadppol test
					
					Callable<Boolean> worker = new UploadTask(key, file);
					FutureTask<Boolean> futureTask = new FutureTask<Boolean>(worker);
					executor.execute(futureTask);
					
					
					totalFileLength+=file.length();
					futureTasklist.add(futureTask);
				}
			}
			
			
			boolean result;
			//while all thread is processed
			while (true) {
				try {
					Thread.sleep(200);
					boolean isDone = false;
					for(FutureTask<Boolean> temp: futureTasklist){
						isDone = temp.isDone();
				    	logger.info("testUpload isDone=" + isDone);
						if(!isDone) break;
					}
					if(!isDone) continue;

			    	logger.info("testUpload executor.shutdown()");
					executor.shutdown();
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
		    // now retrieve the result
			
			for (Future<Boolean> future : futureTasklist) {
				try {
					result = future.get();
					System.out.println("future.result="+result);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		    executor.shutdown();
			
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		long fileSize = totalFileLength;
		long elapTime = (System.currentTimeMillis() - start);
		float bytePerSecond = (float)fileSize / ((float)elapTime/1000);
		
    	logger.info("testUpload End"+
    	", numThread=" + NTHREDS +
    	", filePerloop=" + filePerloop +
    	", totalNumFileSent="+(loop * filePerloop)+
    	", totalFileLengthByte="+fileSize+
		", totalElapsedTimeMS="+ elapTime +
		", totalBytePerSecond="+bytePerSecond
		);
    	
    }

    
    /*
    private static void testUpload(int loop, int filePerloop){
    	long totalFileLength = 0;
    	String keyformat = "%s/%s";
    	String key = null;
    	File file = null;
    	String orderId = null;
    	String path = null;
    	long start = System.currentTimeMillis();
    	

        ExecutorService executor = Executors.newFixedThreadPool(NTHREDS);
        List<Future<Boolean>> list = new ArrayList<Future<Boolean>>();
    	
    	logger.info("testUpload Start");
		try {
			for (int i = 0; i < loop; i++) {
				orderId = String.format("o%s", String.valueOf(1000000 + i));

				for (int j = 1; j <= filePerloop; j++) {
					path = String.format(pathformat, j);
					file = getSampleFile(path);
					key = String.format(keyformat, orderId, file.getName());
					// logger.info("key="+key);

					//Static Test
					//s3UploadTest(key, file);
					
					// threadppol test
					
					Callable<Boolean> worker = new UploadTask(key, file);
				    Future<Boolean> submit = executor.submit(worker);
				    list.add(submit);
					
					
					totalFileLength+=file.length();
				}
			}
			
			
			boolean result;
		    // now retrieve the result
			for (Future<Boolean> future : list) {
				try {
					result = future.get();
					System.out.println("future.result="+result);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		    executor.shutdown();
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		long fileSize = totalFileLength;
		long elapTime = (System.currentTimeMillis() - start);
		float bytePerSecond = (float)fileSize / ((float)elapTime/1000);
		
    	logger.info("testUpload End"+
    	", numThread=" + NTHREDS +
    	", filePerloop=" + filePerloop +
    	", totalNumFileSent="+(loop * filePerloop)+
    	", totalFileLengthByte="+fileSize+
		", totalElapsedTimeMS="+ elapTime +
		", totalBytePerSecond="+bytePerSecond
		);
    	
    }
    */
    private static void s3UploadTest(String key, File file){
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

			logger.info("key=" + key + ", fileLength(byte)=" + fileSize
					+ ", elapsedTime(ms)=" + elapTime + ", bytePerSecond="
					+ bytePerSecond);

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");

        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            ace.printStackTrace();
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(e.getMessage());
		}
    }
    
    private static void s3Test(){
       	
        /*
         * This credentials provider implementation loads your AWS credentials
         * from a properties file at the root of your classpath.
         *
         * Important: Be sure to fill in your AWS access credentials in the
         *            AwsCredentials.properties file before you try to run this
         *            sample.
         * http://aws.amazon.com/security-credentials
         */
        //AmazonS3 s3 = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider());
//        String credentialPath = "META-INF/spring/AwsCredentials.properties";
//        AmazonS3 s3 = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider(credentialPath));
//        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
 //       Region apSouthEast1 = Region.getRegion(Regions.AP_SOUTHEAST_1);

    	// proxy
        ClientConfiguration clientCfg = new ClientConfiguration();
        clientCfg.setProtocol(Protocol.HTTP);

        //setup proxy connection:
        clientCfg.setProxyHost("127.0.0.1");
        clientCfg.setProxyPort(81);
        AmazonS3 s3 = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider(), clientCfg);
    	
    	
        String name = Regions.AP_SOUTHEAST_1.getName();
        System.out.println("regionName = "+ name);
        
        Region apSouthEast1 = Region.getRegion(Regions.fromName("ap-southeast-1"));
        s3.setRegion(apSouthEast1);
/*
        String bucketName = "my-first-s3-bucket-" + UUID.randomUUID();
        String key = "MyObjectKey";
*/        
//        String bucketName = "lc-oomsdoc";
        String bucketName = "lc-oomsdoc-uat";
        
        
//        String key = "o5209551/PackNote.pdf";
        
//        String bucketName = "lc-oomsdoc-test";
        String key = "test2/sample.pdf";
 

        System.out.println("===========================================");
        System.out.println("Getting Started with Amazon S3");
        System.out.println("===========================================\n");

        try {
        	long start = System.currentTimeMillis();
            /*
             * Create a new S3 bucket - Amazon S3 bucket names are globally unique,
             * so once a bucket name has been taken by any user, you can't create
             * another bucket with that same name.
             *
             * You can optionally specify a location for your bucket if you want to
             * keep your data closer to your applications or users.
             */
            //if bucket not exist!
//            System.out.println("Creating bucket " + bucketName + "\n");
//            s3.createBucket(bucketName);
            // if bucket does exist!
            s3.getBucketAcl(bucketName);
            
            
            /*
             * List the buckets in your account
             */
            
            System.out.println("Listing buckets");
            for (Bucket bucket : s3.listBuckets()) {
                System.out.println(" - " + bucket.getName());
            }
            System.out.println();

            // get object in bucket, retrieve its etag.
            /*
            ObjectMetadata meta = null;
            try{
            	meta =  s3.getObjectMetadata(bucketName, key);
            }catch (Exception e){
            	e.printStackTrace();
            }
            boolean isKeyExist = false;
            String etag = null;
            if(null != meta){
            	etag = meta.getETag();
            	System.out.println("etag="+etag);
            	isKeyExist = true;
            }
            */
            
            
 
            
            /*
             * Upload an object to your bucket - You can easily upload a file to
             * S3, or upload directly an InputStream if you know the length of
             * the data in the stream. You can also specify your own metadata
             * when uploading to S3, which allows you set a variety of options
             * like content-type and content-encoding, plus additional metadata
             * specific to your applications.
             */
/*            
            System.out.println("Uploading a new object to S3 from a file\n");
            start = System.currentTimeMillis();
            File file = createSampleFile();
            try {

                start = System.currentTimeMillis();
	            System.out.println("getmd5checksum()");
				String md5 = getMD5Checksum(file, CHECKSUM_ALGORITHM_MD5);
	            System.out.println("md5="+md5);
	            
//	            boolean isFileIdnetical = false;
//	            if(isKeyExist && null != etag){
//	            	isFileIdnetical = etag.contentEquals(md5);
//	            }
//	            System.out.println("isFileIdnetical="+isFileIdnetical);
				
//	            if(!isFileIdnetical) {
	            	PutObjectResult putResult = s3.putObject(new PutObjectRequest(bucketName, key, file));
		            System.out.println("Uploading a new object to S3 from a file"+", elapsed time = "+(System.currentTimeMillis()-start));
		            System.out.println("putResult getContentMd5="+ putResult.getContentMd5());
		            System.out.println("putResult getETag="+ putResult.getETag());
		            System.out.println("putResult getExpirationTimeRuleId="+ putResult.getExpirationTimeRuleId());
		            System.out.println("putResult getServerSideEncryption="+ putResult.getServerSideEncryption());
		            System.out.println("putResult getVersionId="+ putResult.getVersionId());
		            System.out.println("putResult getExpirationTime="+ putResult.getExpirationTime());
		            System.out.println();
		            System.out.println("md5=" + md5+", elapsedTime="+(System.currentTimeMillis()-start));
		            logger.info("md5=" + md5+", elapsedTime="+(System.currentTimeMillis()-start));
//	            }

				
	            
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logger.error(e.getMessage());
			}
*/            


            /*
             * Download an object - When you download an object, you get all of
             * the object's metadata and a stream from which to read the contents.
             * It's important to read the contents of the stream as quickly as
             * possibly since the data is streamed directly from Amazon S3 and your
             * network connection will remain open until you read all the data or
             * close the input stream.
             *
             * GetObjectRequest also supports several other options, including
             * conditional downloading of objects based on modification times,
             * ETags, and selectively downloading a range of an object.
             */
            /*
            System.out.println("Downloading an object: "+key);
            start = System.currentTimeMillis();
            S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
            
            System.out.println("Content-Type: "  + object.getObjectMetadata().getContentType()+", etag: "  + object.getObjectMetadata().getETag()+", elapsed time = "+(System.currentTimeMillis()-start));
            displayTextInputStream(object.getObjectContent());
             */
            /*
             * List objects in your bucket by prefix - There are many options for
             * listing the objects in your bucket.  Keep in mind that buckets with
             * many objects might truncate their results when listing their objects,
             * so be sure to check if the returned object listing is truncated, and
             * use the AmazonS3.listNextBatchOfObjects(...) operation to retrieve
             * additional results.
             */
//            String testPrefix = "o106014421";
//            String testPrefix = "o146732481";
            //String testPrefix = "o108498950";
//            String testPrefix = "o122124598";
//          String testPrefix = "o123418874";
//          String testPrefix = "o148469951";
//          String testPrefix = "o162519821";
            

            //SIT
 //           String testPrefix = "o1590008";
            
            // UAT 
          //           String testPrefix = "o49028814";
          //                    String testPrefix = "o49132594";
            String testPrefix = "o177215179";
            
            
            //prod
            //String testPrefix = "o120734252";
//            String testPrefix = "pdf/o204379263/20140618/";
            
            //test
            //String testPrefix = "o120825098";
             
            
            System.out.println("Listing objects: "+testPrefix);
            long totalsize = 0;
            ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
                    .withBucketName(bucketName)
//                    .withPrefix("My"));
                    .withPrefix(testPrefix)
                    );
            
            if(null == objectListing){
            	System.out.println("objectListing is null.");
            }else{
            	System.out.println("objectListing is not null.");
            	List<S3ObjectSummary> l_summary = objectListing.getObjectSummaries();
            	if(null == l_summary || l_summary.size()<=0){
            		System.out.println("l_summary is null or empty.");	
            	}else{
            		System.out.println("l_summary exists");	
					for (S3ObjectSummary objectSummary : objectListing
							.getObjectSummaries()) {
						System.out.println(" - " + objectSummary.getKey() + "  "
								+ "(size = " + objectSummary.getSize() + ")"
								+ "(etag = " + objectSummary.getETag() + ")"
								+ "(LastModifiedDate = "
								+ objectSummary.getLastModified() + ")");
						totalsize += objectSummary.getSize();
					}
            	}
            }
            System.out.println();
            System.out.println("totalsize="+totalsize);

            /*
             * Delete an object - Unless versioning has been turned on for your bucket,
             * there is no way to undelete an object, so use caution when deleting objects.
             */
//            System.out.println("Deleting an object\n");
//            s3.deleteObject(bucketName, key);

            /*
             * Delete a bucket - A bucket must be completely empty before it can be
             * deleted, so remember to delete any objects from your buckets before
             * you try to delete them.
             */
//            System.out.println("Deleting bucket " + bucketName + "\n");
//            s3.deleteBucket(bucketName);
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
        }
        catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }
    /**
     * Creates a temporary file with text data to demonstrate uploading a file
     * to Amazon S3
     *
     * @return A newly created temporary file with text data.
     *
     * @throws IOException
     */
    private static File createSampleFile() throws IOException {
        File file = File.createTempFile("aws-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.write("01234567890112345678901234\n");
        writer.write("!@#$%^&*()-=[]{};':',.<>/?\n");
        writer.write("01234567890112345678901234\n");
        writer.write("abcdefghijklmnopqrstuvwxyz\n");
        writer.close();

        return file;
    }

    private static File getSampleFile(String path) throws IOException {
    	File ret = null;
    	
    	try {
    		ret = new File(path);
    	} catch (Exception e){
    		e.printStackTrace();
    		logger.error(e.getMessage());
    	}
    	return ret;
    }
    
    private static void testgetfile(int i){
//    	String path = "temp/pdf_1.pdf";
    	String path = String.format(pathformat, String.valueOf(i));
    	logger.info("path="+path);
    	File file;
		try {
			file = getSampleFile(path);

			if (null == file) {
				logger.info("file is null=");
			} else {
				logger.info("filelength=" + file.length());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    /**
     * Displays the contents of the specified input stream as text.
     *
     * @param input
     *            The input stream to display as text.
     *
     * @throws IOException
     */
    private static void displayTextInputStream(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) break;

            System.out.println("    " + line);
        }
        System.out.println();
    }

    
	public static final String CHECKSUM_ALGORITHM_MD2 = "MD2";
	public static final String CHECKSUM_ALGORITHM_MD5 = "MD5";
	public static final String CHECKSUM_ALGORITHM_SHA1 = "SHA-1";
	public static final String CHECKSUM_ALGORITHM_SHA256 = "SHA-256";
	public static final String CHECKSUM_ALGORITHM_SHA384 = "SHA-384";
	public static final String CHECKSUM_ALGORITHM_SHA512 = "SHA-512";
	
    public static byte[] createChecksum(File file, String algorithm) throws Exception {
        InputStream fis =  new FileInputStream(file);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance(algorithm);
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    public static String getMD5Checksum(File file, String algorithm) throws Exception {
        byte[] b = createChecksum(file, algorithm);
        String result = "";

        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
    
    public static boolean createFolderAndFiles(String folderName, String fileName){
    	boolean ret = false;
    	boolean createFolder = new File(folderName).mkdir();
    	if(createFolder){
	    	File file = new File(folderName+"\\"+fileName);
	
	        Writer writer;
			try {
				writer = new OutputStreamWriter(new FileOutputStream(file));
		        writer.write("abcdefghijklmnopqrstuvwxyz");
		        writer.close();
		        
		        ret = true;
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	return ret;
    }
    
	public static boolean delete(File file) {
		boolean ret = false;
		if(null == file || !file.exists()) return ret;
		// Check if file is directory/folder
		try{
			if (file.isDirectory()) {
				// Get all files in the folder
				File[] files = file.listFiles();
	
				for (int i = 0; i < files.length; i++) {
					// Delete each file in the folder
					System.out.println("getAbsolutePath="+files[i].getAbsolutePath());
					System.out.println("getCanonicalPath="+files[i].getCanonicalPath());
					System.out.println("getName="+files[i].getName());
					System.out.println("getParent="+files[i].getParent());
					System.out.println("getPath="+files[i].getPath());
					delete(files[i]);
				}
				// Delete the folder
				ret = file.delete();
			} else {
				// Delete the file if it is not a folder
				ret = file.delete();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return ret;
	}
	
	private static void filetest(){
    	boolean createfile =  createFolderAndFiles("C:\\Users\\roychan\\git\\aws-java-sample\\test","abc.txt");
		System.out.println("createfile="+createfile);
    	if(createfile){
    		File file = new File("C:\\Users\\roychan\\git\\aws-java-sample\\test");
    		boolean del = delete(file);		
    		System.out.println("del="+del);
    		
    	}
	}
}
