package com.pelatro.entitylist.upload.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class EntUploadHandler implements HttpHandler {

	private String filePath;

	private static final String FILENAME_DELIMETER = "@";

	private static final String FILE_SEPARATOR = "/";

	private static final String TXT = ".txt";
	
	private static final String processId = "process_id";
	
	private static final String buId = "bu_id";
	
	private static final String entityId = "entity_id";
	
	private static final String containerId = "container_id";
	
	private static final String entityListName = "ent_list_name";

	public EntUploadHandler( String path ) {
		this.filePath = path;
	}

	@Override
	public void handle( HttpExchange t ) throws IOException {
		System.out.println( "---- Recieved request" );
		OutputStream os = t.getResponseBody();

		for ( Entry<String, List<String>> header : t.getRequestHeaders().entrySet() ) {
			System.out.println( header.getKey() + ": " + header.getValue().get( 0 ) );
		}
		DiskFileItemFactory d = new DiskFileItemFactory();

		try {
			ServletFileUpload up = new ServletFileUpload( d );
			List<FileItem> result = up.parseRequest( new RequestContext() {

				@Override
				public String getCharacterEncoding() {
					return "UTF-8";
				}

				@Override
				public int getContentLength() {
					return 0;
				}

				@Override
				public String getContentType() {
					return t.getRequestHeaders().getFirst( "Content-type" );
				}

				@Override
				public InputStream getInputStream() throws IOException {
					return t.getRequestBody();
				}

			} );
			t.getResponseHeaders().add( "Content-type", "text/plain" );
			t.sendResponseHeaders( 200, 0 );
		
			Map<String, String> tagsMap = new HashMap<String, String>();
			InputStream inputStream = null;
			for ( FileItem fi : result ) {
				if ( fi.isFormField() ) {
					String fieldName = fi.getFieldName();
					String fieldValue = fi.getString();
					tagsMap.put( fieldName, fieldValue );
				}
				else {
					inputStream = fi.getInputStream();
				}
			}
			String jsonbody = writeToDirectory( tagsMap, inputStream );
			os.write( jsonbody.getBytes() );
			os.flush();
			os.close();

		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	private String writeToDirectory( Map<String, String> tags, InputStream stream ) throws IOException {
		if ( stream == null ) {
			return "";
		}
		String processIdtag = tags.get( processId );
		String buIdtag = tags.get( buId );
		String entityIdtag = tags.get( entityId);
		String entityNametag = tags.get( entityListName );
		String containerIdtag = tags.get( containerId );
		Random rand = new Random();
		int number = rand.nextInt( ( 9999999 - 1024 ) + 1 ) + 1024;
		Date date = new Date();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyymmdd" );
		String formatedDate = simpleDateFormat.format( date );
		String fileName = filePath + FILE_SEPARATOR + entityNametag + FILENAME_DELIMETER 
				+ processIdtag + FILENAME_DELIMETER + entityIdtag + FILENAME_DELIMETER 
				+ containerIdtag + FILENAME_DELIMETER + buIdtag + FILENAME_DELIMETER 
				+ number + FILENAME_DELIMETER + formatedDate + TXT;
		File file = new File( fileName );
		FileUtils.copyInputStreamToFile( stream, file );
		JSONObject entListJson = new JSONObject();
		try {
			entListJson.put( "entityListName", entityNametag );
			entListJson.put( "entityListUid", number );
		}
		catch ( JSONException e ) {
			e.printStackTrace();
		}

		String ouputResponse = entListJson.toString();
		return ouputResponse;
	}

}
