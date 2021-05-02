package delta.downloads.async;

import java.io.ByteArrayOutputStream;

/**
 * Receives bytes into a buffer.
 * @author DAM
 */
public class BufferReceiver implements BytesReceiver
{
  private ByteArrayOutputStream _bos;

  /**
   * Constructor.
   */
  public BufferReceiver()
  {
    _bos=new ByteArrayOutputStream();
  }

  @Override
  public boolean start()
  {
    _bos=new ByteArrayOutputStream();
    return true;
  }

  @Override
  public boolean handleBytes(byte[] buffer, int offset, int count)
  {
    _bos.write(buffer,offset,count);
    return true;
  }

  @Override
  public boolean terminate()
  {
    return true;
  }

  /**
   * Get the result buffer.
   * @return the result buffer.
   */
  public byte[] getBytes()
  {
    return _bos.toByteArray();
  }

  @Override
  public String toString()
  {
    return "Buffer receiver";
  }
}
