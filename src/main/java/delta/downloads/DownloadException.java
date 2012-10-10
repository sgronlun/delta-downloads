package delta.downloads;

/**
 * Download exception.
 * @author DAM
 */
public class DownloadException extends Exception
{
  /**
   * Constructor.
   * @param message Error message.
   * @param cause Source exception.
   */
  public DownloadException(String message, Throwable cause)
  {
    super(message,cause);
  }
}
