package delta.downloads.async;

import java.util.concurrent.Future;

import org.apache.http.HttpResponse;

/**
 * Download task state.
 * @author DAM
 */
public class DownloadTask
{
  private int _id;
  private String _url;
  private BytesReceiver _receiver;
  private Integer _expectedSize;
  private int _doneSize;
  private DownloadState _state;
  private Future<HttpResponse> _future;

  /**
   * Constructor.
   * @param id Task ID.
   * @param url URL to get.
   * @param receiver Bytes receiver.
   */
  public DownloadTask(int id, String url, BytesReceiver receiver)
  {
    _id=id;
    _url=url;
    _receiver=receiver;
    _state=DownloadState.NOT_RUNNING;
  }

  /**
   * Get the task identifier.
   * @return a task identifier.
   */
  public int getID()
  {
    return _id;
  }

  /**
   * Get the URL to get.
   * @return an URL.
   */
  public String getURL()
  {
    return _url;
  }

  /**
   * Get the file to write to.
   * @return A file.
   */
  public BytesReceiver getReceiver()
  {
    return _receiver;
  }

  /**
   * Set the expected download size.
   * @param expectedSize Size to set (bytes).
   */
  public void setExpectedSize(Integer expectedSize)
  {
    _expectedSize=expectedSize;
  }

  /**
   * Get the expected size.
   * @return a size in bytes.
   */
  public Integer getExpectedSize()
  {
    return _expectedSize;
  }

  /**
   * Get the total bytes already downloaded.
   * @return a size (bytes).
   */
  public int getDoneSize()
  {
    return _doneSize;
  }

  /**
   * Set the total bytes already downloaded.
   * @param doneSize Size to set (bytes).
   */
  public void setDoneSize(int doneSize)
  {
    _doneSize=doneSize;
  }

  /**
   * Get the download state.
   * @return the download state.
   */
  public DownloadState getDownloadState()
  {
    return _state;
  }

  /**
   * Set the download state.
   * @param state State to set.
   */
  public void setDownloadState(DownloadState state)
  {
    _state=state;
  }

  /**
   * Get the future that drives the download.
   * @return a future or <code>null</code> if none.
   */
  Future<HttpResponse> getFuture()
  {
    return _future;
  }

  /**
   * Set the future that drives the download.
   * @param future Future to set.
   */
  void setFuture(Future<HttpResponse> future)
  {
    _future=future;
  }

  @Override
  public String toString()
  {
    StringBuilder sb=new StringBuilder();
    sb.append("Download from ").append(_url);
    sb.append(" to receiver ").append(_receiver);
    sb.append(": state=").append(_state);
    sb.append(", done ").append(_doneSize);
    sb.append("/");
    if (_expectedSize!=null)
    {
      sb.append(_expectedSize);
    }
    else
    {
      sb.append('?');
    }
    sb.append(" bytes");
    return sb.toString();
  }
}
