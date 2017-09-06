/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/
package p1;
import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
* 
* Modified on 9/5/17 by Xitlally Salmon: The changes that I did to this method was to add 
* the boolean WasAbleToRead which is true when the file exists and is readable other wise it is false.
* Another change to the Method was the fact that the writeContent() will not be called unless WasAbleToRead
* is true
**/
public void run()
{
 
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String Path=readHTTPRequest(is);
      boolean WasAbleToRead=ReadFile(Path);
      writeHTTPHeader(os,"text/html", WasAbleToRead);
      if(WasAbleToRead==true)
    	  writeContent(os,Path);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
* Modified on 9/5/17 by Xitlally Salmon: The changes that I made to this method was to change the return type
* to a string so that it can return the path to the file for the rest of the methods, to do this I added a
* if statement that will only be true when the while loop has iterated only  once because the first line is
* the one with the path, to get just the path I use split on the spaces and get the second entry.
 * @throws Exception 
**/
private String readHTTPRequest(InputStream is)
{
	   String line = null;
	   String[] path = null;
	   int lineNumber=1;
	   BufferedReader r = new BufferedReader(new InputStreamReader(is));
	   while (true) {
	      try {
	         while (!r.ready()) Thread.sleep(1);
	         line = r.readLine();
	         if(lineNumber==1) {
	        	 path=line.split(" ");
	         }
	         System.err.println("Request line: ("+line+")");
	         lineNumber++;
	         if (line.length()==0) break;
	      } catch (Exception e) {
	         System.err.println("Request error: "+e);
	         break;
	      }
	   }
       return path[1];
}

/*
 *Made on 9/5/17 by Xitlally Salmon: This method is to just check to make sure the file
 *is readable, it returns a boolean that is then saved into WasAbleToRead in run  
 */
private boolean ReadFile(String path)   {
	String line;
	try {
		BufferedReader F = new BufferedReader(new FileReader("./"+path));

		try {
			if((line = F.readLine()) != null) {
				F.close();
			    return true;
			}
		} catch (IOException e) {
			System.out.println("Unable to open file ");  
	        return false;
		}   

        // Always close files.
        try {
			F.close();
		} catch (IOException e) {
			System.out.println("Unable to close file ");   
	        return true;
		}   
	}
    catch(FileNotFoundException ex) {
        System.out.println("Unable to open file ");   
        return false;
    }
	return true;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
* 
* Modified on 9/5/17 by Xitlally Salmon: The only thing that I changed in this was to add a
* parm, which is a boolean and if its try the method will send out a 202 code  it will send
* out a 404 error code 
**/
private void writeHTTPHeader(OutputStream os, String contentType, boolean FileExists) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   if(FileExists==true)
	   os.write("HTTP/1.1 200 OK\n".getBytes());
   else {
	   os.write("HTTP/1.1 404: Not Found\n".getBytes());
   }
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
 * @throws IOException 
 * 
 * Modified on 9/5/17 by Xitlally Salmon: The changes that I made to this method was to read the file
 * that was at the end of the file path and do the tag substitution for what is written in the file
 * both when the tags are on new lines and when the tags are right next to each other
**/
private void writeContent(OutputStream os, String file) throws IOException
{
   os.write("<html><head></head><body>\n".getBytes());
   String line;
	try {
		BufferedReader F = new BufferedReader(new FileReader("../"+file));
		
		try {
			if((line = F.readLine()) != null) {
				if(line.indexOf(">")==line.lastIndexOf(">")) {
					if(line.contains("date"))
						os.write(" <script language=\"javascript\">\nvar today = new Date();\ndocument.write(today);\n</script>".getBytes());
					else
						os.write("<h3>Web Server name: Xitlally's Server</h3>\n".getBytes());
				}
				else {
					while(line.indexOf(">")!=line.lastIndexOf(">")) {
						if(line.charAt(line.indexOf('>')-1)=='e')
							os.write(" <script language=\"javascript\">\nvar today = new Date();\ndocument.write(today);\n</script>".getBytes());						else
							os.write("<h3>Web Server name: Xitlally's Server</h3>\n".getBytes());
						line.subSequence(line.indexOf('>')+1, line.length()-1);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Unable to open file ");
		}   

       // Always close files.
		try {
			F.close();
		} catch (IOException e) {
			System.out.println("Unable to close file ");
		}  
	}
   catch(FileNotFoundException ex) {
       System.out.println("Unable to open file ");
   }
   os.write("</body></html>\n".getBytes());
}

} // end class
