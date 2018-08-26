package reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class URLReader {
	
	public static final int ATTEMPTS = 4;
	public static final int BUFFER_SIZE = 524288;
	public static final int BUFFER_SIZE_SMALL = 4096;
	//database: https://udger.com/resources/ua-list
	//https://github.com/tamimibrahim17/List-of-user-agents
	public static final String USER_AGENT_OLD_BROWSER = "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0";
	public static final String USER_AGENT             = "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/40.0";
	//https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding
	public static final String[] COMPRESS_VARS = {"gzip","compress","deflate","br","identity"/*no compression*/};
	
	private static enum DECOMPRESS {
		GZIP,//legacy
		COMPRESS,
		DEFLATE,//most common
		BR,
		IDENTITY
	};
	
	
	
	
	public static RawData grabURLDataFullSuite(String urlString){
		return grabURLDataFullSuite(urlString, null);
	}
	public static RawData grabURLDataFullSuite(String urlString, String params){
		RawData data = null;
		
		data = grabURLData(urlString, params, true, true);
		
		for(int attempt = 0; attempt < ATTEMPTS; attempt++){
			switch(attempt){
			case(0):
				data = grabURLDataHttps(urlString, params);
				break;
			case(1):
				data = grabURLData(urlString, params);
				break;
			case(2):
				data = grabURLDataHttpsJava(urlString, params);
				break;
			case(3):
				data = grabURLDataJava(urlString, params);
				break;
			}
			if(data!=null){
				System.out.println("URLRead Success Type: "+attempt);
				return data;
			}
		}
		
		return data;
	}
	
	//https://stackoverflow.com/questions/4205980/java-sending-http-parameters-via-post-method-easily
	public static RawData grabURLDataHttps(String urlString){
		return grabURLData(urlString, null, true, true);
	}
	public static RawData grabURLDataHttps(String urlString, String urlParams){
		return grabURLData(urlString, urlParams, true, true);
	}
	//
	//
	//
	public static RawData grabURLData(String urlString){
		return grabURLData(urlString, null, true, false);
	}
	public static RawData grabURLData(String urlString, String urlParams){
		return grabURLData(urlString, urlParams, true, false);
	}
	//
	//
	//
	public static RawData grabURLDataJava(String urlString){
		return grabURLData(urlString, null, false, false);
	}
	public static RawData grabURLDataJava(String urlString, String urlParams){
		return grabURLData(urlString, urlParams, false, false);
	}
	//
	//
	//
	public static RawData grabURLDataHttpsJava(String urlString){
		return grabURLData(urlString, null, false, true);
	}
	public static RawData grabURLDataHttpsJava(String urlString, String urlParams){
		return grabURLData(urlString, urlParams, false, true);
	}
	
	public static RawData grabURLData(String urlString, String urlParams, boolean spoofType, boolean https){
		return grabURLData(urlString, urlParams, spoofType, https, true);
	}
	
	public static RawData grabURLData(String urlString, String urlParams, boolean spoofType, boolean https, boolean compress){
		try{
			URL url = new URL(urlString);
			URLConnection conn;
			if(https){
				conn = (HttpsURLConnection)url.openConnection();
			}else{
				conn = url.openConnection();
			}
			
			if(spoofType){
				conn.setRequestProperty("User-Agent",USER_AGENT);
			}
			if(compress){
				conn.setRequestProperty("Content-Encoding","deflate");
			}
			
			if(urlParams!=null){
				conn.setDoOutput(true);
				OutputStream serverParams = conn.getOutputStream();
				serverParams.write(urlParams.getBytes(StandardCharsets.UTF_8));
				serverParams.flush();
				serverParams.close();
				//conn.setDoOutput(false);
			}
			
			//conn.getRequestProperties()
			conn.getRequestProperties();
			
			RawData base = new RawData(),item=base;
			InputStream serverResponse = conn.getInputStream();
			
			byte[] buffer = new byte[BUFFER_SIZE];
			int n;
			
			while ((n = serverResponse.read(buffer)) >0) 
			{
				item.linked=new RawData(n,Arrays.copyOf(buffer, n));
				item=item.linked;
			}
			serverResponse.close();
			
			//make sure first value actually holds data, prevents extra null skips down the road
			base=base.linked;
			
			return base;
			}catch(Exception e){
				e.printStackTrace();
			}
		return null;
	}
	
	public static void print_https_cert(HttpsURLConnection con) {
		if (con != null) {
			try {
				System.out.println("Response Code : " + con.getResponseCode());
				System.out.println("Cipher Suite : " + con.getCipherSuite());
				System.out.println("\n");

				Certificate[] certs = con.getServerCertificates();
				for (Certificate cert : certs) {
					System.out.println("Cert Type : " + cert.getType());
					System.out.println("Cert Hash Code : " + cert.hashCode());
					System.out.println("Cert Public Key Algorithm : "
							+ cert.getPublicKey().getAlgorithm());
					System.out.println("Cert Public Key Format : "
							+ cert.getPublicKey().getFormat());
					System.out.println("\n");
				}

			} catch (SSLPeerUnverifiedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	//https://gist.github.com/yfnick/227e0c12957a329ad138
	public static RawData decompressGzip(RawData data) throws IOException{
		return decompressData(data,DECOMPRESS.GZIP);
	}

	// https://stackoverflow.com/questions/33020765/java-decompress-a-string-compressed-with-zlib-deflate
	public static RawData decompressDeflate(RawData data) throws IOException {
		return decompressData(data,DECOMPRESS.DEFLATE);
	}

	public static RawData decompressData(RawData data, DECOMPRESS type) throws IOException {
		byte[] block = data.getSolidBlock();
		if(block==null){
			return null;
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(block);
		InflaterInputStream iis = null;
		switch (type) {
		case DEFLATE:
			iis = new InflaterInputStream(bais);
			break;
		case GZIP:
			iis = new GZIPInputStream(bais);
			break;
		case BR:
			//https://github.com/google/brotli/blob/master/java/org/brotli/dec/BrotliInputStream.java
			//https://github.com/google/brotli/blob/master/java/org/brotli/dec/Decode.java
			//iis = new BrotliInputStream(bais);
			break;
		case COMPRESS:
			//dunno at the moment
			//basically complete web deprecation
			return null;
		default:
			return null;
		}
		if(iis==null){
			return null;
		}
		RawData base = new RawData(), item = base;
		byte[] buffer = new byte[BUFFER_SIZE];
		int n;
		while ((n = iis.read(buffer)) > 0) {
			item.linked = new RawData(n, Arrays.copyOf(buffer, n));
			item = item.linked;
		}

		return item;
	}
	
	
	
}
