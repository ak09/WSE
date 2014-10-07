package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Vector;



class QueryHandler  implements HttpHandler  {
	private static String plainResponse =
			"Request received, but I am not smart enough to echo yet!\n";
	
	private enum ACTION{
		CLICK,RENDER;
	}
	private Ranker _ranker;
	  
	  Vector<Headers> session=new Vector<Headers>();
	  int session_id=1;
	public QueryHandler(Ranker ranker){
		_ranker = ranker;
		
        
	}

	public static Map<String, String> getQueryMap(String query){  
		String[] params = query.split("&");  
		Map<String, String> map = new HashMap<String, String>();  
		for (String param : params){  
			String name = param.split("=")[0];  
			String value = param.split("=")[1];  
			map.put(name, value);  
		}
		return map;  
	} 

	public void log(Vector<ScoredDocument> sds,String uriPath,String query,int did){

		long render_time= 0;
		long click_time=0;
		Iterator < ScoredDocument > itr = sds.iterator();
		
Vector<LogInfo> log=new Vector <LogInfo>();
		

	if ( uriPath.equals("/search")){  
						
		session_id++;
		render_time = System.currentTimeMillis();
	while (itr.hasNext()){

		ScoredDocument sd = itr.next();

		LogInfo li=new LogInfo(session_id,query,sd._did,ACTION.RENDER.toString(),render_time);
		log.add(li);
		//System.out.println(li.asString());
		
	}
	}
	
if (uriPath.equals("/response")){  
		
		click_time = System.currentTimeMillis();
		LogInfo li=new LogInfo(session_id,query,did,ACTION.CLICK.toString(),click_time);
		log.add(li);
		//System.out.println(li.asString());
	}

write_to_file("hw1.4-log.tsv", log);
//System.out.println(log);
	}
	
	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
		if (!requestMethod.equalsIgnoreCase("GET")){  // GET requests only.
			return;
		}
		/**
		 * trying to log rendering by server on every get request
		 */
		
		// Print the user request header.
		Headers requestHeaders = exchange.getRequestHeaders();
		System.out.print("Incoming request: ");
		for (String key : requestHeaders.keySet()){
			System.out.print(key + ":" + requestHeaders.get(key) + "; ");
		}
		System.out.println();
		String queryResponse = "";  
		String uriQuery = exchange.getRequestURI().getQuery();
		String uriPath = exchange.getRequestURI().getPath();
		Map<String,String> query_map = getQueryMap(uriQuery);
		
		
			//Iterator itr = session.iterator();
			/*
			if (session.size()==0)
				session.add(exchange.getRequestHeaders());
			for(int i=0;i!=session.size();i++) 
			{
				if(!session.get(i).equals(exchange.getRequestHeaders()))
				{
					session.add(exchange.getRequestHeaders());
					session_id=i+2;
					System.out.println(session.get(i));
					break;
				}
				
					
			}
			
			System.out.println(session_id+"   "+start+"ms");
			//System.out.println("Integer.toString(render_log)");
		    InputStream is = exchange.getRequestBody();
		    while (is.read() != -1) {
		      System.out.println(is);
		    }
		    is.close();
			*/
		
		
		if ((uriPath != null) && (uriQuery != null)){
			if (uriPath.equals("/search") || uriPath.equals("/response")){

				Set<String> keys = query_map.keySet();
				String query = query_map.get("query") + "\t";
				int did=99999999;
				if (keys.contains("did")){
				did=Integer.parseInt(query_map.get("did"));
				}
				if (keys.contains("query")){
					if (keys.contains("ranker")&&keys.contains("format")){
						String ranker_type = query_map.get("ranker");
						String format_type = query_map.get("format");
						// @CS2580: Invoke different ranking functions inside your
						// implementation of the Ranker class.
						if (ranker_type.equalsIgnoreCase("cosine")&& format_type.equalsIgnoreCase("text")){
							Vector <ScoredDocument> sds = _ranker.runCosineQuery(query_map.get("query"));
							log(sds,uriPath,query,did);
							queryResponse = makeResponse(sds,query);
							
						} else if (ranker_type.equalsIgnoreCase("QL")&& format_type.equalsIgnoreCase("text")){
							Vector <ScoredDocument> sds = _ranker.QLRunQuery(query_map.get("query"));
							queryResponse = makeResponse(sds,query);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse(sds,query);
						} else if (ranker_type.equalsIgnoreCase("phrase")&& format_type.equalsIgnoreCase("text")){
							Vector <ScoredDocument> sds = _ranker.runPhraserQuery(query_map.get("query"));
							queryResponse = makeResponse(sds,query);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse(sds,query);
						} else if (ranker_type.equalsIgnoreCase("linear")&& format_type.equalsIgnoreCase("text")){
							Vector <ScoredDocument> sds = _ranker.linearRunQuery(query_map.get("query"));
							queryResponse = makeResponse(sds,query);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse(sds,query);
						} else if (format_type.equalsIgnoreCase("text")){
							Vector <ScoredDocument> sds = _ranker.runquery_numviews(query_map.get("query"));
							queryResponse = makeResponse(sds,query);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse(sds,query);
						}
						
						else if (ranker_type.equalsIgnoreCase("cosine")&& format_type.equalsIgnoreCase("html")){
							Vector <ScoredDocument> sds = _ranker.runCosineQuery(query_map.get("query"));
							log(sds,uriPath,query,did);
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
							
						} else if (ranker_type.equalsIgnoreCase("QL")&& format_type.equalsIgnoreCase("html")){
							Vector <ScoredDocument> sds = _ranker.QLRunQuery(query_map.get("query"));
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
						} else if (ranker_type.equalsIgnoreCase("phrase")&& format_type.equalsIgnoreCase("html")){
							Vector <ScoredDocument> sds = _ranker.runPhraserQuery(query_map.get("query"));
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
						} else if (ranker_type.equalsIgnoreCase("linear")&& format_type.equalsIgnoreCase("html")){
							Vector <ScoredDocument> sds = _ranker.linearRunQuery(query_map.get("query"));
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
						} else if (format_type.equalsIgnoreCase("html")){
							Vector <ScoredDocument> sds = _ranker.runquery_numviews(query_map.get("query"));
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
							log(sds,uriPath,query,did);
							queryResponse = makeResponse_HTML(sds, query,format_type,ranker_type);
						}
					} else {
						// @CS2580: The following is instructor's simple ranker that does not
						// use the Ranker class.
						Vector < ScoredDocument > sds = _ranker.runquery(query_map.get("query"));
						queryResponse = makeResponse(sds,query);
						log(sds,uriPath,query,did);
						
					}
				}
			}
		}

		// Construct a simple response.
		//Headers responseHeaders = exchange.getResponseHeaders();
	
/**
 * removed content-type, causes curl error:empty response on terminal but output is fine on browser
 * if(format_type.equalsIgnoreCase("html"))
 * {
 * responseHeaders.set("Content-Type","text/html");
 * }
 * else
 * {
 * responseHeaders.set("Content-Type","text/plain");
 * }
 * declare format_type outside if condition to use this code
 */
		
		exchange.sendResponseHeaders(200, 0);  // arbitrary number of bytes
		OutputStream responseBody = exchange.getResponseBody();
		responseBody.write(queryResponse.getBytes());
		responseBody.close();
		
		
	}


	  private String makeResponse(Vector < ScoredDocument > sds, String query){
	      StringBuilder queryResponse = new StringBuilder();
	      Iterator < ScoredDocument > itr = sds.iterator();
	      while (itr.hasNext()){
	        ScoredDocument sd = itr.next();
	        if (queryResponse.length() > 0){
	          queryResponse = queryResponse.append("\n");
	        }
	        queryResponse.append(query + sd.asString());
	      }
	      if (queryResponse.length() > 0){
	        queryResponse.append("\n");
	      }
		  return queryResponse.toString();
	  }
	private String makeResponse_HTML(Vector < ScoredDocument > sds, String query,String format_type, String ranker_type){
		StringBuilder queryResponse = new StringBuilder();
		Iterator < ScoredDocument > itr = sds.iterator();
		queryResponse.append("<html> <body><table border=1><b><tr><th>Query String</th><th>Document ID </th><th>Title</th><th>Score</th></tr></b>");

		while (itr.hasNext()){

			ScoredDocument sd = itr.next();

			queryResponse.append("<tr><td>" +query +"</td><td>"+ Integer.toString(sd._did) + "</td><td><a href='/response?query=" + query +"&ranker="+ ranker_type +"&format="+format_type+"&did="+sd._did+"'>" + sd._title + "</a></td><td>" + sd._score + "</td></tr>");

		}

		queryResponse.append("</table></body></html>");

		
		return queryResponse.toString();
	}
	
	public void write_to_file(String written_filename,Vector<LogInfo> log) {   
	    PrintWriter writer=null;
	    try {
	    	
	      writer =  new PrintWriter(new BufferedWriter(new FileWriter(written_filename, true)));
	      
	      for(int i=0;i!=log.size();i++) {
	    	  LogInfo logger=log.get(i);
	        writer.write(logger.asString()+"\n");
	      }
	    }
	    catch (IOException ignore)  {

	    }
	    finally {
	      //Close PrintWriter resources in Finally clause so that it is closed 
	      //even if an exception occurred
	      try {
	        if(writer != null) {
	        	writer.flush();
	          writer.close();
	        }
	      }
	      catch (Exception handle_writer_not_empty) {
	      // writer.flush();
	       //writer.close();
	      }
	    }
	}
}
