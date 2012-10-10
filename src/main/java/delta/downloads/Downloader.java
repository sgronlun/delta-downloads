package delta.downloads;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;

import delta.common.utils.files.FileCopy;
import delta.common.utils.text.EncodingNames;
import delta.downloads.utils.DownloadsLoggers;

/**
 * A download service.
 * @author DAM
 */
public class Downloader
{
  private static final Logger _logger=DownloadsLoggers.getDownloadsLogger();

  // Underlying service
  private HttpClient _client;
  // Configuration
  private boolean _followsRedirects;
  private String _charset;
  private boolean _storeCookies;
  // Statistics
  private long _downloads;
  private long _bytes;
  // Session information
  private HashMap<String,String> _cookies;

  /**
   * Constructor.
   */
  public Downloader()
  {
    _client=new HttpClient(new MultiThreadedHttpConnectionManager());
    _client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
    _followsRedirects=true;
    _charset=EncodingNames.UTF_8;
    _storeCookies=false;
    _downloads=0;
    _bytes=0;
    _cookies=new HashMap<String,String>();
  }

  /**
   * Set the 'follow redirects' behavior.
   * @param followsRedirects <code>true</code> to follow redirect responses, <code>false</code> otherwise.
   */
  public void setFollowsRedirects(boolean followsRedirects)
  {
    _followsRedirects=followsRedirects;
  }

  /**
   * Set the charset to use.
   * @param charset A charset identifier.
   */
  public void setCharset(String charset)
  {
    _charset=charset;
  }

  /**
   * Set the value of the 'store cookies' flag.
   * @param storeCookies Flag value to set.
   */
  public void setStoreCookies(boolean storeCookies)
  {
    _storeCookies=storeCookies;
  }

  private interface ResultGetter<T>
  {
    public T getResult(GetMethod method) throws Exception;
  }

  private synchronized <T> T privateDowload(String url, ResultGetter<T> getter) throws DownloadException
  {
    if (_logger.isInfoEnabled())
    {
      _logger.info("Downloading from URL ["+url+"].");
    }
    T ret=null;
    GetMethod get=new GetMethod(url);
    try
    {
      get.setFollowRedirects(_followsRedirects);
      get.getParams().setHttpElementCharset(_charset);
      int iGetResultCode=_client.executeMethod(get);
      if (_logger.isInfoEnabled())
      {
        _logger.info("Status code : "+iGetResultCode);
      }
      ret=getter.getResult(get);
      if (_storeCookies)
      {
        catchCookies(get);
      }
    }
    catch (Exception e)
    {
      throw new DownloadException("Download error for ["+url+"]!",e);
    }
    finally
    {
      get.releaseConnection();
    }
    return ret;
  }

  private void updateStatistics(long length, long downloads)
  {
    _bytes+=length;
    _downloads+=downloads;
  }

  /**
   * Download an URL as a byte buffer.
   * @param url Source URL.
   * @return A byte buffer or <code>null</code>.
   * @throws DownloadException
   */
  public byte[] downloadBuffer(String url) throws DownloadException
  {
    ResultGetter<byte[]> getter=new ResultGetter<byte[]>()
    {
      public byte[] getResult(GetMethod method) throws Exception
      {
        byte[] ret=method.getResponseBody();
        if (ret!=null)
        {
          updateStatistics(ret.length,1);
        }
        return ret;
      }
    };
    byte[] ret=privateDowload(url,getter);
    return ret;
  }

  /**
   * Download an URL as a string.
   * @param url Source URL.
   * @return A string or <code>null</code>.
   * @throws DownloadException
   */
  public String downloadString(String url) throws DownloadException
  {
    ResultGetter<String> getter=new ResultGetter<String>()
    {
      public String getResult(GetMethod method) throws Exception
      {
        String ret=method.getResponseBodyAsString();
        if (ret!=null)
        {
          long length=method.getResponseContentLength();
          updateStatistics(length,1);
        }
        return ret;
      }
    };
    String ret=privateDowload(url,getter);
    return ret;
  }

  /**
   * Download an URL into a file.
   * @param url Source URL.
   * @param to Target file.
   * @return <code>true</code> if file was successfully written, <code>false</code> otherwise.
   * @throws DownloadException
   */
  public boolean downloadToFile(String url, final File to) throws DownloadException
  {
    ResultGetter<Boolean> getter=new ResultGetter<Boolean>()
    {
      public Boolean getResult(GetMethod method) throws Exception
      {
        InputStream is=method.getResponseBodyAsStream();
        boolean ok=FileCopy.copy(is,to);
        if (ok)
        {
          updateStatistics(to.length(),1);
        }
        return Boolean.valueOf(ok);
      }
    };
    Boolean ret=privateDowload(url,getter);
    return ret.booleanValue();
  }

  /**
   * Download an URL into a file.
   * @param url Source URL.
   * @param to Target file.
   * @return <code>true</code> if file was successfully written, <code>false</code> otherwise.
   * @deprecated Use downloadToFile instead.
   */
  public boolean downloadPage(String url, File to)
  {
    boolean ret=false;
    try
    {
      ret=downloadToFile(url,to);
    }
    catch(DownloadException e)
    {
      _logger.error("Download error",e);
    }
    return ret;
  }

  /**
   * Download a page to a file using the POST method.
   * @param url Source URL.
   * @param to Target file.
   * @param parameters Parameters to post.
   * @return <code>true</code> if file was successfully written, <code>false</code> otherwise.
   */
  public boolean downloadPageAsPost(String url, File to, Map<String,String> parameters)
  {
    if (_logger.isInfoEnabled())
    {
      _logger.info("Downloading URL ["+url+"] to file ["+to+"]");
    }
    boolean ret=false;
    PostMethod post=new PostMethod(url);
    for(Map.Entry<String,String> parameter : parameters.entrySet())
    {
      post.setParameter(parameter.getKey(),parameter.getValue());
    }
    try
    {
      //get.setFollowRedirects(true);
      int iGetResultCode=_client.executeMethod(post);
      if (_logger.isInfoEnabled())
      {
        _logger.info("Status code : "+iGetResultCode);
      }
      InputStream is=post.getResponseBodyAsStream();
      ret=FileCopy.copy(is,to);
      _bytes+=to.length();
      _downloads++;
      catchCookies(post);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      post.releaseConnection();
    }
    return ret;
  }

  private void catchCookies(HttpMethodBase method)
  {
    _cookies.clear();
    Header[] headers=method.getRequestHeaders();
    for(int i=0;i<headers.length;i++)
    {
      String name=headers[i].getName();
      if ("Cookie".equals(name))
      {
        String value=headers[i].getValue();
        int separator=value.indexOf(' ');
        if (separator!=-1)
        {
          String keyValue=value.substring(separator+1);
          separator=keyValue.indexOf('=');
          String cookieName=keyValue.substring(0,separator);
          String cookieValue=keyValue.substring(separator+1);
          separator=cookieValue.indexOf(';');
          if (separator!=-1)
          {
            cookieValue=cookieValue.substring(0,separator);
          }
          _cookies.put(cookieName,cookieValue);
        }
      }
    }
  }

  /**
   * Get the value of a cookie.
   * @param cookieName Name of the cookie to get.
   * @return A cookie value or <code>null</code> if not found.
   */
  public String getCookieValue(String cookieName)
  {
    return _cookies.get(cookieName);
  }

  /**
   * Get statistics.
   * @return a statistics string.
   */
  public String getStatistics()
  {
    return "Downloaded "+_downloads+" item(s) - "+((float)_bytes)/(1024*1024)+"Mo";
  }

  /**
   * Dispose all managed resources.
   */
  public void dispose()
  {
    _client=null;
    _cookies.clear();
  }
}
