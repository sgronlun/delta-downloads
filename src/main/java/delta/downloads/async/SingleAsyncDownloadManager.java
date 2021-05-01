package delta.downloads.async;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncByteConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

import delta.common.utils.NumericTools;
import delta.common.utils.files.BinaryFileWriter;

/**
 * @author dm
 */
public class SingleAsyncDownloadManager
{
  private static final Logger LOGGER=Logger.getLogger(SingleAsyncDownloadManager.class);

  private CloseableHttpAsyncClient _client;
  private DownloadTask _task;
  private BinaryFileWriter _writer;
  private DownloadListener _listener;

  /**
   * Constructor.
   * @param client Underlying HTTP client.
   * @param task Download task.
   */
  public SingleAsyncDownloadManager(CloseableHttpAsyncClient client, DownloadTask task)
  {
    _client=client;
    _task=task;
  }

  /**
   * Set the listener.
   * @param listener Listener to use.
   */
  public void setListener(DownloadListener listener)
  {
    _listener=listener;
  }

  /**
   * Start download.
   * @return <code>true</code> if start was successfull, <code>false</code> otherwise.
   */
  public boolean start()
  {
    File targetFile=_task.getTargetFile();
    _writer=new BinaryFileWriter(targetFile);
    boolean ok=_writer.start();
    if (!ok)
    {
      _task.setDownloadState(DownloadState.FAILED);
      return false;
    }
    String url=_task.getURL();
    final HttpGet get=new HttpGet(url);
    HttpAsyncRequestProducer producer=HttpAsyncMethods.create(get);
    AsyncByteConsumer<HttpResponse> consumer=buildConsumer();
    FutureCallback<HttpResponse> callback=buildCallback();
    Future<HttpResponse> future=_client.execute(producer,consumer,callback);
    _task.setFuture(future);
    _task.setDownloadState(DownloadState.RUNNING);
    return true;
  }

  private AsyncByteConsumer<HttpResponse> buildConsumer()
  {
    AsyncByteConsumer<HttpResponse> consumer=new AsyncByteConsumer<HttpResponse>()
    {
      private HttpResponse responseStorage;

      @Override
      protected void onByteReceived(ByteBuffer buf, IOControl ioctrl) throws IOException
      {
        handleBytesReceived(buf);
      }

      @Override
      protected HttpResponse buildResult(HttpContext context) throws Exception
      {
        return this.responseStorage;
      }

      @Override
      protected void onResponseReceived(HttpResponse response) throws HttpException, IOException
      {
        if (LOGGER.isDebugEnabled())
        {
          LOGGER.debug("Received response: "+response);
        }
        this.responseStorage=response;
        Header[] headers=response.getHeaders("Content-Length");
        if ((headers!=null)&&(headers.length>0))
        {
          String valueStr=headers[0].getValue();
          Integer expectedLength=NumericTools.parseInteger(valueStr);
          if (LOGGER.isDebugEnabled())
          {
            LOGGER.debug("Expected length: "+expectedLength);
          }
          _task.setExpectedSize(expectedLength);
          invokeListener();
        }
      }
    };
    return consumer;
  }

  private FutureCallback<HttpResponse> buildCallback()
  {
    FutureCallback<HttpResponse> futureCb=new FutureCallback<HttpResponse>()
    {
      public void completed(final HttpResponse response)
      {
        handleCompletion(response.getStatusLine());
      }
      public void failed(Exception ex)
      {
        handleFailure(ex);
      }
      public void cancelled()
      {
        handleCancellation();
      }
    };
    return futureCb;
  }

  private void handleBytesReceived(ByteBuffer buf)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("Received: "+buf.remaining());
    }
    int bytesCount=buf.remaining();
    try
    {
      _writer.getDataOutputStream().write(buf.array(),0,bytesCount);
      int doneSize=_task.getDoneSize();
      doneSize+=bytesCount;
      _task.setDoneSize(doneSize);
      invokeListener();
    }
    catch (IOException ioe)
    {
      LOGGER.warn("Could not write data!",ioe);
      cancel();
      handleFailure(ioe);
    }
  }

  private void handleCompletion(StatusLine statusLine)
  {
    if (LOGGER.isDebugEnabled())
    {
      LOGGER.debug("COMPLETED "+_task.getURL()+" => "+statusLine);
    }
    int statusCode=statusLine.getStatusCode();
    if (statusCode==HttpStatus.SC_OK)
    {
      _task.setDownloadState(DownloadState.OK);
    }
    else
    {
      _task.setDownloadState(DownloadState.FAILED);
    }
    handleTermination();
    invokeListener();
  }

  private void handleFailure(Exception e)
  {
    _task.setDownloadState(DownloadState.FAILED);
    handleTermination();
    invokeListener();
  }

  private void handleCancellation()
  {
    _task.setDownloadState(DownloadState.CANCELLED);
    handleTermination();
    invokeListener();
  }

  private void handleTermination()
  {
    _writer.terminate();
    _writer=null;
  }

  /**
   * Cancel download.
   */
  public void cancel()
  {
    Future<HttpResponse> future=_task.getFuture();
    if (future!=null)
    {
      future.cancel(true);
    }
  }

  /**
   * Wait for download termination.
   */
  public void waitForDownloadTermination()
  {
    Future<HttpResponse> future=_task.getFuture();
    if (future==null)
    {
      return;
    }
    HttpResponse response=null;
    try
    {
      response=future.get();
    }
    catch (Exception e)
    {
      LOGGER.error("Caught exception in Future.get!",e);
    }
    if (response!=null)
    {
      LOGGER.debug("Got response: "+response);
    }
  }

  private void invokeListener()
  {
    if (_listener!=null)
    {
      _listener.downloadTaskUpdated(_task);
    }
  }
}
