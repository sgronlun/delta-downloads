package delta.downloads.async;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContexts;

/**
 * Synchronous/Asynchronous downloads manager.
 * @author DAM
 */
public class DownloadsManager
{
  private CloseableHttpAsyncClient _client;
  private Map<Integer,SingleAsyncDownloadManager> _tasks;
  private int _nextID;

  /**
   * Constructor.
   */
  public DownloadsManager()
  {
    _nextID=1;
    _tasks=new HashMap<Integer,SingleAsyncDownloadManager>();
    _client=buildClient();
  }

  /**
   * Synchronous download.
   * @param url URL to get.
   * @param to File to write to.
   * @param listener Download listener (optional).
   * @return the download task.
   */
  public DownloadTask syncDownload(String url, File to, DownloadListener listener)
  {
    DownloadTask task=newDownload(url,to);
    boolean startOK=startDownload(task,listener);
    if (startOK)
    {
      waitForTaskTermination(task);
    }
    return task;
  }

  /**
   * Build a new download task.
   * @param url URL to get.
   * @param to File to write to.
   * @return A new download task.
   */
  public DownloadTask newDownload(String url, File to)
  {
    DownloadTask task=new DownloadTask(_nextID,url,to);
    SingleAsyncDownloadManager downloadManager=new SingleAsyncDownloadManager(_client,task);
    _tasks.put(Integer.valueOf(_nextID),downloadManager);
    _nextID++;
    return task;
  }

  /**
   * Start a download.
   * @param task Download task.
   * @param listener Optional listener for download status updates.
   * @return <code>true</code> if download started, <code>false</code> otherwise.
   */
  public boolean startDownload(DownloadTask task, DownloadListener listener)
  {
    boolean ok=false;
    int taskID=task.getID();
    SingleAsyncDownloadManager downloadManager=getDownloadManager(taskID);
    if (downloadManager!=null)
    {
      downloadManager.setListener(listener);
      ok=downloadManager.start();
    }
    else
    {
      task.setDownloadState(DownloadState.FAILED);
    }
    return ok;
  }

  /**
   * Wait for the termination of a download.
   * @param task Download task.
   */
  public void waitForTaskTermination(DownloadTask task)
  {
    int taskID=task.getID();
    SingleAsyncDownloadManager downloadManager=getDownloadManager(taskID);
    if (downloadManager!=null)
    {
      downloadManager.waitForDownloadTermination();
    }
  }

  private SingleAsyncDownloadManager getDownloadManager(int taskID)
  {
    return _tasks.get(Integer.valueOf(taskID));
  }

  /*
  LATER: wait for termination of all downloads.
  public void waitForTermination()
  {
    
  }
  */

  private CloseableHttpAsyncClient buildClient()
  {
    String[] protocols=new String[] {"TLSv1.2"};
    SSLContext sslcontext=SSLContexts.createDefault();
    SSLIOSessionStrategy sslSessionStrategy=new SSLIOSessionStrategy(sslcontext,protocols,null,SSLIOSessionStrategy.getDefaultHostnameVerifier());
    CloseableHttpAsyncClient httpclient=HttpAsyncClients.custom().setSSLStrategy(sslSessionStrategy).build();
    httpclient.start();
    return httpclient;
  }
}
