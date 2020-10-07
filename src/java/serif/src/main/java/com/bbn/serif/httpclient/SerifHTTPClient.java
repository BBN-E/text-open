package com.bbn.serif.httpclient;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class provides a simple wrapper for making calls to a Serif HTTP Server. The class is
 * initialized with a server URL and possibly a SerifXMLLoader instance.
 *
 * @author dkolas
 */
public class SerifHTTPClient {


  private String url;


  private static final String requestTemplate1 = "<SerifXMLRequest> \n" +
      "  <ProcessDocument ";

  private static final String requestTemplate2 = "output_format=\"SERIFXML\">\n" +
      "    <Document docid=\"";

  private static final String requestTemplate3 = "\" language=\"";
  private static final String requestTemplate4 = "\">\n" +
      "      <OriginalText><Contents>";

  private static final String requestTemplate5 = "</Contents></OriginalText>\n" +
      "    </Document>\n" +
      "  </ProcessDocument>\n" +
      "</SerifXMLRequest>\n";


  private int readTimeout;


  /**
   * Initialize a SerifHTTPClient with a given URL and a SerifXMLLoader from standard ACE Types.
   *
   * The Read Timeout for the http calls will be infininte.
   *
   * @param url the URL of the Serif HTTP Server, including port ex: "http://localhost:8000/SerifXMLRequest"
   * @throws IOException if there is a problem initializing the SerifXMLLoader
   */
  public SerifHTTPClient(String url) throws IOException {
    this(url, 0);
  }

  /**
   * Initialize a SerifHTTPClient with a given URL and a SerifXMLLoader from standard ACE Types.
   *
   * @param url         the URL of the Serif HTTP Server, including port ex:
   *                    "http://localhost:8000/SerifXMLRequest"
   * @param readTimeout the amount of time to wait for Serif to respond, in milliseconds
   * @throws IOException if there is a problem initializing the SerifXMLLoader
   */
  public SerifHTTPClient(String url, int readTimeout) throws IOException {
    this.url = url;
    this.readTimeout = readTimeout;
  }

  /**
   * Process a document via the SerifXML server.
   *
   * @param docid    a docID to use for this document
   * @param language ex. English, Spanish
   * @param text     raw text of document
   * @return the DocTheory resulting from the processing
   */
  @SuppressWarnings("SameParameterValue")
  public DocTheory processDocument(String docid, String language, String text) throws IOException {
    return processDocument(docid, language, text, null);
  }

  /**
   * Process a document via the SerifXML server. Convenience method loads a DocTheory with a default
   * SerifXMLLoader.
   *
   * @param docid    a docID to use for this document
   * @param language ex. English, Spanish
   * @param text     raw text of document
   * @param endStage the Serif stage at which processing should finish
   * @return the DocTheory resulting from the processing
   */
  public DocTheory processDocument(String docid, String language, String text, String endStage)
      throws IOException {

    return SerifXMLLoader.builder().build()
        .loadFromString(retrieveDocumentXML(docid, language, text, endStage));
  }

  /**
   * Process a document with markup via the SerifXML server. Convenience method loads a DocTheory
   * with a default SerifXMLLoader.
   *
   * This method assumes that any HTML/XML markup in the textWithMarkup parameter is intended to be
   * processed as markup by Serif. The < and > tags of the markup should be sent as entity
   * references &gt; and &lt;.  If blocks of content within the text are meant to *not* be processed
   * as markup, they should be wrapped as CDATA by calling the applyCDATA method.
   *
   * Further, any markup that would count as region breaks to Serif will be removed EVEN IF IT IS IN
   * A CDATA BLOCK.
   *
   * @param docid    a docID to use for this document
   * @param language ex. English, Spanish
   * @param textWithMarkup     raw text of document
   * @return the DocTheory resulting from the processing
   */
  public DocTheory processSGMDocument(String docid, String language, String textWithMarkup)
      throws IOException {
    return processSGMDocument(docid, language, textWithMarkup, null);
  }

  /**
   * Process a document with markup via the SerifXML server. This method assumes that any HTML/XML
   * markup in the textWithMarkup parameter is intended to be processed as markup by Serif. The <
   * and > tags of the markup should be sent as entity references &gt; and &lt;.  If blocks of
   * content within the text are meant to *not* be processed as markup, they should be wrapped as
   * CDATA by calling the applyCDATA method.
   *
   * Further, any markup that would count as region breaks to Serif will be removed EVEN IF IT IS IN
   * A CDATA BLOCK.
   *
   * @param docid    a docID to use for this document
   * @param language ex. English, Spanish
   * @param textWithMarkup     raw text of document
   * @param endStage the Serif stage at which processing should finish
   * @return the DocTheory resulting from the processing
   */
  public DocTheory processSGMDocument(String docid, String language, String textWithMarkup,
      String endStage) throws IOException {

    return SerifXMLLoader.builder().build()
        .loadFromString(retrieveDocumentXMLForSGM(docid, language, textWithMarkup, endStage));
  }

  /**
   * Send a request to the server and get back raw XML.
   *
   * @param docid    a docID to use for this document
   * @param language ex. English, Spanish
   * @param text     raw text of document
   * @return raw SerifXML string
   */
  public String retrieveDocumentXML(String docid, String language, String text) throws IOException {
    return retrieveDocumentXML(docid, language, text, null);
  }


  /**
   * Send a request to the server and get back raw XML. This method assumes that any XML/HTML markup
   * in the 'text' string is intended to be part of the document text, and should not be treated as
   * markup by Serif.  If you want to use xml/html markup within the text content, use
   * retrieveDocumentXMLForSGM instead.
   *
   * @param docid    a docID to use for this document
   * @param language ex. English, Spanish
   * @param text     raw text of document
   * @param endStage the Serif stage at which processing should finish
   * @return raw SerifXML string
   */
  public String retrieveDocumentXML(String docid, String language, String text, String endStage)
      throws IOException {
    return retrieveDocumentXMLForSGM(docid, language, applyCDATA(text), endStage);
  }

  /**
   * Send a request to the server and get back raw XML. This method assumes that any HTML/XML markup
   * in the textWithMarkup parameter is intended to be processed as markup by Serif. The < and >
   * tags of the markup should be sent as entity references &gt; and &lt;.  If blocks of content
   * within the text are meant to *not* be processed as markup, they should be wrapped as CDATA by
   * calling the applyCDATA method.
   *
   * Further, any markup not provided by you that was actually to be included in the original
   * document will be removed EVEN IF IT IS IN A CDATA BLOCK.
   *
   * @param docid    a docID to use for this document
   * @param language ex. English, Spanish
   * @param textWithMarkup     raw text of document
   * @param endStage the Serif stage at which processing should finish
   * @return raw SerifXML string
   */
  public String retrieveDocumentXMLForSGM(String docid, String language, String textWithMarkup,
      String endStage) throws IOException {
    StringBuilder request = new StringBuilder();
    request.append(requestTemplate1);
//		request.append("input_type=\"sgm\" ");
    if (endStage != null) {
      request.append(" end_stage=\"");
      request.append(endStage);
      request.append("\" ");
    }
    request.append(requestTemplate2);
    request.append(docid);
    request.append(requestTemplate3);
    request.append(language);
    request.append(requestTemplate4);
    request.append(textWithMarkup);
    request.append(requestTemplate5);

    String requestString = request.toString();
                /* Even though this connection object is not shared across requests,
		 * the underlying TCP connection will be reused if it has not timed out.
		 */
    HttpURLConnection connection = (HttpURLConnection) (new URL(url).openConnection());
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Length",
        "" + Integer.toString(requestString.getBytes(Charsets.UTF_8).length));
    connection.setReadTimeout(readTimeout);

    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
    outputStream.write(requestString.getBytes(Charsets.UTF_8));
    outputStream.flush();
    outputStream.close();

    BufferedReader reader =
        new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

    int read;
    StringBuilder builder = new StringBuilder();
    char[] chars = new char[1000];
    while ((read = reader.read(chars)) != -1) {
      builder.append(chars, 0, read);
    }
    reader.close();
    return builder.toString();
  }

  public static String applyCDATA(String text) {
    String[] textParts = text.split("\\]\\]>");

    StringBuilder result = new StringBuilder();
    for (int i = 0; i < textParts.length; i++) {
      result.append("<![CDATA[");
      result.append(textParts[i]);
      result.append("]]>");
      if (i != textParts.length - 1 || (i == textParts.length - 1 && text.endsWith("]]>"))) {
        result.append("]]&gt;");
      }
    }
    return result.toString();
  }

}
