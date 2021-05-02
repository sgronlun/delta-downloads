package delta.downloads.async;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import delta.common.utils.files.BinaryFileWriter;

/**
 * Receives bytes into a file.
 * @author DAM
 */
public class FileReceiver implements BytesReceiver
{
  private static final Logger LOGGER=Logger.getLogger(FileReceiver.class);

  private File _to;
  private BinaryFileWriter _writer;

  /**
   * Constructor.
   * @param to Target file.
   */
  public FileReceiver(File to)
  {
    _to=to;
  }

  @Override
  public boolean start()
  {
    _writer=new BinaryFileWriter(_to);
    boolean ok=_writer.start();
    return ok;
  }

  @Override
  public boolean handleBytes(byte[] buffer, int offset, int count)
  {
    boolean ok;
    try
    {
      _writer.getDataOutputStream().write(buffer,0,count);
      ok=true;
    }
    catch (IOException ioe)
    {
      LOGGER.warn("Could not write data!",ioe);
      ok=false;
    }
    return ok;
  }

  @Override
  public boolean terminate()
  {
    _writer.terminate();
    _writer=null;
    return true;
  }

  @Override
  public String toString()
  {
    return "File receiver: "+_to;
  }
}
