package delta.downloads.async;

/**
 * Interface of a bytes receiver.
 * @author DAM
 */
public interface BytesReceiver
{
  /**
   * Start this receiver.
   * @return <code>true</code> if OK, <code>false</code> otherwise.
   */
  boolean start();

  /**
   * Handle the reception of some bytes.
   * @param buffer Storage buffer.
   * @param offset Start offset.
   * @param count Bytes count.
   * @return <code>true</code> if OK, <code>false</code> otherwise.
   */
  boolean handleBytes(byte[] buffer, int offset, int count);

  /**
   * Terminate reception.
   * @return <code>true</code> if OK, <code>false</code> otherwise.
   */
  boolean terminate();
}
